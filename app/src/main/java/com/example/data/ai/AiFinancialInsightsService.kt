package com.example.data.ai

import android.util.Log
import com.example.model.AnalyticsSummary
import com.example.model.AppCurrency
import com.example.model.AppLanguage
import com.example.model.CashFlowReport
import com.example.model.CurrencyFormatter
import com.example.model.FinancialInsight
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

class AiFinancialInsightsService {

    private val insightSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(description = "One concise executive headline summarizing cash flow health."),
            "healthBadge" to Schema.string(description = "Short 1-2 word status badge such as Strong Surplus, Balanced, or Burn Alert."),
            "runwayObservation" to Schema.string(description = "One short sentence on monthly burn rate, margin, or cash trajectory."),
            "keyFindings" to Schema.array(
                items = Schema.string(),
                description = "3 concise data-backed observations (under 20 words each) citing actual categories and periods."
            ),
            "recommendations" to Schema.array(
                items = Schema.string(),
                description = "3 actionable business recommendations (under 20 words each) to improve net cash flow."
            )
        )
    )

    private val insightGenerationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = insightSchema
    }

    private val primaryModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL_NAME,
            generationConfig = insightGenerationConfig
        )
    }

    private val fallbackModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL_NAME,
            generationConfig = insightGenerationConfig
        )
    }

    suspend fun analyzeReport(
        report: CashFlowReport,
        summary: AnalyticsSummary,
        currency: AppCurrency,
        language: AppLanguage
    ): Result<FinancialInsight> = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("You are Novie, an executive CFO financial analyst.")
            appendLine("Respond strictly in ${language.englishName} (${language.nativeLabel}). Keep responses crisp and concise.")
            appendLine("All currency figures must be expressed in ${currency.code} (${currency.symbol}).")
            appendLine("Report Title: ${report.title}")
            appendLine("Total Inflow: ${CurrencyFormatter.formatFull(summary.totalInflow, currency, Locale.US)}")
            appendLine("Total Outflow: ${CurrencyFormatter.formatFull(summary.totalOutflow, currency, Locale.US)}")
            appendLine("Net Cash Flow: ${CurrencyFormatter.formatFull(summary.netCashFlow, currency, Locale.US)}")
            appendLine("Operating Margin: ${String.format(Locale.US, "%.1f", summary.operatingMarginPct)}%")
            appendLine("Avg Monthly Burn: ${CurrencyFormatter.formatFull(summary.avgMonthlyBurn, currency, Locale.US)}")
            appendLine("Periods:")
            summary.periods.take(12).forEach { p ->
                appendLine(
                    "- ${p.periodLabel}: In=${CurrencyFormatter.formatFull(p.inflow, currency, Locale.US)}, " +
                        "Out=${CurrencyFormatter.formatFull(p.outflow, currency, Locale.US)}, " +
                        "Net=${CurrencyFormatter.formatFull(p.net, currency, Locale.US)}"
                )
            }
            appendLine("Top Outflows:")
            summary.outflowCategories.take(5).forEach { c ->
                appendLine("- ${c.category}: ${CurrencyFormatter.formatFull(c.amount, currency, Locale.US)} (${String.format(Locale.US, "%.1f", c.percentage)}%)")
            }
            appendLine("Top Inflows:")
            summary.inflowCategories.take(5).forEach { c ->
                appendLine("- ${c.category}: ${CurrencyFormatter.formatFull(c.amount, currency, Locale.US)} (${String.format(Locale.US, "%.1f", c.percentage)}%)")
            }
        }

        try {
            val insight = generateInsightWithStream(primaryModel, prompt, PRIMARY_TIMEOUT_MS)
            Result.success(insight)
        } catch (primaryTimeout: TimeoutCancellationException) {
            Log.w(TAG, "Primary AI model ($PRIMARY_MODEL_NAME) timed out after ${PRIMARY_TIMEOUT_MS}ms; retrying with $FALLBACK_MODEL_NAME", primaryTimeout)
            try {
                val fallbackInsight = generateInsightWithStream(fallbackModel, prompt, FALLBACK_TIMEOUT_MS)
                Result.success(fallbackInsight)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI insight generation timed out on both primary and fallback models", e)
                Result.failure(IllegalStateException("Analysis timed out. Please check your connection and retry.", e))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fallback AI insight generation failed", e)
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI insight generation failed", e)
            Result.failure(e)
        }
    }

    private suspend fun generateInsightWithStream(
        model: GenerativeModel,
        prompt: String,
        timeoutMs: Long
    ): FinancialInsight {
        val rawJson = withTimeout(timeoutMs) {
            val accumulated = StringBuilder()
            model.generateContentStream(prompt).collect { chunk ->
                chunk.text?.let { accumulated.append(it) }
            }
            accumulated.toString().trim()
        }
        if (rawJson.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
        val json = JSONObject(rawJson)
        val findingsArr = json.getJSONArray("keyFindings")
        val recsArr = json.getJSONArray("recommendations")
        return FinancialInsight(
            headline = json.getString("headline"),
            healthBadge = json.getString("healthBadge"),
            runwayObservation = json.getString("runwayObservation"),
            keyFindings = (0 until findingsArr.length()).map { findingsArr.getString(it) },
            recommendations = (0 until recsArr.length()).map { recsArr.getString(it) }
        )
    }

    private companion object {
        const val TAG = "AiFinancialInsights"
        const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
        const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"
        const val PRIMARY_TIMEOUT_MS = 30_000L
        const val FALLBACK_TIMEOUT_MS = 30_000L
    }
}
