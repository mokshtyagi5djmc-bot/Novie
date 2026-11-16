package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.launchInteractiveGoogleSignIn
import com.example.model.AnalyticsSummary
import com.example.model.AppCurrency
import com.example.model.AppLanguage
import com.example.model.CurrencyFormatter
import com.example.model.StorageMode
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
import com.example.ui.theme.TabularNumberStyle
import java.util.Locale

@Composable
fun NovieTopHeader(
    currency: AppCurrency,
    language: AppLanguage,
    storageMode: StorageMode,
    isSignedIn: Boolean,
    onCurrencyClick: () -> Unit,
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NovieMint, NovieCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ShowChart,
                    contentDescription = stringResource(R.string.app_name),
                    tint = NovieOnMint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = NovieTextPrimary
                    )
                    StorageBadge(storageMode = storageMode, isSignedIn = isSignedIn)
                }
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.labelMedium,
                    color = NovieTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderPillButton(
                icon = Icons.Filled.CurrencyExchange,
                label = "${currency.code} ${currency.symbol}",
                onClick = onCurrencyClick,
                tag = "header_currency_pill"
            )
            HeaderPillButton(
                icon = Icons.Filled.Language,
                label = language.shortCode,
                onClick = onLanguageClick,
                tag = "header_language_pill"
            )
        }
    }
}

@Composable
private fun StorageBadge(storageMode: StorageMode, isSignedIn: Boolean) {
    val isCloud = storageMode == StorageMode.GOOGLE_SYNC && isSignedIn
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isCloud) NovieCyanContainer else NovieSurfaceHighest)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (isCloud) Icons.Filled.CloudDone else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (isCloud) NovieCyan else NovieMint,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = stringResource(if (isCloud) R.string.cloud_badge else R.string.private_badge),
            style = MaterialTheme.typography.labelSmall,
            color = if (isCloud) NovieCyan else NovieMint
        )
    }
}

@Composable
private fun HeaderPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(NovieSurfaceElevated)
            .border(1.dp, NovieBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NovieMint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = NovieTextPrimary
        )
    }
}

@Composable
fun KpiSummaryGrid(
    summary: AnalyticsSummary,
    currency: AppCurrency,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiMetricCard(
                title = stringResource(R.string.kpi_inflow),
                value = CurrencyFormatter.formatCompact(summary.totalInflow, currency, locale),
                subValue = CurrencyFormatter.formatFull(summary.totalInflow, currency, locale),
                accentColor = NovieMint,
                containerColor = NovieMintContainer.copy(alpha = 0.45f),
                icon = Icons.Filled.ArrowUpward,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = stringResource(R.string.kpi_outflow),
                value = CurrencyFormatter.formatCompact(summary.totalOutflow, currency, locale),
                subValue = CurrencyFormatter.formatFull(summary.totalOutflow, currency, locale),
                accentColor = NovieRose,
                containerColor = NovieRoseContainer.copy(alpha = 0.45f),
                icon = Icons.Filled.ArrowDownward,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val netPositive = summary.netCashFlow >= 0
            KpiMetricCard(
                title = stringResource(R.string.kpi_net),
                value = CurrencyFormatter.formatCompact(summary.netCashFlow, currency, locale),
                subValue = CurrencyFormatter.formatFull(summary.netCashFlow, currency, locale),
                accentColor = if (netPositive) NovieCyan else NovieRose,
                containerColor = if (netPositive) NovieCyanContainer.copy(alpha = 0.45f) else NovieRoseContainer.copy(alpha = 0.45f),
                icon = Icons.Filled.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = stringResource(R.string.kpi_margin),
                value = "${String.format(locale, "%.1f", summary.operatingMarginPct)}%",
                subValue = "${stringResource(R.string.kpi_burn)}: ${CurrencyFormatter.formatCompact(summary.avgMonthlyBurn, currency, locale)}",
                accentColor = NovieMint,
                containerColor = NovieSurfaceElevated,
                icon = Icons.Filled.ShowChart,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subValue: String,
    accentColor: Color,
    containerColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = NovieTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = TabularNumberStyle.copy(
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                fontWeight = FontWeight.Bold
            ),
            color = accentColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subValue,
            style = MaterialTheme.typography.labelSmall,
            color = NovieTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GoogleSignInActionButton(
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            launchInteractiveGoogleSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onSuccess = {
                    isLoading = false
                    onSuccess()
                },
                onCancelled = {
                    isLoading = false
                },
                onError = { msg ->
                    isLoading = false
                    onError(msg)
                }
            )
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = NovieMint,
            contentColor = NovieOnMint
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(50.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = NovieOnMint,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = stringResource(R.string.sign_in_google),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CurrencyPickerDialog(
    selected: AppCurrency,
    onSelect: (AppCurrency) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NovieSurface,
        title = {
            Text(
                text = stringResource(R.string.settings_currency),
                style = MaterialTheme.typography.titleLarge,
                color = NovieTextPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppCurrency.entries) { item ->
                    val isSelected = item == selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(item)
                                onDismiss()
                            },
                        color = if (isSelected) NovieMintContainer else NovieSurfaceElevated,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = item.symbol,
                                    style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                                    color = if (isSelected) NovieMint else NovieCyan
                                )
                                Column {
                                    Text(item.code, style = MaterialTheme.typography.titleSmall, color = NovieTextPrimary)
                                    Text(item.displayName, style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = NovieMint)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = NovieMint)
            }
        }
    )
}

@Composable
fun LanguagePickerDialog(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NovieSurface,
        title = {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
                color = NovieTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { lang ->
                    val isSelected = lang == selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(lang)
                                onDismiss()
                            },
                        color = if (isSelected) NovieMintContainer else NovieSurfaceElevated,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(lang.nativeLabel, style = MaterialTheme.typography.titleMedium, color = NovieTextPrimary)
                                Text(lang.englishName, style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
                            }
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = NovieMint)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = NovieMint)
            }
        }
    )
}
