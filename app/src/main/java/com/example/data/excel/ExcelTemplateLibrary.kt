package com.example.data.excel

import android.content.Context
import android.net.Uri
import com.example.model.CashFlowEntry
import com.example.model.CashFlowReport
import com.example.model.FlowType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExcelSampleTemplate(
    val id: String,
    val titleKey: String,
    val subtitleKey: String,
    val fileName: String,
    val currencyCode: String,
    val reportBuilder: () -> CashFlowReport
)

object ExcelTemplateLibrary {

    val templates: List<ExcelSampleTemplate> = listOf(
        ExcelSampleTemplate(
            id = "saas_startup",
            titleKey = "saas",
            subtitleKey = "saas_sub",
            fileName = "novie_saas_q1_q4_cashflow.xlsx",
            currencyCode = "USD",
            reportBuilder = ::buildSaasReport
        ),
        ExcelSampleTemplate(
            id = "retail_commerce",
            titleKey = "retail",
            subtitleKey = "retail_sub",
            fileName = "novie_retail_monthly_ledger.xlsx",
            currencyCode = "EUR",
            reportBuilder = ::buildRetailReport
        ),
        ExcelSampleTemplate(
            id = "creative_studio",
            titleKey = "studio",
            subtitleKey = "studio_sub",
            fileName = "novie_studio_annual_flow.xlsx",
            currencyCode = "INR",
            reportBuilder = ::buildStudioReport
        )
    )

    private fun monthMillis(year: Int, monthZeroBased: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, monthZeroBased, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun buildSaasReport(): CashFlowReport {
        val entries = mutableListOf<CashFlowEntry>()
        val months = listOf(
            "2026-01" to 0, "2026-02" to 1, "2026-03" to 2,
            "2026-04" to 3, "2026-05" to 4, "2026-06" to 5,
            "2026-07" to 6, "2026-08" to 7, "2026-09" to 8
        )
        val subRev = listOf(42000.0, 46500.0, 51200.0, 54800.0, 61000.0, 66400.0, 72000.0, 78500.0, 86000.0)
        val entRev = listOf(18000.0, 12000.0, 26000.0, 21000.0, 32000.0, 28000.0, 36000.0, 42000.0, 49000.0)
        val payroll = listOf(31000.0, 31000.0, 34000.0, 34000.0, 36500.0, 36500.0, 39000.0, 39000.0, 41000.0)
        val cloudOps = listOf(7800.0, 8200.0, 8900.0, 9400.0, 10200.0, 11000.0, 11400.0, 12100.0, 12900.0)
        val marketing = listOf(9500.0, 11000.0, 14500.0, 12000.0, 16000.0, 13500.0, 15000.0, 18000.0, 19500.0)
        val office = listOf(4200.0, 4200.0, 4400.0, 4300.0, 4500.0, 4600.0, 4500.0, 4700.0, 4800.0)

        months.forEachIndexed { i, (label, m) ->
            val ts = monthMillis(2026, m)
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Subscriptions", type = FlowType.INFLOW, amount = subRev[i], note = "Recurring SaaS MRR")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Enterprise Deals", type = FlowType.INFLOW, amount = entRev[i], note = "Annual contracts")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Payroll", type = FlowType.OUTFLOW, amount = payroll[i], note = "Core team")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Cloud Infra", type = FlowType.OUTFLOW, amount = cloudOps[i], note = "Hosting & AI compute")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Marketing", type = FlowType.OUTFLOW, amount = marketing[i], note = "Growth campaigns")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Operations", type = FlowType.OUTFLOW, amount = office[i], note = "Legal & SaaS tools")
        }

        return CashFlowReport(
            id = "sample_saas_2026",
            title = "SaaS Startup Cash Flow",
            sourceFileName = "novie_saas_q1_q4_cashflow.xlsx",
            sheetName = "CashFlow_2026",
            baseCurrencyCode = "USD",
            entries = entries
        )
    }

    private fun buildRetailReport(): CashFlowReport {
        val entries = mutableListOf<CashFlowEntry>()
        val months = listOf("2026-01" to 0, "2026-02" to 1, "2026-03" to 2, "2026-04" to 3, "2026-05" to 4, "2026-06" to 5)
        val store = listOf(28500.0, 26000.0, 31200.0, 34000.0, 38500.0, 42100.0)
        val online = listOf(19200.0, 21500.0, 24800.0, 27900.0, 33400.0, 37800.0)
        val inventory = listOf(21000.0, 19500.0, 23000.0, 24500.0, 27000.0, 29500.0)
        val rent = listOf(6800.0, 6800.0, 6800.0, 6800.0, 6800.0, 6800.0)
        val logistics = listOf(4100.0, 4300.0, 4900.0, 5200.0, 6100.0, 6500.0)
        val staff = listOf(9200.0, 9200.0, 9800.0, 9800.0, 10500.0, 10800.0)

        months.forEachIndexed { i, (label, m) ->
            val ts = monthMillis(2026, m)
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Store POS", type = FlowType.INFLOW, amount = store[i], note = "Flagship retail")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "E-Commerce", type = FlowType.INFLOW, amount = online[i], note = "Direct webstore")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Inventory COGS", type = FlowType.OUTFLOW, amount = inventory[i], note = "Supplier restock")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Lease & Utilities", type = FlowType.OUTFLOW, amount = rent[i], note = "Storefront lease")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Logistics", type = FlowType.OUTFLOW, amount = logistics[i], note = "Fulfillment & shipping")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Store Staff", type = FlowType.OUTFLOW, amount = staff[i], note = "Shifts & commissions")
        }

        return CashFlowReport(
            id = "sample_retail_2026",
            title = "Retail & E-Commerce Ledger",
            sourceFileName = "novie_retail_monthly_ledger.xlsx",
            sheetName = "Monthly_Ledger",
            baseCurrencyCode = "EUR",
            entries = entries
        )
    }

    private fun buildStudioReport(): CashFlowReport {
        val entries = mutableListOf<CashFlowEntry>()
        val months = listOf("2026-01" to 0, "2026-02" to 1, "2026-03" to 2, "2026-04" to 3, "2026-05" to 4, "2026-06" to 5)
        val retainers = listOf(520000.0, 560000.0, 580000.0, 620000.0, 640000.0, 700000.0)
        val sprints = listOf(280000.0, 190000.0, 340000.0, 410000.0, 290000.0, 460000.0)
        val contractors = listOf(210000.0, 185000.0, 240000.0, 260000.0, 230000.0, 275000.0)
        val payroll = listOf(240000.0, 240000.0, 255000.0, 255000.0, 265000.0, 265000.0)
        val software = listOf(45000.0, 48000.0, 52000.0, 50000.0, 54000.0, 58000.0)
        val taxReserve = listOf(80000.0, 75000.0, 92000.0, 103000.0, 93000.0, 116000.0)

        months.forEachIndexed { i, (label, m) ->
            val ts = monthMillis(2026, m)
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Client Retainers", type = FlowType.INFLOW, amount = retainers[i], note = "Monthly design retainers")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Sprint Milestones", type = FlowType.INFLOW, amount = sprints[i], note = "Product launches")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Contractors", type = FlowType.OUTFLOW, amount = contractors[i], note = "3D & motion specialists")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Core Payroll", type = FlowType.OUTFLOW, amount = payroll[i], note = "Full-time leads")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Software & AI", type = FlowType.OUTFLOW, amount = software[i], note = "Design & cloud suite")
            entries += CashFlowEntry(periodLabel = label, dateMillis = ts, category = "Tax Reserve", type = FlowType.OUTFLOW, amount = taxReserve[i], note = "GST & corporate reserve")
        }

        return CashFlowReport(
            id = "sample_studio_2026",
            title = "Creative Studio Annual Flow",
            sourceFileName = "novie_studio_annual_flow.xlsx",
            sheetName = "Annual_Flow",
            baseCurrencyCode = "INR",
            entries = entries
        )
    }

    suspend fun exportTemplateAsXlsx(context: Context, uri: Uri, template: ExcelSampleTemplate): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val report = template.reportBuilder()
                val xlsxBytes = buildMinimalXlsx(report)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(xlsxBytes)
                } ?: throw IllegalStateException("Could not open output stream for Excel export.")
            }
        }

    fun buildMinimalXlsx(report: CashFlowReport): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun addEntry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            addEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
            )
            addEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )
            addEntry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(report.sheetName)}" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
            )
            addEntry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""
            )

            val rowsXml = StringBuilder()
            rowsXml.append(
                """<row r="1">
  <c r="A1" t="inlineStr"><is><t>Period</t></is></c>
  <c r="B1" t="inlineStr"><is><t>Category</t></is></c>
  <c r="C1" t="inlineStr"><is><t>Type</t></is></c>
  <c r="D1" t="inlineStr"><is><t>Amount</t></is></c>
  <c r="E1" t="inlineStr"><is><t>Note</t></is></c>
</row>"""
            )
            report.entries.forEachIndexed { idx, entry ->
                val rowNum = idx + 2
                rowsXml.append(
                    """<row r="$rowNum">
  <c r="A$rowNum" t="inlineStr"><is><t>${escapeXml(entry.periodLabel)}</t></is></c>
  <c r="B$rowNum" t="inlineStr"><is><t>${escapeXml(entry.category)}</t></is></c>
  <c r="C$rowNum" t="inlineStr"><is><t>${entry.type.name}</t></is></c>
  <c r="D$rowNum"><v>${entry.amount}</v></c>
  <c r="E$rowNum" t="inlineStr"><is><t>${escapeXml(entry.note)}</t></is></c>
</row>"""
                )
            }
            addEntry(
                "xl/worksheets/sheet1.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>$rowsXml</sheetData>
</worksheet>"""
            )
        }
        return out.toByteArray()
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
