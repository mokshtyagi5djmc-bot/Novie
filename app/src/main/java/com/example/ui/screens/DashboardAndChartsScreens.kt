package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.excel.ExcelSampleTemplate
import com.example.data.excel.ExcelTemplateLibrary
import com.example.model.AnalyticsSummary
import com.example.model.AppCurrency
import com.example.model.AppLanguage
import com.example.model.CashFlowReport
import com.example.ui.charts.CashFlowDonutChart
import com.example.ui.charts.CashFlowGroupedBarChart
import com.example.ui.charts.CashFlowStackedBarChart
import com.example.ui.charts.CashFlowTrendLineChart
import com.example.ui.components.KpiSummaryGrid
import com.example.ui.theme.NovieBorder
import com.example.ui.theme.NovieCyan
import com.example.ui.theme.NovieCyanContainer
import com.example.ui.theme.NovieMint
import com.example.ui.theme.NovieMintContainer
import com.example.ui.theme.NovieOnMint
import com.example.ui.theme.NovieRose
import com.example.ui.theme.NovieRoseContainer
import com.example.ui.theme.NovieSurface
import com.example.ui.theme.NovieSurfaceElevated
import com.example.ui.theme.NovieSurfaceHighest
import com.example.ui.theme.NovieTextPrimary
import com.example.ui.theme.NovieTextSecondary
import com.example.ui.viewmodel.AiInsightUiState

@Composable
fun DashboardScreen(
    reports: List<CashFlowReport>,
    activeReport: CashFlowReport?,
    summary: AnalyticsSummary?,
    currency: AppCurrency,
    language: AppLanguage,
    isParsingFile: Boolean,
    aiInsightState: AiInsightUiState,
    onSelectReport: (String) -> Unit,
    onPickExcelFile: () -> Unit,
    onLoadTemplate: (ExcelSampleTemplate) -> Unit,
    onExportTemplate: (ExcelSampleTemplate) -> Unit,
    onAnalyzeWithAi: () -> Unit,
    onOpenChartsLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = language.toLocale()
    var selectedChartTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Action Row + Report Switcher
        item {
            ImportActionBanner(
                isParsingFile = isParsingFile,
                onPickExcelFile = onPickExcelFile
            )
        }

        if (reports.isNotEmpty() && activeReport != null && summary != null) {
            item {
                ReportSwitcherRow(
                    reports = reports,
                    activeReportId = activeReport.id,
                    onSelectReport = onSelectReport
                )
            }

            item {
                KpiSummaryGrid(
                    summary = summary,
                    currency = currency,
                    locale = locale
                )
            }

            // Interactive Chart Suite Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(NovieSurface)
                        .border(1.dp, NovieBorder, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeReport.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = NovieTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.chart_tap_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = NovieTextSecondary
                            )
                        }
                        TextButton(onClick = onOpenChartsLab) {
                            Text(stringResource(R.string.nav_charts), color = NovieCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ChartTypeSelectorRow(
                        selectedTab = selectedChartTab,
                        onSelectTab = { selectedChartTab = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedChartTab) {
                        0 -> CashFlowGroupedBarChart(
                            periods = summary.periods,
                            currency = currency,
                            locale = locale
                        )
                        1 -> CashFlowDonutChart(
                            outflowSlices = summary.outflowCategories,
                            inflowSlices = summary.inflowCategories,
                            currency = currency,
                            locale = locale
                        )
                        2 -> CashFlowTrendLineChart(
                            periods = summary.periods,
                            currency = currency,
                            locale = locale
                        )
                        else -> CashFlowStackedBarChart(
                            periods = summary.periods,
                            topCategories = summary.topOutflowCategoryNames,
                            currency = currency,
                            locale = locale
                        )
                    }
                }
            }

            // AI Financial Insights Card
            item {
                AiFinancialInsightsCard(
                    reportId = activeReport.id,
                    aiState = aiInsightState,
                    onAnalyze = onAnalyzeWithAi
                )
            }
        } else {
            item {
                EmptyDashboardCard()
            }
        }

        // Built-In Sample Templates (Always accessible for instant testing or .xlsx export)
        item {
            SampleTemplatesSection(
                onLoadTemplate = onLoadTemplate,
                onExportTemplate = onExportTemplate
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ChartsLabScreen(
    reports: List<CashFlowReport>,
    activeReport: CashFlowReport?,
    summary: AnalyticsSummary?,
    currency: AppCurrency,
    language: AppLanguage,
    isParsingFile: Boolean,
    onSelectReport: (String) -> Unit,
    onPickExcelFile: () -> Unit,
    onLoadTemplate: (ExcelSampleTemplate) -> Unit,
    onExportTemplate: (ExcelSampleTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = language.toLocale()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (reports.isNotEmpty() && activeReport != null && summary != null) {
            item {
                ReportSwitcherRow(
                    reports = reports,
                    activeReportId = activeReport.id,
                    onSelectReport = onSelectReport
                )
            }

            item {
                ChartShowcaseCard(
                    title = stringResource(R.string.chart_bar_grouped),
                    icon = Icons.Filled.BarChart,
                    accent = NovieMint
                ) {
                    CashFlowGroupedBarChart(
                        periods = summary.periods,
                        currency = currency,
                        locale = locale
                    )
                }
            }

            item {
                ChartShowcaseCard(
                    title = stringResource(R.string.chart_pie_donut),
                    icon = Icons.Filled.PieChart,
                    accent = NovieCyan
                ) {
                    CashFlowDonutChart(
                        outflowSlices = summary.outflowCategories,
                        inflowSlices = summary.inflowCategories,
                        currency = currency,
                        locale = locale
                    )
                }
            }

            item {
                ChartShowcaseCard(
                    title = stringResource(R.string.chart_line_trend),
                    icon = Icons.Filled.ShowChart,
                    accent = NovieCyan
                ) {
                    CashFlowTrendLineChart(
                        periods = summary.periods,
                        currency = currency,
                        locale = locale
                    )
                }
            }

            item {
                ChartShowcaseCard(
                    title = stringResource(R.string.chart_bar_stacked),
                    icon = Icons.Filled.StackedBarChart,
                    accent = NovieMint
                ) {
                    CashFlowStackedBarChart(
                        periods = summary.periods,
                        topCategories = summary.topOutflowCategoryNames,
                        currency = currency,
                        locale = locale
                    )
                }
            }
        } else {
            item {
                EmptyDashboardCard()
            }
            item {
                ImportActionBanner(isParsingFile = isParsingFile, onPickExcelFile = onPickExcelFile)
            }
            item {
                SampleTemplatesSection(onLoadTemplate = onLoadTemplate, onExportTemplate = onExportTemplate)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ChartShowcaseCard(
    title: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NovieSurface)
            .border(1.dp, NovieBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = NovieTextPrimary)
        }
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun ImportActionBanner(
    isParsingFile: Boolean,
    onPickExcelFile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(NovieMintContainer, NovieSurfaceElevated)
                )
            )
            .border(1.dp, NovieMint.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(enabled = !isParsingFile, onClick = onPickExcelFile)
            .testTag("import_excel_button")
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NovieMint),
                contentAlignment = Alignment.Center
            ) {
                if (isParsingFile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = NovieOnMint,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = stringResource(R.string.import_excel),
                        tint = NovieOnMint
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.import_excel),
                    style = MaterialTheme.typography.titleMedium,
                    color = NovieTextPrimary
                )
                Text(
                    text = stringResource(
                        if (isParsingFile) R.string.parsing_file_status else R.string.import_excel_sub
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = NovieTextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovieSurfaceHighest)
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(".XLSX", style = MaterialTheme.typography.labelSmall, color = NovieMint)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NovieSurfaceHighest)
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(".CSV", style = MaterialTheme.typography.labelSmall, color = NovieCyan)
            }
        }
    }
}

@Composable
private fun ReportSwitcherRow(
    reports: List<CashFlowReport>,
    activeReportId: String,
    onSelectReport: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        reports.forEach { report ->
            val selected = report.id == activeReportId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) NovieMintContainer else NovieSurfaceElevated)
                    .border(
                        width = 1.dp,
                        color = if (selected) NovieMint else NovieBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectReport(report.id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) NovieMint else NovieTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ChartTypeSelectorRow(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val tabs = listOf(
        Triple(0, Icons.Filled.BarChart, stringResource(R.string.chart_bar_grouped)),
        Triple(1, Icons.Filled.PieChart, stringResource(R.string.chart_pie_donut)),
        Triple(2, Icons.Filled.ShowChart, stringResource(R.string.chart_line_trend)),
        Triple(3, Icons.Filled.StackedBarChart, stringResource(R.string.chart_bar_stacked))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (idx, icon, label) ->
            val isSelected = selectedTab == idx
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) NovieMint else NovieSurfaceElevated)
                    .border(
                        1.dp,
                        if (isSelected) NovieMint else NovieBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectTab(idx) }
                    .testTag("chart_tab_$idx")
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) NovieOnMint else NovieTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) NovieOnMint else NovieTextSecondary
                )
            }
        }
    }
}

@Composable
private fun AiFinancialInsightsCard(
    reportId: String,
    aiState: AiInsightUiState,
    onAnalyze: () -> Unit
) {
    val activeInsight = (aiState as? AiInsightUiState.Success)?.takeIf { it.reportId == reportId }?.insight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NovieSurface)
            .border(1.dp, NovieCyan.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NovieCyanContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = NovieCyan, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(stringResource(R.string.ai_insights_title), style = MaterialTheme.typography.titleMedium, color = NovieTextPrimary)
                    Text(stringResource(R.string.ai_insights_subtitle), style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
                }
            }

            Button(
                onClick = onAnalyze,
                enabled = aiState !is AiInsightUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = NovieCyan, contentColor = NovieOnMint),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("ai_analyze_button")
            ) {
                if (aiState is AiInsightUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NovieOnMint, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = if (activeInsight == null) Icons.Filled.AutoAwesome else Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(if (activeInsight == null) R.string.ai_analyze_btn else R.string.ai_reanalyze_btn),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        AnimatedVisibility(visible = aiState is AiInsightUiState.Loading) {
            Text(
                text = stringResource(R.string.ai_analyzing),
                style = MaterialTheme.typography.bodyMedium,
                color = NovieCyan,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (aiState is AiInsightUiState.Error) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovieRoseContainer)
                    .padding(12.dp)
            ) {
                Text(aiState.message, style = MaterialTheme.typography.bodySmall, color = NovieRose)
                TextButton(onClick = onAnalyze, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.retry), color = NovieMint)
                }
            }
        }

        if (activeInsight != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NovieMintContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(activeInsight.healthBadge, style = MaterialTheme.typography.labelSmall, color = NovieMint)
                }
                Text(
                    text = activeInsight.headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = NovieTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(activeInsight.runwayObservation, style = MaterialTheme.typography.bodyMedium, color = NovieTextSecondary)

            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.ai_findings_header), style = MaterialTheme.typography.labelLarge, color = NovieCyan)
            Spacer(modifier = Modifier.height(4.dp))
            activeInsight.keyFindings.forEach { finding ->
                Text("• $finding", style = MaterialTheme.typography.bodySmall, color = NovieTextPrimary, modifier = Modifier.padding(vertical = 2.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(stringResource(R.string.ai_recommendations_header), style = MaterialTheme.typography.labelLarge, color = NovieMint)
            Spacer(modifier = Modifier.height(4.dp))
            activeInsight.recommendations.forEach { rec ->
                Text("→ $rec", style = MaterialTheme.typography.bodySmall, color = NovieTextPrimary, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun EmptyDashboardCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NovieSurface)
            .border(1.dp, NovieBorder, RoundedCornerShape(22.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(NovieSurfaceHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.BarChart, contentDescription = null, tint = NovieMint, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.empty_dashboard_title),
            style = MaterialTheme.typography.titleLarge,
            color = NovieTextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_dashboard_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = NovieTextSecondary
        )
    }
}

@Composable
fun SampleTemplatesSection(
    onLoadTemplate: (ExcelSampleTemplate) -> Unit,
    onExportTemplate: (ExcelSampleTemplate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text(
                text = stringResource(R.string.sample_templates_title),
                style = MaterialTheme.typography.titleLarge,
                color = NovieTextPrimary
            )
            Text(
                text = stringResource(R.string.sample_templates_sub),
                style = MaterialTheme.typography.bodySmall,
                color = NovieTextSecondary
            )
        }

        ExcelTemplateLibrary.templates.forEach { template ->
            val (titleRes, subRes) = when (template.titleKey) {
                "saas" -> R.string.template_saas_title to R.string.template_saas_sub
                "retail" -> R.string.template_retail_title to R.string.template_retail_sub
                else -> R.string.template_studio_title to R.string.template_studio_sub
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(NovieSurface)
                    .border(1.dp, NovieBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, color = NovieTextPrimary)
                        Text(stringResource(subRes), style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NovieSurfaceHighest)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(template.currencyCode, style = MaterialTheme.typography.labelSmall, color = NovieCyan)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onLoadTemplate(template) },
                        colors = ButtonDefaults.buttonColors(containerColor = NovieMint, contentColor = NovieOnMint),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_template_${template.id}")
                    ) {
                        Text(stringResource(R.string.load_template), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onExportTemplate(template) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("export_template_${template.id}")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = NovieCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.export_xlsx), color = NovieCyan)
                    }
                }
            }
        }
    }
}
