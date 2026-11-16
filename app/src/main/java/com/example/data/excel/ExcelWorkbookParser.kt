package com.example.data.excel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.example.model.CashFlowEntry
import com.example.model.FlowType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream

data class ParsedSheet(
    val name: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

data class ParsedWorkbook(
    val fileName: String,
    val sheets: List<ParsedSheet>
)

data class ColumnMapping(
    val periodCol: Int = 0,
    val categoryCol: Int = 1,
    val typeCol: Int? = null,
    val inflowCol: Int? = null,
    val outflowCol: Int? = null,
    val amountCol: Int? = null,
    val noteCol: Int? = null
)

object ExcelWorkbookParser {

    private const val MAX_IMPORT_BYTES = 15 * 1024 * 1024

    suspend fun parseUri(context: Context, uri: Uri): Result<ParsedWorkbook> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = resolveFileName(context, uri)
            val lowerName = fileName.lowercase(Locale.ROOT)
            if (lowerName.endsWith(".xls") && !lowerName.endsWith(".xlsx")) {
                throw IllegalArgumentException(
                    "Legacy binary .xls files are not supported directly. Please save the workbook in Excel as .xlsx or .csv."
                )
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                readBoundedBytes(stream, MAX_IMPORT_BYTES)
            } ?: throw IllegalStateException("Unable to read selected file.")

            if (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                parseXlsxBytes(fileName, bytes)
            } else if (bytes.size >= 4 &&
                bytes[0] == 0xD0.toByte() &&
                bytes[1] == 0xCF.toByte() &&
                bytes[2] == 0x11.toByte() &&
                bytes[3] == 0xE0.toByte()
            ) {
                throw IllegalArgumentException(
                    "Legacy binary .xls format detected. Please export as .xlsx or .csv in Microsoft Excel."
                )
            } else if (looksLikeBinaryNonText(bytes)) {
                throw IllegalArgumentException(
                    "Unsupported file format. Please select an Excel (.xlsx) or CSV (.csv) spreadsheet."
                )
            } else {
                parseDelimitedBytes(fileName, bytes)
            }
        }
    }

    private fun readBoundedBytes(stream: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) {
                throw IllegalArgumentException("Selected file exceeds the 15 MB limit.")
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun looksLikeBinaryNonText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val hasUtf16Bom = (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) ||
            (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())
        if (hasUtf16Bom) return false
        if (bytes.size >= 4 &&
            bytes[0] == 0x25.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x44.toByte() &&
            bytes[3] == 0x46.toByte()
        ) {
            return true // %PDF
        }
        val sampleSize = minOf(bytes.size, 512)
        var suspiciousControlCount = 0
        for (i in 0 until sampleSize) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0 || (b < 0x20 && b != 0x09 && b != 0x0A && b != 0x0D)) {
                suspiciousControlCount++
            }
        }
        return suspiciousControlCount > sampleSize / 10
    }

    fun parseXlsxBytes(fileName: String, bytes: ByteArray): ParsedWorkbook {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = ByteArrayOutputStream()
                    zis.copyTo(out)
                    entries[entry.name.trimStart('/')] = out.toByteArray()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val dateStyleIndices = entries["xl/styles.xml"]?.let(::parseDateStyleIndices).orEmpty()
        val sheetRefs = resolveSheetEntries(entries)

        if (sheetRefs.isEmpty()) {
            throw IllegalArgumentException("No worksheets found inside the Excel .xlsx file.")
        }

        val parsedSheets = sheetRefs.mapNotNull { (sheetName, path) ->
            val sheetBytes = entries[path] ?: return@mapNotNull null
            val rawRows = parseWorksheetXml(sheetBytes, sharedStrings, dateStyleIndices)
                .filter { row -> row.any { it.isNotBlank() } }
            if (rawRows.isEmpty()) return@mapNotNull null

            val maxCols = rawRows.maxOf { it.size }.coerceAtLeast(1)
            val normalizedRows = rawRows.map { row ->
                if (row.size < maxCols) row + List(maxCols - row.size) { "" } else row
            }
            val headerIndex = normalizedRows.indexOfFirst { row -> row.count { it.isNotBlank() } >= 2 }
                .takeIf { it >= 0 } ?: 0
            val headers = normalizedRows[headerIndex].mapIndexed { idx, value ->
                value.trim().ifBlank { "Column ${idx + 1}" }
            }
            val dataRows = normalizedRows.drop(headerIndex + 1).filter { row -> row.any { it.isNotBlank() } }
            ParsedSheet(name = sheetName, headers = headers, rows = dataRows)
        }

        if (parsedSheets.isEmpty()) {
            throw IllegalArgumentException("The selected Excel workbook has no readable rows.")
        }

        return ParsedWorkbook(fileName = fileName, sheets = parsedSheets)
    }

    fun parseDelimitedBytes(fileName: String, bytes: ByteArray): ParsedWorkbook {
        val text = decodeSpreadsheetText(bytes).removePrefix("\uFEFF")
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) {
            throw IllegalArgumentException("The selected CSV file is empty.")
        }

        val sample = lines.take(5).joinToString("\n")
        val delimiter = when {
            sample.count { it == '\t' } >= sample.count { it == ',' } && sample.contains('\t') -> '\t'
            sample.count { it == ';' } > sample.count { it == ',' } -> ';'
            else -> ','
        }

        val parsedRows = parseDelimitedText(text, delimiter).filter { row -> row.any { it.isNotBlank() } }
        if (parsedRows.isEmpty()) {
            throw IllegalArgumentException("No data rows found in spreadsheet.")
        }
        val maxCols = parsedRows.maxOf { it.size }.coerceAtLeast(1)
        val normalized = parsedRows.map { row ->
            if (row.size < maxCols) row + List(maxCols - row.size) { "" } else row
        }
        val headers = normalized.first().mapIndexed { idx, v -> v.trim().ifBlank { "Column ${idx + 1}" } }
        val dataRows = normalized.drop(1)
        return ParsedWorkbook(
            fileName = fileName,
            sheets = listOf(ParsedSheet(name = "Sheet1", headers = headers, rows = dataRows))
        )
    }

    private fun decodeSpreadsheetText(bytes: ByteArray): String {
        return when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            else -> bytes.toString(Charsets.UTF_8)
        }
    }

    fun autoDetectMapping(headers: List<String>, rows: List<List<String>>): ColumnMapping {
        val lower = headers.map { it.lowercase(Locale.ROOT).trim() }

        val periodCol = findBestIndex(lower, listOf("date", "month", "period", "quarter", "year", "datum", "mois", "période", "monat", "تاريخ", "التاريخ", "الشهر", "तारीख", "माह", "अवधि"))
            ?: 0

        val categoryCol = findBestIndex(lower, listOf("category", "item", "account", "department", "segment", "catégorie", "kategorie", "posten", "الفئة", "البند", "श्रेणी", "मद", "description", "label"))
            ?: (if (headers.size > 1 && periodCol != 1) 1 else 0)

        val inflowCol = findBestIndex(lower, listOf("inflow", "cash in", "revenue", "income", "credit", "receipt", "recettes", "entrée", "einnahmen", "zufluss", "التدفق الداخل", "الإيرادات", "आय", "आवक"))
        val outflowCol = findBestIndex(lower, listOf("outflow", "cash out", "expense", "debit", "cost", "spend", "dépenses", "sortie", "ausgaben", "abfluss", "المصروفات", "التدفق الخارج", "व्यय", "खर्च"))

        val typeCol = findBestIndex(lower, listOf("type", "direction", "flow", "kind", "art", "typ", "النوع", "प्रकार"))
        val amountCol = findBestIndex(lower, listOf("amount", "value", "total", "net", "cash", "montant", "betrag", "summe", "المبلغ", "राशि", "कुल"))
            ?: detectNumericColumn(headers, rows, exclude = setOfNotNull(periodCol, categoryCol, typeCol))

        val noteCol = findBestIndex(lower, listOf("note", "notes", "memo", "remark", "comment", "details", "hinweis", "ملاحظات", "नोट", "विवरण"))

        return if (inflowCol != null || outflowCol != null) {
            ColumnMapping(
                periodCol = periodCol,
                categoryCol = categoryCol,
                typeCol = null,
                inflowCol = inflowCol,
                outflowCol = outflowCol,
                amountCol = null,
                noteCol = noteCol
            )
        } else {
            ColumnMapping(
                periodCol = periodCol,
                categoryCol = categoryCol,
                typeCol = typeCol,
                inflowCol = null,
                outflowCol = null,
                amountCol = amountCol,
                noteCol = noteCol
            )
        }
    }

    fun convertRowsToEntries(sheet: ParsedSheet, mapping: ColumnMapping): List<CashFlowEntry> {
        val result = mutableListOf<CashFlowEntry>()
        val baseMillis = System.currentTimeMillis()

        sheet.rows.forEachIndexed { index, row ->
            val rawPeriod = row.getOrNull(mapping.periodCol)?.trim().orEmpty()
            val periodLabel = normalizePeriodLabel(rawPeriod, index)
            val dateMillis = parseDateMillis(rawPeriod, baseMillis + index * 86_400_000L)
            val category = row.getOrNull(mapping.categoryCol)?.trim().orEmpty().ifBlank { "General" }
            val note = mapping.noteCol?.let { row.getOrNull(it)?.trim().orEmpty() }.orEmpty()

            if (mapping.inflowCol != null || mapping.outflowCol != null) {
                val inflowVal = mapping.inflowCol?.let { parseNumber(row.getOrNull(it)) } ?: 0.0
                val outflowVal = mapping.outflowCol?.let { parseNumber(row.getOrNull(it)) } ?: 0.0
                if (inflowVal > 0.0) {
                    result += CashFlowEntry(
                        periodLabel = periodLabel,
                        dateMillis = dateMillis,
                        category = category,
                        type = FlowType.INFLOW,
                        amount = inflowVal,
                        note = note
                    )
                }
                if (outflowVal > 0.0) {
                    result += CashFlowEntry(
                        periodLabel = periodLabel,
                        dateMillis = dateMillis,
                        category = category,
                        type = FlowType.OUTFLOW,
                        amount = kotlin.math.abs(outflowVal),
                        note = note
                    )
                }
            } else if (mapping.amountCol != null) {
                val rawAmt = parseSignedNumber(row.getOrNull(mapping.amountCol)) ?: return@forEachIndexed
                if (rawAmt == 0.0) return@forEachIndexed
                val typeStr = mapping.typeCol?.let { row.getOrNull(it)?.trim()?.lowercase(Locale.ROOT) }.orEmpty()
                val flowType = when {
                    rawAmt < 0 -> FlowType.OUTFLOW
                    typeStr.contains("out") ||
                        typeStr.contains("exp") ||
                        typeStr.contains("cost") ||
                        typeStr.contains("debit") ||
                        typeStr.contains("withdraw") ||
                        typeStr.contains("payment") ||
                        typeStr.contains("bill") ||
                        typeStr.contains("charge") ||
                        typeStr.contains("dép") ||
                        typeStr.contains("ausg") ||
                        typeStr.contains("خارج") ||
                        typeStr.contains("مصروف") ||
                        typeStr.contains("व्यय") ||
                        typeStr.contains("खर्च") -> FlowType.OUTFLOW
                    else -> FlowType.INFLOW
                }
                result += CashFlowEntry(
                    periodLabel = periodLabel,
                    dateMillis = dateMillis,
                    category = category,
                    type = flowType,
                    amount = kotlin.math.abs(rawAmt),
                    note = note
                )
            }
        }
        return result
    }

    private fun resolveFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    val name = cursor.getString(idx)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "cash_flow.xlsx"
    }

    private fun resolveSheetEntries(entries: Map<String, ByteArray>): List<Pair<String, String>> {
        val workbookBytes = entries["xl/workbook.xml"]
        val relsBytes = entries["xl/_rels/workbook.xml.rels"]
        if (workbookBytes != null && relsBytes != null) {
            val relMap = parseRelationships(relsBytes)
            val sheets = parseWorkbookSheets(workbookBytes)
            val resolved = sheets.mapNotNull { (name, rId) ->
                val target = relMap[rId]?.trimStart('/') ?: return@mapNotNull null
                val fullPath = if (target.startsWith("xl/")) target else "xl/$target"
                name to fullPath
            }
            if (resolved.isNotEmpty()) return resolved
        }
        return entries.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sorted()
            .mapIndexed { idx, path -> "Sheet ${idx + 1}" to path }
    }

    private fun parseRelationships(bytes: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (!id.isNullOrBlank() && !target.isNullOrBlank()) {
                    map[id] = target
                }
            }
            event = parser.next()
        }
        return map
    }

    private fun parseWorkbookSheets(bytes: ByteArray): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                val rId = (0 until parser.attributeCount).firstNotNullOfOrNull { idx ->
                    val attrName = parser.getAttributeName(idx)
                    if (attrName == "id" || attrName.endsWith(":id")) parser.getAttributeValue(idx) else null
                }
                if (!rId.isNullOrBlank()) {
                    list += name to rId
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var inSi = false
        var inT = false
        val current = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            current.clear()
                        }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inT) current.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inT = false
                        "si" -> {
                            strings += current.toString()
                            inSi = false
                        }
                    }
                }
            }
            event = parser.next()
        }
        return strings
    }

    private fun parseDateStyleIndices(bytes: ByteArray): Set<Int> {
        val customDateFormatIds = mutableSetOf<Int>()
        val dateStyleIndices = mutableSetOf<Int>()
        val builtInDateIds = setOf(14, 15, 16, 17, 18, 19, 20, 21, 22, 45, 46, 47)

        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var inCellXfs = false
        var xfIndex = 0
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "numFmt" -> {
                            val id = parser.getAttributeValue(null, "numFmtId")?.toIntOrNull()
                            val code = parser.getAttributeValue(null, "formatCode")?.lowercase(Locale.ROOT).orEmpty()
                            if (id != null && (code.contains('y') || code.contains("mm") || code.contains('d')) && !code.contains("0.00")) {
                                customDateFormatIds += id
                            }
                        }
                        "cellXfs" -> {
                            inCellXfs = true
                            xfIndex = 0
                        }
                        "xf" -> if (inCellXfs) {
                            val numFmtId = parser.getAttributeValue(null, "numFmtId")?.toIntOrNull()
                            if (numFmtId != null && (numFmtId in builtInDateIds || numFmtId in customDateFormatIds)) {
                                dateStyleIndices += xfIndex
                            }
                            xfIndex++
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "cellXfs") inCellXfs = false
                }
            }
            event = parser.next()
        }
        return dateStyleIndices
    }

    private fun parseWorksheetXml(
        bytes: ByteArray,
        sharedStrings: List<String>,
        dateStyleIndices: Set<Int>
    ): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var currentRow = mutableListOf<String>()
        var cellType: String? = null
        var cellStyle: Int? = null
        var cellColIndex = 0
        var inValue = false
        var inInlineText = false
        val valueBuilder = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                            cellColIndex = 0
                        }
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r")
                            cellColIndex = if (ref != null) columnIndexFromRef(ref) else currentRow.size
                            cellType = parser.getAttributeValue(null, "t")
                            cellStyle = parser.getAttributeValue(null, "s")?.toIntOrNull()
                            valueBuilder.clear()
                        }
                        "v" -> inValue = true
                        "t" -> if (cellType == "inlineStr") inInlineText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue || inInlineText) valueBuilder.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inValue = false
                        "t" -> inInlineText = false
                        "c" -> {
                            val raw = valueBuilder.toString().trim()
                            val resolved = when (cellType) {
                                "s" -> raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                                "inlineStr", "str" -> raw
                                "b" -> if (raw == "1") "TRUE" else "FALSE"
                                else -> {
                                    val numeric = raw.toDoubleOrNull()
                                    if (numeric != null && cellStyle != null && cellStyle in dateStyleIndices && numeric in 20000.0..80000.0) {
                                        formatExcelSerialDate(numeric)
                                    } else {
                                        raw
                                    }
                                }
                            }
                            while (currentRow.size <= cellColIndex) {
                                currentRow.add("")
                            }
                            currentRow[cellColIndex] = resolved
                        }
                        "row" -> rows += currentRow
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun columnIndexFromRef(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun formatExcelSerialDate(serial: Double): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, serial.toInt())
        }
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(cal.time)
    }

    private fun parseDelimitedText(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    currentRow.add(cell.toString().trim())
                    cell.clear()
                }
                (c == '\n' || c == '\r') && !inQuotes -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    currentRow.add(cell.toString().trim())
                    cell.clear()
                    if (currentRow.any { it.isNotBlank() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(cell.toString().trim())
            if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
        }
        return rows
    }

    private fun findBestIndex(lowerHeaders: List<String>, keywords: List<String>): Int? {
        for (keyword in keywords) {
            val exact = lowerHeaders.indexOfFirst { it == keyword }
            if (exact >= 0) return exact
        }
        for (keyword in keywords) {
            val partial = lowerHeaders.indexOfFirst { it.contains(keyword) }
            if (partial >= 0) return partial
        }
        return null
    }

    private fun detectNumericColumn(
        headers: List<String>,
        rows: List<List<String>>,
        exclude: Set<Int>
    ): Int? {
        return headers.indices.firstOrNull { col ->
            col !in exclude && rows.take(10).any { row -> parseSignedNumber(row.getOrNull(col)) != null }
        }
    }

    private fun parseNumber(raw: String?): Double? {
        val parsed = parseSignedNumber(raw) ?: return null
        return if (parsed < 0) kotlin.math.abs(parsed) else parsed
    }

    private fun parseSignedNumber(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        var cleaned = raw.trim()
        val isAccountingNeg = cleaned.startsWith("(") && cleaned.endsWith(")")
        if (isAccountingNeg) cleaned = cleaned.drop(1).dropLast(1)
        cleaned = cleaned.replace(Regex("[^0-9.,\\-]"), "")
        if (cleaned.isBlank() || cleaned == "-") return null

        // Handle European decimal comma e.g. 1.250,50 or 1250,50
        val normalized = when {
            cleaned.contains(',') && cleaned.contains('.') -> {
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "")
                }
            }
            cleaned.contains(',') && cleaned.substringAfterLast(',').length <= 2 -> {
                cleaned.replace(',', '.')
            }
            else -> cleaned.replace(",", "")
        }
        val num = normalized.toDoubleOrNull() ?: return null
        return if (isAccountingNeg) -kotlin.math.abs(num) else num
    }

    private fun normalizePeriodLabel(raw: String, fallbackIndex: Int): String {
        if (raw.isBlank()) return "P${fallbackIndex + 1}"
        // Match YYYY-MM-DD -> YYYY-MM
        val isoMatch = Regex("^(\\d{4})[-/](\\d{1,2})[-/]\\d{1,2}$").find(raw)
        if (isoMatch != null) {
            val y = isoMatch.groupValues[1]
            val m = isoMatch.groupValues[2].padStart(2, '0')
            return "$y-$m"
        }
        return raw.take(24)
    }

    private fun parseDateMillis(raw: String, fallback: Long): Long {
        if (raw.isBlank()) return fallback
        val patterns = listOf("yyyy-MM-dd", "yyyy-MM", "MM/dd/yyyy", "dd/MM/yyyy", "MMM yyyy", "MMM-yy", "MMM")
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(raw)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return fallback
    }
}
