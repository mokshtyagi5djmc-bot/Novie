package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.authStateFlow
import com.example.data.HybridCashFlowRepository
import com.example.data.ai.AiFinancialInsightsService
import com.example.data.excel.ColumnMapping
import com.example.data.excel.ExcelSampleTemplate
import com.example.data.excel.ExcelTemplateLibrary
import com.example.data.excel.ExcelWorkbookParser
import com.example.data.excel.ParsedWorkbook
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesDataStore
import com.example.model.AnalyticsSummary
import com.example.model.AppCurrency
import com.example.model.AppLanguage
import com.example.model.CashFlowAnalyticsCalculator
import com.example.model.CashFlowEntry
import com.example.model.CashFlowReport
import com.example.model.FinancialInsight
import com.example.model.StorageMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data class Success(val reports: List<CashFlowReport>) : ReportsUiState
    data class Error(val message: String) : ReportsUiState
}

sealed interface AiInsightUiState {
    data object Idle : AiInsightUiState
    data object Loading : AiInsightUiState
    data class Success(val reportId: String, val insight: FinancialInsight) : AiInsightUiState
    data class Error(val message: String) : AiInsightUiState
}

data class ImportPreviewState(
    val workbook: ParsedWorkbook,
    val selectedSheetIndex: Int,
    val mapping: ColumnMapping,
    val reportTitle: String,
    val baseCurrencyCode: String,
    val parsedEntries: List<CashFlowEntry>
)

@OptIn(ExperimentalCoroutinesApi::class)
class NovieViewModel(
    private val preferencesStore: UserPreferencesDataStore,
    private val repository: HybridCashFlowRepository,
    private val aiService: AiFinancialInsightsService
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UserPreferences()
        )

    val authUser: StateFlow<FirebaseUser?> = Firebase.auth.authStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = Firebase.auth.currentUser
        )

    val reportsState: StateFlow<ReportsUiState> = combine(
        preferences.map { it.storageMode }.distinctUntilChanged(),
        authUser.map { it?.uid }.distinctUntilChanged()
    ) { mode, uid -> mode to uid }
        .flatMapLatest { (mode, uid) ->
            if (mode == StorageMode.GOOGLE_SYNC && uid.isNullOrBlank()) {
                flowOf<ReportsUiState>(ReportsUiState.Success(emptyList()))
            } else {
                repository.observeReports(mode, uid)
                    .map<List<CashFlowReport>, ReportsUiState> { ReportsUiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing reports", rootError)
                        emit(ReportsUiState.Error(rootError.localizedMessage ?: "Failed to load reports"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ReportsUiState.Loading
        )

    private val _selectedReportId = MutableStateFlow<String?>(null)

    val activeReport: StateFlow<CashFlowReport?> = combine(
        reportsState,
        _selectedReportId
    ) { state, selectedId ->
        val list = (state as? ReportsUiState.Success)?.reports.orEmpty()
        list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null
    )

    val analyticsSummary: StateFlow<AnalyticsSummary?> = combine(
        activeReport,
        preferences
    ) { report, prefs ->
        report?.let {
            CashFlowAnalyticsCalculator.compute(
                report = it,
                targetCurrency = prefs.currency,
                convertUsingRates = prefs.convertCurrencyUsingRates
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null
    )

    private val _importPreview = MutableStateFlow<ImportPreviewState?>(null)
    val importPreview: StateFlow<ImportPreviewState?> = _importPreview.asStateFlow()

    private val _isParsingFile = MutableStateFlow(false)
    val isParsingFile: StateFlow<Boolean> = _isParsingFile.asStateFlow()

    private val _aiInsightState = MutableStateFlow<AiInsightUiState>(AiInsightUiState.Idle)
    val aiInsightState: StateFlow<AiInsightUiState> = _aiInsightState.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var aiAnalysisJob: Job? = null

    fun setStorageMode(mode: StorageMode) {
        viewModelScope.launch {
            preferencesStore.setStorageMode(mode)
        }
    }

    fun markOnboardingDone() {
        viewModelScope.launch {
            preferencesStore.markOnboardingCompleted()
        }
    }

    fun setCurrency(currency: AppCurrency) {
        viewModelScope.launch {
            preferencesStore.setCurrency(currency)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesStore.setLanguage(language)
        }
    }

    fun setConvertCurrencyUsingRates(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.setConvertCurrencyUsingRates(enabled)
        }
    }

    fun selectReport(reportId: String) {
        if (_selectedReportId.value != reportId) {
            aiAnalysisJob?.cancel()
            aiAnalysisJob = null
        }
        _selectedReportId.value = reportId
        val currentAi = _aiInsightState.value
        if (currentAi is AiInsightUiState.Loading ||
            (currentAi is AiInsightUiState.Success && currentAi.reportId != reportId) ||
            currentAi is AiInsightUiState.Error
        ) {
            _aiInsightState.value = AiInsightUiState.Idle
        }
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    fun showBannerMessage(message: String) {
        _bannerMessage.value = message
    }

    fun loadSampleTemplate(template: ExcelSampleTemplate, onLoaded: () -> Unit = {}) {
        viewModelScope.launch {
            val prefs = preferences.value
            val uid = authUser.value?.uid
            val report = template.reportBuilder().copy(
                id = if (prefs.storageMode == StorageMode.GOOGLE_SYNC) {
                    "${template.id}_${System.currentTimeMillis()}"
                } else {
                    template.id
                }
            )
            val result = repository.saveReport(report, prefs.storageMode, uid)
            result.onSuccess { savedId ->
                _selectedReportId.value = savedId
                _aiInsightState.value = AiInsightUiState.Idle
                onLoaded()
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Unable to load template."
            }
        }
    }

    fun importExcelFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isParsingFile.value = true
            val result = ExcelWorkbookParser.parseUri(context, uri)
            _isParsingFile.value = false
            result.onSuccess { workbook ->
                val sheet = workbook.sheets.first()
                val mapping = ExcelWorkbookParser.autoDetectMapping(sheet.headers, sheet.rows)
                val entries = ExcelWorkbookParser.convertRowsToEntries(sheet, mapping)
                val cleanTitle = workbook.fileName
                    .substringBeforeLast('.')
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .trim()
                    .ifBlank { "Cash Flow Report" }

                _importPreview.value = ImportPreviewState(
                    workbook = workbook,
                    selectedSheetIndex = 0,
                    mapping = mapping,
                    reportTitle = cleanTitle,
                    baseCurrencyCode = preferences.value.currency.code,
                    parsedEntries = entries
                )
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to read Excel file."
            }
        }
    }

    fun selectImportSheet(index: Int) {
        val current = _importPreview.value ?: return
        val sheet = current.workbook.sheets.getOrNull(index) ?: return
        val mapping = ExcelWorkbookParser.autoDetectMapping(sheet.headers, sheet.rows)
        val entries = ExcelWorkbookParser.convertRowsToEntries(sheet, mapping)
        _importPreview.value = current.copy(
            selectedSheetIndex = index,
            mapping = mapping,
            parsedEntries = entries
        )
    }

    fun updateImportMapping(mapping: ColumnMapping) {
        val current = _importPreview.value ?: return
        val sheet = current.workbook.sheets.getOrNull(current.selectedSheetIndex) ?: return
        val entries = ExcelWorkbookParser.convertRowsToEntries(sheet, mapping)
        _importPreview.value = current.copy(
            mapping = mapping,
            parsedEntries = entries
        )
    }

    fun updateImportTitle(title: String) {
        _importPreview.value = _importPreview.value?.copy(reportTitle = title)
    }

    fun updateImportCurrency(code: String) {
        _importPreview.value = _importPreview.value?.copy(baseCurrencyCode = code)
    }

    fun dismissImportPreview() {
        _importPreview.value = null
    }

    fun commitImportPreview(onSaved: () -> Unit = {}) {
        val preview = _importPreview.value ?: return
        if (preview.parsedEntries.isEmpty()) {
            _bannerMessage.value = "No valid cash flow amounts were detected with the current column mapping."
            return
        }
        viewModelScope.launch {
            val prefs = preferences.value
            val uid = authUser.value?.uid
            val sheet = preview.workbook.sheets[preview.selectedSheetIndex]
            val report = CashFlowReport(
                id = UUID.randomUUID().toString(),
                title = preview.reportTitle.trim().ifBlank { "Cash Flow Report" },
                sourceFileName = preview.workbook.fileName,
                sheetName = sheet.name,
                baseCurrencyCode = preview.baseCurrencyCode,
                entries = preview.parsedEntries
            )
            val result = repository.saveReport(report, prefs.storageMode, uid)
            result.onSuccess { savedId ->
                _importPreview.value = null
                _selectedReportId.value = savedId
                _aiInsightState.value = AiInsightUiState.Idle
                onSaved()
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to save imported report."
            }
        }
    }

    fun renameReport(reportId: String, newTitle: String) {
        viewModelScope.launch {
            val mode = preferences.value.storageMode
            val uid = authUser.value?.uid
            repository.renameReport(reportId, newTitle, mode, uid)
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Rename failed."
                }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            val mode = preferences.value.storageMode
            val uid = authUser.value?.uid
            repository.deleteReport(reportId, mode, uid)
                .onSuccess {
                    if (_selectedReportId.value == reportId) {
                        _selectedReportId.value = null
                        _aiInsightState.value = AiInsightUiState.Idle
                    }
                }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Delete failed."
                }
        }
    }

    fun syncLocalReportsToCloud(onSynced: (Int) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncLocalReportsToCloud()
            _isSyncing.value = false
            result.onSuccess { count ->
                onSynced(count)
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Cloud sync failed."
            }
        }
    }

    fun exportTemplate(context: Context, uri: Uri, template: ExcelSampleTemplate, successMsg: String) {
        viewModelScope.launch {
            ExcelTemplateLibrary.exportTemplateAsXlsx(context, uri, template)
                .onSuccess { _bannerMessage.value = successMsg }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Excel export failed."
                }
        }
    }

    fun runAiAnalysis() {
        val report = activeReport.value ?: return
        val summary = analyticsSummary.value ?: return
        val prefs = preferences.value
        aiAnalysisJob?.cancel()
        aiAnalysisJob = viewModelScope.launch {
            _aiInsightState.value = AiInsightUiState.Loading
            val result = aiService.analyzeReport(
                report = report,
                summary = summary,
                currency = prefs.currency,
                language = prefs.language
            )
            _aiInsightState.value = result.fold(
                onSuccess = { insight -> AiInsightUiState.Success(report.id, insight) },
                onFailure = { err ->
                    AiInsightUiState.Error(err.localizedMessage ?: "AI analysis failed. Please try again.")
                }
            )
        }
    }

    private companion object {
        const val TAG = "NovieViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
