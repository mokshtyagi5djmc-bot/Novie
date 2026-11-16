package com.example.data.local

import com.example.model.CashFlowReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalCashFlowRepository(private val dao: CashFlowDao) {

    fun observeReports(): Flow<List<CashFlowReport>> =
        dao.observeAllReports().map { list -> list.map { it.toDomain() } }

    suspend fun saveReport(report: CashFlowReport): String {
        val now = System.currentTimeMillis()
        val id = report.id.ifBlank { UUID.randomUUID().toString() }
        val reportEntity = CashFlowReportEntity(
            id = id,
            title = report.title.trim().ifBlank { "Cash Flow Report" },
            sourceFileName = report.sourceFileName,
            sheetName = report.sheetName,
            baseCurrencyCode = report.baseCurrencyCode,
            createdAtMillis = report.createdAtMillis,
            updatedAtMillis = now
        )
        val entryEntities = report.entries.map { entry ->
            CashFlowEntryEntity(
                id = entry.id.ifBlank { UUID.randomUUID().toString() },
                reportId = id,
                periodLabel = entry.periodLabel,
                dateMillis = entry.dateMillis,
                category = entry.category,
                type = entry.type.name,
                amount = entry.amount,
                note = entry.note
            )
        }
        dao.upsertReportWithEntries(reportEntity, entryEntities)
        return id
    }

    suspend fun renameReport(reportId: String, newTitle: String) {
        dao.renameReport(reportId, newTitle.trim().ifBlank { "Cash Flow Report" }, System.currentTimeMillis())
    }

    suspend fun deleteReport(reportId: String) {
        dao.deleteReportById(reportId)
    }
}
