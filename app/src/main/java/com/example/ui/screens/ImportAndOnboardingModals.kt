package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.excel.ColumnMapping
import com.example.model.AppCurrency
import com.example.model.FlowType
import com.example.ui.components.GoogleSignInActionButton
import com.example.ui.theme.NovieBorder
import com.example.ui.theme.NovieCyan
import com.example.ui.theme.NovieCyanContainer
import com.example.ui.theme.NovieMint
import com.example.ui.theme.NovieMintContainer
import com.example.ui.theme.NovieOnMint
import com.example.ui.theme.NovieRose
import com.example.ui.theme.NovieSlateBg
import com.example.ui.theme.NovieSurface
import com.example.ui.theme.NovieSurfaceElevated
import com.example.ui.theme.NovieTextPrimary
import com.example.ui.theme.NovieTextSecondary
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.viewmodel.ImportPreviewState

@Composable
fun WorkspaceWelcomeScreen(
    onChoosePrivate: () -> Unit,
    onGoogleSyncAuthenticated: () -> Unit,
    onAuthError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovieSlateBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(NovieMint, NovieCyan))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ShowChart, contentDescription = null, tint = NovieOnMint, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = NovieTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = NovieTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Private On-Device Option
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(NovieSurface)
                .border(1.dp, NovieMint.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(NovieMintContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = NovieMint, modifier = Modifier.size(18.dp))
                }
                Text(stringResource(R.string.mode_private_title), style = MaterialTheme.typography.titleMedium, color = NovieTextPrimary)
            }
            Text(stringResource(R.string.mode_private_desc), style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
            Button(
                onClick = onChoosePrivate,
                colors = ButtonDefaults.buttonColors(containerColor = NovieMint, contentColor = NovieOnMint),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("welcome_private_button")
            ) {
                Text(stringResource(R.string.continue_private), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sync Option
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(NovieSurface)
                .border(1.dp, NovieCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(NovieCyanContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CloudDone, contentDescription = null, tint = NovieCyan, modifier = Modifier.size(18.dp))
                }
                Text(stringResource(R.string.mode_cloud_title), style = MaterialTheme.typography.titleMedium, color = NovieTextPrimary)
            }
            Text(stringResource(R.string.mode_cloud_desc), style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
            GoogleSignInActionButton(
                onSuccess = onGoogleSyncAuthenticated,
                onError = onAuthError,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GoogleSyncAuthGate(
    onSignedIn: () -> Unit,
    onSwitchToPrivate: () -> Unit,
    onAuthError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NovieCyanContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CloudDone, contentDescription = null, tint = NovieCyan, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.auth_gate_title), style = MaterialTheme.typography.headlineSmall, color = NovieTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.auth_gate_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = NovieTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(22.dp))
        GoogleSignInActionButton(
            onSuccess = onSignedIn,
            onError = onAuthError,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSwitchToPrivate,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(stringResource(R.string.continue_private), color = NovieTextPrimary)
        }
    }
}

@Composable
fun ExcelImportPreviewDialog(
    state: ImportPreviewState,
    onSelectSheet: (Int) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateCurrency: (String) -> Unit,
    onUpdateMapping: (ColumnMapping) -> Unit,
    onPickAnotherFile: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheet = state.workbook.sheets[state.selectedSheetIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = NovieSlateBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, NovieBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.import_preview_title), style = MaterialTheme.typography.titleLarge, color = NovieTextPrimary)
                        Text(state.workbook.fileName, style = MaterialTheme.typography.bodySmall, color = NovieCyan)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onPickAnotherFile,
                            modifier = Modifier.testTag("choose_another_file_button")
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, tint = NovieCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(stringResource(R.string.choose_another_file), color = NovieCyan)
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel), color = NovieTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.reportTitle,
                        onValueChange = onUpdateTitle,
                        label = { Text(stringResource(R.string.report_title_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.workbook.sheets.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.sheet_label), style = MaterialTheme.typography.labelLarge, color = NovieTextSecondary)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.workbook.sheets.forEachIndexed { idx, s ->
                                    val selected = idx == state.selectedSheetIndex
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) NovieMintContainer else NovieSurfaceElevated)
                                            .border(1.dp, if (selected) NovieMint else NovieBorder, RoundedCornerShape(10.dp))
                                            .clickable { onSelectSheet(idx) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(s.name, style = MaterialTheme.typography.labelLarge, color = if (selected) NovieMint else NovieTextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Source currency selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.base_currency_label), style = MaterialTheme.typography.labelLarge, color = NovieTextSecondary)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppCurrency.entries.forEach { curr ->
                                val selected = curr.code == state.baseCurrencyCode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) NovieCyanContainer else NovieSurfaceElevated)
                                        .border(1.dp, if (selected) NovieCyan else NovieBorder, RoundedCornerShape(10.dp))
                                        .clickable { onUpdateCurrency(curr.code) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("${curr.code} ${curr.symbol}", style = MaterialTheme.typography.labelMedium, color = if (selected) NovieCyan else NovieTextPrimary)
                                }
                            }
                        }
                    }

                    // Column Mapping Dropdowns
                    ColumnDropdownField(
                        label = stringResource(R.string.col_period),
                        headers = sheet.headers,
                        selectedIndex = state.mapping.periodCol,
                        allowNone = false,
                        onSelect = { idx -> onUpdateMapping(state.mapping.copy(periodCol = idx ?: 0)) }
                    )
                    ColumnDropdownField(
                        label = stringResource(R.string.col_category),
                        headers = sheet.headers,
                        selectedIndex = state.mapping.categoryCol,
                        allowNone = false,
                        onSelect = { idx -> onUpdateMapping(state.mapping.copy(categoryCol = idx ?: 0)) }
                    )
                    ColumnDropdownField(
                        label = stringResource(R.string.col_inflow),
                        headers = sheet.headers,
                        selectedIndex = state.mapping.inflowCol,
                        allowNone = true,
                        onSelect = { idx -> onUpdateMapping(state.mapping.copy(inflowCol = idx)) }
                    )
                    ColumnDropdownField(
                        label = stringResource(R.string.col_outflow),
                        headers = sheet.headers,
                        selectedIndex = state.mapping.outflowCol,
                        allowNone = true,
                        onSelect = { idx -> onUpdateMapping(state.mapping.copy(outflowCol = idx)) }
                    )
                    if (state.mapping.inflowCol == null && state.mapping.outflowCol == null) {
                        ColumnDropdownField(
                            label = stringResource(R.string.col_type),
                            headers = sheet.headers,
                            selectedIndex = state.mapping.typeCol,
                            allowNone = true,
                            onSelect = { idx -> onUpdateMapping(state.mapping.copy(typeCol = idx)) }
                        )
                        ColumnDropdownField(
                            label = stringResource(R.string.col_amount),
                            headers = sheet.headers,
                            selectedIndex = state.mapping.amountCol,
                            allowNone = true,
                            onSelect = { idx -> onUpdateMapping(state.mapping.copy(amountCol = idx)) }
                        )
                    }

                    // Preview summary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NovieSurface)
                            .border(1.dp, NovieBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.preview_rows_count, sheet.rows.size, state.parsedEntries.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (state.parsedEntries.isNotEmpty()) NovieMint else NovieRose
                            )
                            if (state.parsedEntries.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.no_mapped_entries_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NovieRose
                                )
                            } else {
                                state.parsedEntries.take(4).forEach { e ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${e.periodLabel} · ${e.category}", style = MaterialTheme.typography.bodySmall, color = NovieTextSecondary)
                                        Text(
                                            text = "${if (e.type == FlowType.INFLOW) "+" else "-"}${e.amount}",
                                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                                            color = if (e.type == FlowType.INFLOW) NovieMint else NovieRose
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onConfirm,
                    enabled = state.parsedEntries.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = NovieMint, contentColor = NovieOnMint),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_import_button")
                ) {
                    Text(stringResource(R.string.save_and_visualize), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ColumnDropdownField(
    label: String,
    headers: List<String>,
    selectedIndex: Int?,
    allowNone: Boolean,
    onSelect: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = selectedIndex?.let { headers.getOrNull(it) } ?: stringResource(R.string.col_none)

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NovieSurfaceElevated)
                .border(1.dp, NovieBorder, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = NovieTextSecondary)
                Text(selectedText, style = MaterialTheme.typography.bodyMedium, color = NovieTextPrimary)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = NovieMint)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowNone) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.col_none)) },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    }
                )
            }
            headers.forEachIndexed { idx, header ->
                DropdownMenuItem(
                    text = { Text(header) },
                    onClick = {
                        onSelect(idx)
                        expanded = false
                    }
                )
            }
        }
    }
}
