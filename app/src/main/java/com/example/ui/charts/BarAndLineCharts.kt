package com.example.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppCurrency
import com.example.model.CurrencyFormatter
import com.example.model.PeriodSummary
import com.example.ui.theme.ChartSegmentColors
import com.example.ui.theme.NovieBorder
import com.example.ui.theme.NovieCyan
import com.example.ui.theme.NovieMint
import com.example.ui.theme.NovieRose
import com.example.ui.theme.NovieSurfaceElevated
import com.example.ui.theme.NovieSurfaceHighest
import com.example.ui.theme.NovieTextMuted
import com.example.ui.theme.NovieTextSecondary
import com.example.ui.theme.TabularNumberStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun CashFlowGroupedBarChart(
    periods: List<PeriodSummary>,
    currency: AppCurrency,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    if (periods.isEmpty()) return

    var selectedIdx by remember(periods) { mutableIntStateOf(periods.lastIndex) }
    val progress = remember(periods) { Animatable(0f) }
    LaunchedEffect(periods) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    val maxVal = remember(periods) {
        periods.maxOf { max(it.inflow, it.outflow) }.coerceAtLeast(1.0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend + Selected Period Inspection Pill
        val activePeriod = periods.getOrNull(selectedIdx)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(color = NovieMint, label = stringResource(R.string.chart_inflow))
                LegendDot(color = NovieRose, label = stringResource(R.string.chart_outflow))
            }
            if (activePeriod != null) {
                Text(
                    text = activePeriod.periodLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = NovieMint,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (activePeriod != null) {
            Spacer(modifier = Modifier.height(10.dp))
            PeriodInspectionStrip(period = activePeriod, currency = currency, locale = locale)
        }

        Spacer(modifier = Modifier.height(14.dp))

        val itemWidthDp = 64.dp
        val scrollState = rememberScrollState()
        val chartHeight = 200.dp

        Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            // Fixed Y-Axis column
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .height(chartHeight - 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf(1.0, 0.5, 0.0).forEach { ratio ->
                    Text(
                        text = CurrencyFormatter.formatCompact(maxVal * ratio, currency, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = NovieTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Scrollable Canvas + X Labels
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                val totalWidth = max(280f, periods.size * itemWidthDp.value).dp
                Column(modifier = Modifier.width(totalWidth)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight - 24.dp)
                            .testTag("grouped_bar_chart_canvas")
                            .pointerInput(periods) {
                                detectTapGestures { offset ->
                                    val slotWidth = size.width / periods.size.toFloat()
                                    val tapped = (offset.x / slotWidth).toInt().coerceIn(0, periods.lastIndex)
                                    selectedIdx = tapped
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        listOf(0f, h * 0.5f, h).forEach { y ->
                            drawLine(
                                color = NovieBorder.copy(alpha = 0.65f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (y < h) dash else null
                            )
                        }

                        val slotWidth = w / periods.size.toFloat()
                        val barWidth = (slotWidth * 0.26f).coerceIn(10.dp.toPx(), 24.dp.toPx())
                        val gap = 4.dp.toPx()

                        periods.forEachIndexed { idx, p ->
                            val cx = slotWidth * idx + slotWidth / 2f
                            if (idx == selectedIdx) {
                                drawRoundRect(
                                    color = NovieSurfaceHighest.copy(alpha = 0.7f),
                                    topLeft = Offset(slotWidth * idx + 4.dp.toPx(), 0f),
                                    size = Size(slotWidth - 8.dp.toPx(), h),
                                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                )
                            }

                            val inflowH = ((p.inflow / maxVal).toFloat() * h * progress.value).coerceAtLeast(2f)
                            val outflowH = ((p.outflow / maxVal).toFloat() * h * progress.value).coerceAtLeast(2f)

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(NovieMint, NovieMint.copy(alpha = 0.65f))
                                ),
                                topLeft = Offset(cx - barWidth - gap / 2f, h - inflowH),
                                size = Size(barWidth, inflowH),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(NovieRose, NovieRose.copy(alpha = 0.65f))
                                ),
                                topLeft = Offset(cx + gap / 2f, h - outflowH),
                                size = Size(barWidth, outflowH),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        periods.forEachIndexed { idx, p ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedIdx = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.periodLabel.takeLast(5),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (idx == selectedIdx) NovieMint else NovieTextMuted,
                                    fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashFlowStackedBarChart(
    periods: List<PeriodSummary>,
    topCategories: List<String>,
    currency: AppCurrency,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    if (periods.isEmpty()) return

    var selectedIdx by remember(periods) { mutableIntStateOf(periods.lastIndex) }
    val progress = remember(periods) { Animatable(0f) }
    LaunchedEffect(periods) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    val categoriesToRender = remember(topCategories, periods) {
        topCategories.ifEmpty {
            periods.flatMap { it.categoryOutflows.keys }.distinct().take(5)
        }
    }
    val maxOutflow = remember(periods) {
        periods.maxOf { it.outflow }.coerceAtLeast(1.0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categoriesToRender.forEachIndexed { idx, cat ->
                LegendDot(
                    color = ChartSegmentColors[idx % ChartSegmentColors.size],
                    label = cat
                )
            }
        }

        val activePeriod = periods.getOrNull(selectedIdx)
        if (activePeriod != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovieSurfaceElevated)
                    .border(1.dp, NovieBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activePeriod.periodLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = NovieCyan
                )
                Text(
                    text = "${stringResource(R.string.chart_outflow)}: ${CurrencyFormatter.formatFull(activePeriod.outflow, currency, locale)}",
                    style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                    color = NovieRose
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val scrollState = rememberScrollState()
        val chartHeight = 196.dp
        Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .height(chartHeight - 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                listOf(1.0, 0.5, 0.0).forEach { ratio ->
                    Text(
                        text = CurrencyFormatter.formatCompact(maxOutflow * ratio, currency, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = NovieTextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
                val totalWidth = max(280f, periods.size * 58f).dp
                Column(modifier = Modifier.width(totalWidth)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight - 24.dp)
                            .testTag("stacked_bar_chart_canvas")
                            .pointerInput(periods) {
                                detectTapGestures { offset ->
                                    val slotWidth = size.width / periods.size.toFloat()
                                    selectedIdx = (offset.x / slotWidth).toInt().coerceIn(0, periods.lastIndex)
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val slotWidth = w / periods.size.toFloat()
                        val barWidth = (slotWidth * 0.45f).coerceIn(14.dp.toPx(), 32.dp.toPx())

                        drawLine(
                            color = NovieBorder,
                            start = Offset(0f, h),
                            end = Offset(w, h),
                            strokeWidth = 1.dp.toPx()
                        )

                        periods.forEachIndexed { idx, p ->
                            val x = slotWidth * idx + (slotWidth - barWidth) / 2f
                            if (idx == selectedIdx) {
                                drawRoundRect(
                                    color = NovieSurfaceHighest.copy(alpha = 0.7f),
                                    topLeft = Offset(slotWidth * idx + 4.dp.toPx(), 0f),
                                    size = Size(slotWidth - 8.dp.toPx(), h),
                                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                )
                            }
                            var currentBottom = h
                            categoriesToRender.forEachIndexed { catIdx, catName ->
                                val amount = p.categoryOutflows[catName] ?: 0.0
                                if (amount > 0.0) {
                                    val segHeight = ((amount / maxOutflow).toFloat() * h * progress.value).coerceAtLeast(2f)
                                    drawRoundRect(
                                        color = ChartSegmentColors[catIdx % ChartSegmentColors.size],
                                        topLeft = Offset(x, currentBottom - segHeight),
                                        size = Size(barWidth, segHeight - 1.dp.toPx()),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                    currentBottom -= segHeight
                                }
                            }
                            val otherSum = p.categoryOutflows
                                .filterKeys { it !in categoriesToRender }
                                .values
                                .sum()
                            if (otherSum > 0.0) {
                                val segHeight = ((otherSum / maxOutflow).toFloat() * h * progress.value).coerceAtLeast(2f)
                                drawRoundRect(
                                    color = NovieTextMuted,
                                    topLeft = Offset(x, currentBottom - segHeight),
                                    size = Size(barWidth, segHeight),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        periods.forEachIndexed { idx, p ->
                            Box(
                                modifier = Modifier.weight(1f).clickable { selectedIdx = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.periodLabel.takeLast(5),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (idx == selectedIdx) NovieCyan else NovieTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowTrendLineChart(
    periods: List<PeriodSummary>,
    currency: AppCurrency,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    if (periods.isEmpty()) return

    var selectedIdx by remember(periods) { mutableIntStateOf(periods.lastIndex) }
    val progress = remember(periods) { Animatable(0f) }
    LaunchedEffect(periods) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val minY = remember(periods) {
        min(0.0, periods.minOf { min(it.cumulativeBalance, it.net) })
    }
    val maxY = remember(periods) {
        max(1.0, periods.maxOf { max(it.cumulativeBalance, it.net) })
    }
    val span = (maxY - minY).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(color = NovieCyan, label = stringResource(R.string.chart_cumulative))
                LegendDot(color = NovieMint, label = stringResource(R.string.chart_net))
            }
        }

        val active = periods.getOrNull(selectedIdx)
        if (active != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovieSurfaceElevated)
                    .border(1.dp, NovieBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(active.periodLabel, style = MaterialTheme.typography.labelMedium, color = NovieTextSecondary)
                    Text(
                        text = "${stringResource(R.string.chart_net)}: ${CurrencyFormatter.formatFull(active.net, currency, locale)}",
                        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                        color = if (active.net >= 0) NovieMint else NovieRose
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.chart_cumulative), style = MaterialTheme.typography.labelMedium, color = NovieTextSecondary)
                    Text(
                        text = CurrencyFormatter.formatFull(active.cumulativeBalance, currency, locale),
                        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                        color = NovieCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val chartHeight = 200.dp
        Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .height(chartHeight - 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(CurrencyFormatter.formatCompact(maxY, currency, locale), style = MaterialTheme.typography.labelSmall, color = NovieTextMuted)
                Text(CurrencyFormatter.formatCompact((maxY + minY) / 2.0, currency, locale), style = MaterialTheme.typography.labelSmall, color = NovieTextMuted)
                Text(CurrencyFormatter.formatCompact(minY, currency, locale), style = MaterialTheme.typography.labelSmall, color = NovieTextMuted)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight - 24.dp)
                        .testTag("trend_line_chart_canvas")
                        .pointerInput(periods) {
                            detectTapGestures { offset ->
                                val stepX = if (periods.size > 1) size.width / (periods.size - 1).toFloat() else size.width.toFloat()
                                val tapped = if (periods.size == 1) 0 else ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, periods.lastIndex)
                                selectedIdx = tapped
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    fun yFor(value: Double): Float = h - (((value - minY) / span).toFloat() * h)

                    // Zero baseline
                    val zeroY = yFor(0.0).coerceIn(0f, h)
                    drawLine(
                        color = NovieBorder,
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )

                    val stepX = if (periods.size > 1) w / (periods.size - 1).toFloat() else w / 2f
                    val cumPoints = periods.mapIndexed { idx, p ->
                        val x = if (periods.size == 1) w / 2f else idx * stepX
                        val y = yFor(p.cumulativeBalance)
                        Offset(x, y)
                    }
                    val netPoints = periods.mapIndexed { idx, p ->
                        val x = if (periods.size == 1) w / 2f else idx * stepX
                        val y = yFor(p.net)
                        Offset(x, y)
                    }

                    if (cumPoints.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(cumPoints.first().x, cumPoints.first().y)
                            for (i in 1 until cumPoints.size) {
                                val prev = cumPoints[i - 1]
                                val curr = cumPoints[i]
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(cumPoints.last().x, h)
                            lineTo(cumPoints.first().x, h)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(NovieCyan.copy(alpha = 0.28f * progress.value), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = NovieCyan.copy(alpha = progress.value),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        val netPath = Path().apply {
                            moveTo(netPoints.first().x, netPoints.first().y)
                            for (i in 1 until netPoints.size) {
                                lineTo(netPoints[i].x, netPoints[i].y)
                            }
                        }
                        drawPath(
                            path = netPath,
                            color = NovieMint.copy(alpha = 0.85f * progress.value),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        )
                    }

                    // Selected vertical guide & points
                    val selX = cumPoints[selectedIdx].x
                    drawLine(
                        color = NovieCyan.copy(alpha = 0.35f),
                        start = Offset(selX, 0f),
                        end = Offset(selX, h),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    cumPoints.forEachIndexed { idx, pt ->
                        val isSel = idx == selectedIdx
                        drawCircle(
                            color = if (isSel) NovieCyan else NovieSurfaceElevated,
                            radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = NovieCyan,
                            radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    netPoints.forEachIndexed { idx, pt ->
                        val dotColor = if (periods[idx].net >= 0) NovieMint else NovieRose
                        drawCircle(
                            color = dotColor,
                            radius = if (idx == selectedIdx) 5.dp.toPx() else 3.dp.toPx(),
                            center = pt
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val step = (periods.size / 4).coerceAtLeast(1)
                    periods.forEachIndexed { idx, p ->
                        if (idx == 0 || idx == periods.lastIndex || idx % step == 0) {
                            Text(
                                text = p.periodLabel.takeLast(5),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (idx == selectedIdx) NovieCyan else NovieTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodInspectionStrip(
    period: PeriodSummary,
    currency: AppCurrency,
    locale: Locale
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NovieSurfaceElevated)
            .border(1.dp, NovieBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactStat(label = stringResource(R.string.chart_inflow), value = CurrencyFormatter.formatCompact(period.inflow, currency, locale), color = NovieMint)
        CompactStat(label = stringResource(R.string.chart_outflow), value = CurrencyFormatter.formatCompact(period.outflow, currency, locale), color = NovieRose)
        CompactStat(
            label = stringResource(R.string.chart_net),
            value = CurrencyFormatter.formatCompact(period.net, currency, locale),
            color = if (period.net >= 0) NovieCyan else NovieRose
        )
    }
}

@Composable
private fun CompactStat(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = NovieTextSecondary)
        Text(
            text = value,
            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
            color = color
        )
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = NovieTextSecondary)
    }
}
