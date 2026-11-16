package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CashFlowDao {

    @Transaction
    @Query("SELECT * FROM cash_flow_reports ORDER BY updatedAtMillis DESC")
    fun observeAllReports(): Flow<List<ReportWithEntries>>

    @Transaction
    @Query("SELECT * FROM cash_flow_reports WHERE id = :reportId LIMIT 1")
    suspend fun getReportById(reportId: String): ReportWithEntries?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: CashFlowReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CashFlowEntryEntity>)

    @Query("DELETE FROM cash_flow_entries WHERE reportId = :reportId")
    suspend fun deleteEntriesForReport(reportId: String)

    @Transaction
    suspend fun upsertReportWithEntries(
        report: CashFlowReportEntity,
        entries: List<CashFlowEntryEntity>
    ) {
        insertReport(report)
        deleteEntriesForReport(report.id)
        if (entries.isNotEmpty()) {
            insertEntries(entries)
        }
    }

    @Query("UPDATE cash_flow_reports SET title = :title, updatedAtMillis = :updatedAtMillis WHERE id = :reportId")
    suspend fun renameReport(reportId: String, title: String, updatedAtMillis: Long)

    @Query("DELETE FROM cash_flow_reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: String)
}
