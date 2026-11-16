package com.example.model

import java.util.UUID

enum class FlowType {
    INFLOW,
    OUTFLOW
}

data class CashFlowEntry(
    val id: String = UUID.randomUUID().toString(),
    val periodLabel: String,
    val dateMillis: Long,
    val category: String,
    val type: FlowType,
    val amount: Double,
    val note: String = ""
)

data class CashFlowReport(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sourceFileName: String,
    val sheetName: String,
    val baseCurrencyCode: String,
    val entries: List<CashFlowEntry>,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val isCloudBacked: Boolean = false
) {
    val totalInflow: Double get() = entries.filter { it.type == FlowType.INFLOW }.sumOf { it.amount }
    val totalOutflow: Double get() = entries.filter { it.type == FlowType.OUTFLOW }.sumOf { it.amount }
    val netCashFlow: Double get() = totalInflow - totalOutflow
}

data class PeriodSummary(
    val periodLabel: String,
    val sortKey: Long,
    val inflow: Double,
    val outflow: Double,
    val net: Double,
    val cumulativeBalance: Double,
    val categoryOutflows: Map<String, Double>,
    val categoryInflows: Map<String, Double>
)

data class CategorySlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val type: FlowType
)

data class AnalyticsSummary(
    val totalInflow: Double,
    val totalOutflow: Double,
    val netCashFlow: Double,
    val operatingMarginPct: Double,
    val avgMonthlyBurn: Double,
    val savingsRatioPct: Double,
    val periods: List<PeriodSummary>,
    val outflowCategories: List<CategorySlice>,
    val inflowCategories: List<CategorySlice>,
    val topOutflowCategoryNames: List<String>
)

data class FinancialInsight(
    val headline: String,
    val healthBadge: String,
    val runwayObservation: String,
    val keyFindings: List<String>,
    val recommendations: List<String>
)

object CashFlowAnalyticsCalculator {

    fun compute(
        report: CashFlowReport,
        targetCurrency: AppCurrency,
        convertUsingRates: Boolean
    ): AnalyticsSummary {
        val convertedEntries = report.entries.map { entry ->
            entry.copy(
                amount = CurrencyFormatter.convertAmount(
                    amount = entry.amount,
                    fromCode = report.baseCurrencyCode,
                    toCurrency = targetCurrency,
                    convertUsingRates = convertUsingRates
                )
            )
        }

        val totalInflow = convertedEntries
            .filter { it.type == FlowType.INFLOW }
            .sumOf { it.amount }
        val totalOutflow = convertedEntries
            .filter { it.type == FlowType.OUTFLOW }
            .sumOf { it.amount }
        val netCashFlow = totalInflow - totalOutflow
        val operatingMarginPct = if (totalInflow > 0.0) {
            ((totalInflow - totalOutflow) / totalInflow) * 100.0
        } else {
            0.0
        }
        val savingsRatioPct = if (totalInflow > 0.0) {
            ((netCashFlow / totalInflow) * 100.0).coerceIn(-200.0, 100.0)
        } else {
            0.0
        }

        val groupedByPeriod = convertedEntries.groupBy { it.periodLabel }
        var runningBalance = 0.0
        val sortedPeriods = groupedByPeriod.entries
            .sortedBy { (_, items) -> items.minOfOrNull { it.dateMillis } ?: 0L }
            .map { (period, items) ->
                val inflow = items.filter { it.type == FlowType.INFLOW }.sumOf { it.amount }
                val outflow = items.filter { it.type == FlowType.OUTFLOW }.sumOf { it.amount }
                val net = inflow - outflow
                runningBalance += net
                val categoryOutflows = items
                    .filter { it.type == FlowType.OUTFLOW }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                val categoryInflows = items
                    .filter { it.type == FlowType.INFLOW }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                PeriodSummary(
                    periodLabel = period,
                    sortKey = items.minOfOrNull { it.dateMillis } ?: 0L,
                    inflow = inflow,
                    outflow = outflow,
                    net = net,
                    cumulativeBalance = runningBalance,
                    categoryOutflows = categoryOutflows,
                    categoryInflows = categoryInflows
                )
            }

        val avgMonthlyBurn = if (sortedPeriods.isNotEmpty()) {
            totalOutflow / sortedPeriods.size
        } else {
            0.0
        }

        val outflowSlices = buildSlices(convertedEntries, FlowType.OUTFLOW, totalOutflow)
        val inflowSlices = buildSlices(convertedEntries, FlowType.INFLOW, totalInflow)

        return AnalyticsSummary(
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            netCashFlow = netCashFlow,
            operatingMarginPct = operatingMarginPct,
            avgMonthlyBurn = avgMonthlyBurn,
            savingsRatioPct = savingsRatioPct,
            periods = sortedPeriods,
            outflowCategories = outflowSlices,
            inflowCategories = inflowSlices,
            topOutflowCategoryNames = outflowSlices.take(5).map { it.category }
        )
    }

    private fun buildSlices(
        entries: List<CashFlowEntry>,
        type: FlowType,
        total: Double
    ): List<CategorySlice> {
        if (total <= 0.0) return emptyList()
        return entries
            .filter { it.type == type }
            .groupBy { it.category }
            .map { (category, list) ->
                val sum = list.sumOf { it.amount }
                CategorySlice(
                    category = category,
                    amount = sum,
                    percentage = ((sum / total) * 100.0).toFloat(),
                    type = type
                )
            }
            .sortedByDescending { it.amount }
    }
}
