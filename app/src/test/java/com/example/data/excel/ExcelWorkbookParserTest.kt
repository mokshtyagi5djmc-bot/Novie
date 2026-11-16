package com.example.data.excel

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppCurrency
import com.example.model.CashFlowAnalyticsCalculator
import com.example.model.FlowType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ExcelWorkbookParserTest {

    @Test
    fun buildMinimalXlsx_roundTripsThroughParserAndAnalytics() {
        val original = ExcelTemplateLibrary.templates.first().reportBuilder()
        val xlsxBytes = ExcelTemplateLibrary.buildMinimalXlsx(original)

        val workbook = ExcelWorkbookParser.parseXlsxBytes("test_cashflow.xlsx", xlsxBytes)
        assertEquals(1, workbook.sheets.size)
        val sheet = workbook.sheets.first()
        assertEquals(original.sheetName, sheet.name)

        val mapping = ExcelWorkbookParser.autoDetectMapping(sheet.headers, sheet.rows)
        val parsedEntries = ExcelWorkbookParser.convertRowsToEntries(sheet, mapping)

        assertEquals(original.entries.size, parsedEntries.size)
        val parsedInflow = parsedEntries.filter { it.type == FlowType.INFLOW }.sumOf { it.amount }
        val parsedOutflow = parsedEntries.filter { it.type == FlowType.OUTFLOW }.sumOf { it.amount }
        assertEquals(original.totalInflow, parsedInflow, 0.01)
        assertEquals(original.totalOutflow, parsedOutflow, 0.01)

        val summary = CashFlowAnalyticsCalculator.compute(
            report = original,
            targetCurrency = AppCurrency.USD,
            convertUsingRates = true
        )
        assertEquals(original.totalInflow, summary.totalInflow, 0.01)
        assertEquals(original.totalOutflow, summary.totalOutflow, 0.01)
        assertTrue(summary.periods.isNotEmpty())
        assertTrue(summary.outflowCategories.isNotEmpty())
    }

    @Test
    fun parseCsv_withInflowAndOutflowColumns_mapsCorrectly() {
        val csv = """
            Month,Category,Inflow,Outflow,Note
            2026-01,Sales,50000,0,Q1 Launch
            2026-01,Payroll,0,22000,Team salaries
            2026-02,Marketing,0,"4,500.50",Ads
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val workbook = ExcelWorkbookParser.parseDelimitedBytes("ledger.csv", csv)
        val sheet = workbook.sheets.first()
        val mapping = ExcelWorkbookParser.autoDetectMapping(sheet.headers, sheet.rows)
        val entries = ExcelWorkbookParser.convertRowsToEntries(sheet, mapping)

        assertEquals(3, entries.size)
        assertEquals(50000.0, entries.filter { it.type == FlowType.INFLOW }.sumOf { it.amount }, 0.01)
        assertEquals(26500.5, entries.filter { it.type == FlowType.OUTFLOW }.sumOf { it.amount }, 0.01)
    }

    @Test
    fun parseUri_readsXlsxAndCsvFromContentResolver() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original = ExcelTemplateLibrary.templates.first().reportBuilder()
        val xlsxFile = File.createTempFile("cashflow_test_", ".xlsx", context.cacheDir)
        val csvFile = File.createTempFile("cashflow_test_", ".csv", context.cacheDir)
        try {
            xlsxFile.writeBytes(ExcelTemplateLibrary.buildMinimalXlsx(original))
            csvFile.writeText(
                "Month;Category;Amount;Type\n2026-03;Subscriptions;1.250,50;Inflow\n2026-03;Cloud;-450,25;Outflow\n",
                Charsets.UTF_8
            )

            val xlsxResult = ExcelWorkbookParser.parseUri(context, Uri.fromFile(xlsxFile))
            assertTrue(xlsxResult.isSuccess)
            assertEquals(original.entries.size, xlsxResult.getOrThrow().sheets.first().rows.size)

            val csvResult = ExcelWorkbookParser.parseUri(context, Uri.fromFile(csvFile))
            assertTrue(csvResult.isSuccess)
            val sheet = csvResult.getOrThrow().sheets.first()
            val mapping = ExcelWorkbookParser.autoDetectMapping(sheet.headers, sheet.rows)
            val entries = ExcelWorkbookParser.convertRowsToEntries(sheet, mapping)
            assertEquals(2, entries.size)
            assertEquals(1250.50, entries.first { it.type == FlowType.INFLOW }.amount, 0.01)
            assertEquals(450.25, entries.first { it.type == FlowType.OUTFLOW }.amount, 0.01)
        } finally {
            xlsxFile.delete()
            csvFile.delete()
        }
    }

    @Test
    fun parseDelimitedBytes_supportsUtf16LeBom_andParseUriRejectsPdfBinary() = runTest {
        val csvText = "Period,Category,Amount\n2026-04,Consulting,3200\n2026-04,Hosting,-180\n"
        val utf16Bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + csvText.toByteArray(Charsets.UTF_16LE)
        val workbook = ExcelWorkbookParser.parseDelimitedBytes("unicode_export.csv", utf16Bytes)
        val sheet = workbook.sheets.first()
        val entries = ExcelWorkbookParser.convertRowsToEntries(
            sheet,
            ExcelWorkbookParser.autoDetectMapping(sheet.headers, sheet.rows)
        )
        assertEquals(2, entries.size)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val pdfFile = File.createTempFile("not_excel_", ".pdf", context.cacheDir)
        try {
            pdfFile.writeBytes("%PDF-1.4\n1 0 obj\n".toByteArray(Charsets.US_ASCII))
            val result = ExcelWorkbookParser.parseUri(context, Uri.fromFile(pdfFile))
            assertTrue(result.isFailure)
        } finally {
            pdfFile.delete()
        }
    }
}
