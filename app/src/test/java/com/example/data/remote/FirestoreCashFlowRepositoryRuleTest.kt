package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.CashFlowEntry
import com.example.model.CashFlowReport
import com.example.model.FlowType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreCashFlowRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleReport(id: String = UUID.randomUUID().toString()): CashFlowReport {
        return CashFlowReport(
            id = id,
            title = "SaaS Q1 Cash Flow",
            sourceFileName = "saas_q1.xlsx",
            sheetName = "CashFlow",
            baseCurrencyCode = "USD",
            entries = listOf(
                CashFlowEntry(
                    periodLabel = "2026-01",
                    dateMillis = 1767225600000L,
                    category = "Subscriptions",
                    type = FlowType.INFLOW,
                    amount = 45000.0,
                    note = "MRR"
                ),
                CashFlowEntry(
                    periodLabel = "2026-01",
                    dateMillis = 1767225600000L,
                    category = "Payroll",
                    type = FlowType.OUTFLOW,
                    amount = 22000.0,
                    note = "Engineering"
                )
            )
        )
    }

    @Test
    fun createAndObserveReport_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCashFlowRepository(firestore, auth)
        val report = sampleReport()

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createReport(report) }
        assertTrue(createResult.isSuccess)
        val reportId = createResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeReports(aliceUid).first { list -> list.any { it.id == reportId } }
        }
        val fetched = emitted.first { it.id == reportId }
        assertEquals("SaaS Q1 Cash Flow", fetched.title)
        assertEquals(2, fetched.entries.size)
    }

    @Test
    fun updateAndDeleteReport_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreCashFlowRepository(firestore, auth)
        val reportId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createReport(sampleReport()).getOrThrow()
        }

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateReportTitle(reportId, "Renamed Q1 Ledger")
        }
        assertTrue(updateResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getReportById(reportId).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals("Renamed Q1 Ledger", fetched?.title)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.deleteReport(reportId) }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun crossUserAccess_deniedBySecurityRules() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreCashFlowRepository(firestore, auth)
        val reportId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createReport(sampleReport()).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreCashFlowRepository(firestore, auth)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getReportByPath(aliceUid, reportId)
        }
        assertTrue(crossReadResult.isFailure)
        val firestoreException = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun unauthenticatedObserve_deniedBySecurityRules() = runBlocking {
        auth.signOut()
        val repository = FirestoreCashFlowRepository(firestore, auth)
        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeReports("some_user_123").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_novie@test.com"
        const val BOB_EMAIL = "bob_novie@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
