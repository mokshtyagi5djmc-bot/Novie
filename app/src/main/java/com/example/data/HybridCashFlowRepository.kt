package com.example.data

import com.example.data.local.LocalCashFlowRepository
import com.example.data.remote.FirestoreCashFlowRepository
import com.example.model.CashFlowReport
import com.example.model.StorageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class HybridCashFlowRepository(
    private val localRepo: LocalCashFlowRepository,
    private val remoteRepo: FirestoreCashFlowRepository
) {
    fun observeReports(mode: StorageMode, authenticatedUserId: String?): Flow<List<CashFlowReport>> {
        return when (mode) {
            StorageMode.PRIVATE_LOCAL -> localRepo.observeReports()
            StorageMode.GOOGLE_SYNC -> {
                if (authenticatedUserId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    remoteRepo.observeReports(authenticatedUserId)
                }
            }
        }
    }

    suspend fun saveReport(
        report: CashFlowReport,
        mode: StorageMode,
        authenticatedUserId: String?
    ): Result<String> {
        return when (mode) {
            StorageMode.PRIVATE_LOCAL -> {
                runCatching { localRepo.saveReport(report) }
            }
            StorageMode.GOOGLE_SYNC -> {
                if (authenticatedUserId.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Sign in with Google to save reports to Google Sync."))
                } else {
                    remoteRepo.createReport(report)
                }
            }
        }
    }

    suspend fun renameReport(
        reportId: String,
        newTitle: String,
        mode: StorageMode,
        authenticatedUserId: String?
    ): Result<Unit> {
        return when (mode) {
            StorageMode.PRIVATE_LOCAL -> runCatching { localRepo.renameReport(reportId, newTitle) }
            StorageMode.GOOGLE_SYNC -> {
                if (authenticatedUserId.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Google Sign-In required."))
                } else {
                    remoteRepo.updateReportTitle(reportId, newTitle)
                }
            }
        }
    }

    suspend fun deleteReport(
        reportId: String,
        mode: StorageMode,
        authenticatedUserId: String?
    ): Result<Unit> {
        return when (mode) {
            StorageMode.PRIVATE_LOCAL -> runCatching { localRepo.deleteReport(reportId) }
            StorageMode.GOOGLE_SYNC -> {
                if (authenticatedUserId.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Google Sign-In required."))
                } else {
                    remoteRepo.deleteReport(reportId)
                }
            }
        }
    }

    suspend fun syncLocalReportsToCloud(): Result<Int> {
        val localReports = localRepo.observeReports().first()
        var syncedCount = 0
        for (report in localReports) {
            val res = remoteRepo.createReport(report)
            if (res.isFailure) {
                return Result.failure(res.exceptionOrNull() ?: IllegalStateException("Sync failed"))
            }
            syncedCount++
        }
        return Result.success(syncedCount)
    }
}
