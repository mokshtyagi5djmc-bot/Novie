package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.signOutGoogle
import com.example.data.local.UserPreferences
import com.example.model.AppCurrency
import com.example.model.AppLanguage
import com.example.model.CashFlowReport
import com.example.model.CurrencyFormatter
import com.example.model.FlowType
import com.example.model.StorageMode
import com.example.ui.components.GoogleSignInActionButton
import com.example.ui.theme.NovieBorder
import com.example.ui.theme.NovieCyan
import com.example.ui.theme.NovieCyanContainer
import com.example.ui.theme.NovieMint
import com.example.ui.theme.NovieMintContainer
import com.example.ui.theme.NovieOnMint
import com.example.ui.theme.NovieRose
import com.example.ui.theme.NovieSurface
import com.example.ui.theme.NovieSurfaceElevated
import com.example.ui.theme.NovieSurfaceHighest
import com.example.ui.theme.NovieTextPrimary
import com.example.ui.theme.NovieTextSecondary
import com.example.ui.theme.TabularNumberStyle
import com.google.firebase.auth.FirebaseUser

@Composable
fun ReportsScreen(
    reports: List<CashFlowReport>,
    activeReport: CashFlowReport?,
    currency: AppCurrency,
    language: AppLanguage,
    convertUsingRates: Boolean,
    isParsingFile: Boolean,
    onSelectReport: (String) -> Unit,
    onRenameReport: (String, String) -> Unit,
    onDeleteReport: (String) -> Unit,
    onPickExcelFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = language.toLocale()
    var renameTarget by remember { mutableStateOf<CashFlowReport?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reports_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = NovieTextPrimary
                )
                Button(
                    onClick = onPickExcelFile,
                    enabled = !isParsingFile,
                    colors = ButtonDefaults.buttonColors(containerColor = NovieMint, contentColor = NovieOnMint),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("reports_import_excel_button")
                ) {
                    if (isParsingFile) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NovieOnMint, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.import_excel), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (reports.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(NovieSurface)
                        .border(1.dp, NovieBorder, RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reports_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovieTextSecondary
                    )
                    OutlinedButton(
                        onClick = onPickExcelFile,
                        enabled = !isParsingFile,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, tint = NovieMint, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.import_excel), color = NovieMint)
                    }
                }
            }
        } else {
            items(reports, key = { it.id }) { report ->
                val isActive = report.id == activeReport?.id
                val netConverted = CurrencyFormatter.convertAmount(
                    amount = report.netCashFlow,
                    fromCode = report.baseCurrencyCode,
                    toCurrency = currency,
                    convertUsingRates = convertUsingRates
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isActive) NovieMintContainer.copy(alpha = 0.35f) else NovieSurface)
                        .border(
                            1.dp,
                            if (isActive) NovieMint else NovieBorder,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelectReport(report.id) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = NovieTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${report.sourceFileName} · ${stringResource(R.string.entries_count, report.entries.size)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovieTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.kpi_net)}: ${CurrencyFormatter.formatFull(netConverted, currency, locale)}",
                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                            color = if (netConverted >= 0) NovieMint else NovieRose
                        )
                    }
                    Row {
                        IconButton(onClick = { renameTarget = report }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename), tint = NovieCyan)
                        }
                        IconButton(onClick = { onDeleteReport(report.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.delete), tint = NovieRose)
                        }
                    }
                }
            }

            if (activeReport != null) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.ledger_preview),
                        style = MaterialTheme.typography.titleMedium,
                        color = NovieTextPrimary
                    )
                }
                items(activeReport.entries.take(25)) { entry ->
                    val converted = CurrencyFormatter.convertAmount(
                        amount = entry.amount,
                        fromCode = activeReport.baseCurrencyCode,
                        toCurrency = currency,
                        convertUsingRates = convertUsingRates
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NovieSurfaceElevated)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.category, style = MaterialTheme.typography.titleSmall, color = NovieTextPrimary)
                            Text(
                                text = listOf(entry.periodLabel, entry.note).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = NovieTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val sign = if (entry.type == FlowType.INFLOW) "+" else "-"
                        Text(
                            text = "$sign${CurrencyFormatter.formatFull(converted, currency, locale)}",
                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                            color = if (entry.type == FlowType.INFLOW) NovieMint else NovieRose
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    renameTarget?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = NovieSurface,
            title = { Text(stringResource(R.string.rename), color = NovieTextPrimary) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.report_title_label)) }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameReport(target.id, text)
                        renameTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovieMint, contentColor = NovieOnMint)
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.cancel), color = NovieTextSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    authUser: FirebaseUser?,
    isSyncing: Boolean,
    onSelectCurrency: (AppCurrency) -> Unit,
    onToggleConvertRates: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectStorageMode: (StorageMode) -> Unit,
    onSyncLocalToCloud: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = NovieTextPrimary
            )
        }

        // Storage & Google Sync Mode
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NovieSurface)
                    .border(1.dp, NovieBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_storage), style = MaterialTheme.typography.titleMedium, color = NovieTextPrimary)

                StorageOptionCard(
                    title = stringResource(R.string.mode_private_title),
                    desc = stringResource(R.string.mode_private_desc),
                    icon = Icons.Filled.Lock,
                    selected = preferences.storageMode == StorageMode.PRIVATE_LOCAL,
                    onClick = { onSelectStorageMode(StorageMode.PRIVATE_LOCAL) },
                    tag = "storage_mode_private"
                )

                StorageOptionCard(
                    title = stringResource(R.string.mode_cloud_title),
                    desc = stringResource(R.string.mode_cloud_desc),
                    icon = Icons.Filled.CloudDone,
                    selected = preferences.storageMode == StorageMode.GOOGLE_SYNC,
                    onClick = { onSelectStorageMode(StorageMode.GOOGLE_SYNC) },
                    tag = "storage_mode_cloud"
                )

                if (authUser == null) {
                    GoogleSignInActionButton(
                        onSuccess = { onSelectStorageMode(StorageMode.GOOGLE_SYNC) },
                        onError = onAuthError,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = stringResource(R.string.signed_in_as, authUser.email ?: authUser.displayName ?: authUser.uid),
                        style = MaterialTheme.typography.bodySmall,
                        color = NovieCyan
                    )
                    Button(
                        onClick = onSyncLocalToCloud,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = NovieCyanContainer, contentColor = NovieCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NovieCyan, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sync_local_to_cloud))
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            signOutGoogle(credentialManager, scope) {
                                onSelectStorageMode(StorageMode.PRIVATE_LOCAL)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = NovieRose)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_out), color = NovieRose)
                    }
                }
            }
        }

        // Currency Selector
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NovieSurface)
                    .border(1.dp, NovieBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_currency), style = MaterialTheme.typography.titleMedium, color = NovieTextPrimary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppCurrency.entries.forEach { curr ->
                        val selected = curr == preferences.currency
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) NovieMint else NovieSurfaceElevated)
                                .border(1.dp, if (selected) NovieMint else NovieBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelectCurrency(curr) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${curr.code} ${curr.symbol}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) NovieOnMint else NovieTextPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(stringResource(R.string.settings_convert_rates), style = MaterialTheme.typography.bodyMedium, color = NovieTextPrimary)
                        Text(stringResource(R.string.settings_convert_rates_desc), style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
                    }
                    Switch(
                        checked = preferences.convertCurrencyUsingRates,
                        onCheckedChange = onToggleConvertRates,
                        colors = SwitchDefaults.colors(checkedThumbColor = NovieOnMint, checkedTrackColor = NovieMint)
                    )
                }
            }
        }

        // Language Selector (EN, FR, DE, AR, HI)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NovieSurface)
                    .border(1.dp, NovieBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium, color = NovieTextPrimary)
                AppLanguage.entries.forEach { lang ->
                    val selected = lang == preferences.language
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) NovieMintContainer else NovieSurfaceElevated)
                            .border(1.dp, if (selected) NovieMint else NovieBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectLanguage(lang) }
                            .testTag("lang_option_${lang.code}")
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lang.nativeLabel, style = MaterialTheme.typography.titleSmall, color = NovieTextPrimary)
                        Text(lang.englishName, style = MaterialTheme.typography.bodySmall, color = if (selected) NovieMint else NovieTextSecondary)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StorageOptionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) NovieMintContainer.copy(alpha = 0.6f) else NovieSurfaceElevated)
            .border(1.dp, if (selected) NovieMint else NovieBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) NovieMint else NovieSurfaceHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) NovieOnMint else NovieCyan, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = NovieTextPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
        }
    }
}
