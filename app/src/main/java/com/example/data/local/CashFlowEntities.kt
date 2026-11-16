package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.model.CashFlowEntry
import com.example.model.CashFlowReport
import com.example.model.FlowType

@Entity(tableName = "cash_flow_reports")
data class CashFlowReportEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceFileName: String,
    val sheetName: String,
    val baseCurrencyCode: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "cash_flow_entries",
    foreignKeys = [
        ForeignKey(
            entity = CashFlowReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reportId")]
)
data class CashFlowEntryEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val periodLabel: String,
    val dateMillis: Long,
    val category: String,
    val type: String,
    val amount: Double,
    val note: String
)

data class ReportWithEntries(
    @Embedded val report: CashFlowReportEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "reportId"
    )
    val entries: List<CashFlowEntryEntity>
) {
    fun toDomain(): CashFlowReport = CashFlowReport(
        id = report.id,
        title = report.title,
        sourceFileName = report.sourceFileName,
        sheetName = report.sheetName,
        baseCurrencyCode = report.baseCurrencyCode,
        entries = entries.sortedBy { it.dateMillis }.map {
            CashFlowEntry(
                id = it.id,
                periodLabel = it.periodLabel,
                dateMillis = it.dateMillis,
                category = it.category,
                type = if (it.type == FlowType.OUTFLOW.name) FlowType.OUTFLOW else FlowType.INFLOW,
                amount = it.amount,
                note = it.note
            )
        },
        createdAtMillis = report.createdAtMillis,
        updatedAtMillis = report.updatedAtMillis,
        isCloudBacked = false
    )
}
