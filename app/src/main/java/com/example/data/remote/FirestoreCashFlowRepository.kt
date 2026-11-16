package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.model.CashFlowEntry
import com.example.model.CashFlowReport
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreCashFlowRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must sign in with Google before accessing cloud sync.")
    }

    fun observeReports(userId: String): Flow<List<CashFlowReport>> = flow {
        val path = "users/$userId/cashFlowReports"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("cashFlowReports")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                CashFlowReportDocument::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )?.toDomain()
                        }
                        .sortedByDescending { it.updatedAtMillis }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createReport(report: CashFlowReport): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = report.id
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(120)
            .ifBlank { UUID.randomUUID().toString() }
        val docRef = db.collection("users")
            .document(uid)
            .collection("cashFlowReports")
            .document(docId)

        val boundedEntries = compactEntriesIfNeeded(report.entries)
        val payload = mapOf(
            "userId" to uid,
            "title" to report.title.trim().ifBlank { "Cash Flow Report" }.take(120),
            "sourceFileName" to report.sourceFileName.ifBlank { "ledger.xlsx" }.take(160),
            "sheetName" to report.sheetName.ifBlank { "Sheet1" }.take(80),
            "baseCurrencyCode" to report.baseCurrencyCode.ifBlank { "USD" }.take(6),
            "totalInflow" to report.totalInflow.coerceAtLeast(0.0),
            "totalOutflow" to report.totalOutflow.coerceAtLeast(0.0),
            "entryCount" to boundedEntries.size,
            "entries" to boundedEntries.map { CashFlowEntryDto.fromDomain(it).toMap() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getReportById(reportId: String): Result<CashFlowReport?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users")
            .document(uid)
            .collection("cashFlowReports")
            .document(reportId)
        return try {
            val snap = docRef.get().await()
            val domain = snap.toObject(
                CashFlowReportDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.toDomain()
            Result.success(domain)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getReportByPath(ownerUid: String, reportId: String): Result<CashFlowReport?> {
        val docRef = db.collection("users")
            .document(ownerUid)
            .collection("cashFlowReports")
            .document(reportId)
        return try {
            val snap = docRef.get().await()
            val domain = snap.toObject(
                CashFlowReportDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.toDomain()
            Result.success(domain)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateReportTitle(reportId: String, newTitle: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val sanitized = newTitle.trim().ifBlank { "Cash Flow Report" }.take(120)
        val docRef = db.collection("users")
            .document(uid)
            .collection("cashFlowReports")
            .document(reportId)
        return try {
            docRef.update(
                mapOf(
                    "title" to sanitized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteReport(reportId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users")
            .document(uid)
            .collection("cashFlowReports")
            .document(reportId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun compactEntriesIfNeeded(entries: List<CashFlowEntry>): List<CashFlowEntry> {
        if (entries.size <= MAX_CLOUD_ENTRIES) return entries
        return entries
            .groupBy { Triple(it.periodLabel, it.category, it.type) }
            .map { (key, grouped) ->
                CashFlowEntry(
                    periodLabel = key.first,
                    dateMillis = grouped.minOf { it.dateMillis },
                    category = key.second,
                    type = key.third,
                    amount = grouped.sumOf { it.amount },
                    note = "${grouped.size} consolidated entries"
                )
            }
            .take(MAX_CLOUD_ENTRIES)
    }

    private companion object {
        const val MAX_CLOUD_ENTRIES = 400
    }
}
