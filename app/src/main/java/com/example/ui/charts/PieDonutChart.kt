package com.example.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppCurrency
import com.example.model.CategorySlice
import com.example.model.CurrencyFormatter
import com.example.ui.theme.ChartSegmentColors
import com.example.ui.theme.NovieBorder
import com.example.ui.theme.NovieMint
import com.example.ui.theme.NovieMintContainer
import com.example.ui.theme.NovieRose
import com.example.ui.theme.NovieRoseContainer
import com.example.ui.theme.NovieSurfaceElevated
import com.example.ui.theme.NovieSurfaceHighest
import com.example.ui.theme.NovieTextPrimary
import com.example.ui.theme.NovieTextSecondary
import com.example.ui.theme.TabularNumberStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun CashFlowDonutChart(
    outflowSlices: List<CategorySlice>,
    inflowSlices: List<CategorySlice>,
    currency: AppCurrency,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    var showOutflows by remember { mutableStateOf(true) }
    val activeSlices = if (showOutflows) outflowSlices else inflowSlices
    var selectedIdx by remember(showOutflows, activeSlices) { mutableIntStateOf(0) }

    val sweepAnim = remember(showOutflows, activeSlices) { Animatable(0f) }
    LaunchedEffect(showOutflows, activeSlices) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    val totalAmount = remember(activeSlices) { activeSlices.sumOf { it.amount } }
    val selectedSlice = activeSlices.getOrNull(selectedIdx)

    Column(modifier = modifier.fillMaxWidth()) {
        // Outflow vs Inflow Toggle Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NovieSurfaceElevated)
                .border(1.dp, NovieBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (showOutflows) NovieRoseContainer else NovieSurfaceElevated)
                    .clickable { showOutflows = true }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.chart_toggle_outflow),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (showOutflows) NovieRose else NovieTextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!showOutflows) NovieMintContainer else NovieSurfaceElevated)
                    .clickable { showOutflows = false }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.chart_toggle_inflow),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (!showOutflows) NovieMint else NovieTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSlices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_dashboard_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovieTextSecondary
                )
            }
            return
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(196.dp)
                    .testTag("donut_chart_canvas")
                    .pointerInput(activeSlices) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy)
                            if (dist > size.width * 0.18f && totalAmount > 0.0) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f
                                var accum = 0f
                                activeSlices.forEachIndexed { idx, slice ->
                                    val sweep = (slice.amount / totalAmount).toFloat() * 360f
                                    if (angle >= accum && angle <= accum + sweep) {
                                        selectedIdx = idx
                                        return@detectTapGestures
                                    }
                                    accum += sweep
                                }
                            }
                        }
                    }
            ) {
                val baseStroke = 28.dp.toPx()
                val gapDegrees = if (activeSlices.size > 1) 2.5f else 0f
                var startAngle = -90f

                activeSlices.forEachIndexed { idx, slice ->
                    val rawSweep = (slice.amount / totalAmount).toFloat() * 360f * sweepAnim.value
                    val drawSweep = (rawSweep - gapDegrees).coerceAtLeast(1f)
                    val isSelected = idx == selectedIdx
                    val color = ChartSegmentColors[idx % ChartSegmentColors.size]
                    val inset = if (isSelected) 4.dp.toPx() else 12.dp.toPx()
                    val strokeW = if (isSelected) baseStroke + 6.dp.toPx() else baseStroke

                    drawArc(
                        color = if (isSelected) color else color.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2f, size.height - inset * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += rawSweep
                }
            }

            Column(
                modifier = Modifier.width(116.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectedSlice?.category ?: stringResource(R.string.chart_center_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = NovieTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = CurrencyFormatter.formatCompact(selectedSlice?.amount ?: totalAmount, currency, locale),
                    style = TabularNumberStyle.copy(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = NovieTextPrimary,
                    maxLines = 1
                )
                if (selectedSlice != null) {
                    Text(
                        text = String.format(locale, "%.1f%%", selectedSlice.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChartSegmentColors[selectedIdx % ChartSegmentColors.size]
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activeSlices.forEachIndexed { idx, slice ->
                val isSelected = idx == selectedIdx
                val sliceColor = ChartSegmentColors[idx % ChartSegmentColors.size]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NovieSurfaceHighest else NovieSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) sliceColor.copy(alpha = 0.75f) else NovieBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedIdx = idx }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(sliceColor))
                        Text(
                            text = slice.category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovieTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = CurrencyFormatter.formatFull(slice.amount, currency, locale),
                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                            color = NovieTextPrimary
                        )
                        Text(
                            text = String.format(locale, "%.1f%%", slice.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = sliceColor
                        )
                    }
                }
            }
        }
    }
}
