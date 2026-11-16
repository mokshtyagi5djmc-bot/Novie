package com.example.data.remote

import com.example.model.CashFlowEntry
import com.example.model.CashFlowReport
import com.example.model.FlowType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CashFlowEntryDto(
    val periodLabel: String = "",
    val dateMillis: Long = 0L,
    val category: String = "",
    val type: String = FlowType.INFLOW.name,
    val amount: Double = 0.0,
    val note: String = ""
) {
    fun toDomain(): CashFlowEntry = CashFlowEntry(
        periodLabel = periodLabel.ifBlank { "Period" },
        dateMillis = dateMillis,
        category = category.ifBlank { "General" },
        type = if (type.equals(FlowType.OUTFLOW.name, ignoreCase = true)) FlowType.OUTFLOW else FlowType.INFLOW,
        amount = amount.coerceAtLeast(0.0),
        note = note
    )

    fun toMap(): Map<String, Any> = mapOf(
        "periodLabel" to periodLabel,
        "dateMillis" to dateMillis,
        "category" to category,
        "type" to type,
        "amount" to amount,
        "note" to note
    )

    companion object {
        fun fromDomain(entry: CashFlowEntry): CashFlowEntryDto = CashFlowEntryDto(
            periodLabel = entry.periodLabel.take(40),
            dateMillis = entry.dateMillis,
            category = entry.category.take(60),
            type = entry.type.name,
            amount = entry.amount.coerceAtLeast(0.0),
            note = entry.note.take(120)
        )
    }
}

@IgnoreExtraProperties
data class CashFlowReportDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val sourceFileName: String = "",
    val sheetName: String = "",
    val baseCurrencyCode: String = "USD",
    val totalInflow: Double = 0.0,
    val totalOutflow: Double = 0.0,
    val entryCount: Long = 0L,
    val entries: List<CashFlowEntryDto> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): CashFlowReport {
        val createdMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val updatedMillis = updatedAt?.toDate()?.time ?: createdMillis
        return CashFlowReport(
            id = id,
            title = title,
            sourceFileName = sourceFileName,
            sheetName = sheetName,
            baseCurrencyCode = baseCurrencyCode,
            entries = entries.map { it.toDomain() },
            createdAtMillis = createdMillis,
            updatedAtMillis = updatedMillis,
            isCloudBacked = true
        )
    }
}
