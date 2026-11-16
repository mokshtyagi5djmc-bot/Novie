package com.example.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.attemptSilentGoogleSignIn
import com.example.data.HybridCashFlowRepository
import com.example.data.ai.AiFinancialInsightsService
import com.example.data.excel.ExcelSampleTemplate
import com.example.data.local.LocalCashFlowRepository
import com.example.data.local.NovieDatabase
import com.example.data.local.UserPreferencesDataStore
import com.example.data.remote.FirestoreCashFlowRepository
import com.example.model.AppLanguage
import com.example.model.StorageMode
import com.example.ui.components.CurrencyPickerDialog
import com.example.ui.components.LanguagePickerDialog
import com.example.ui.components.NovieTopHeader
import com.example.ui.screens.ChartsLabScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExcelImportPreviewDialog
import com.example.ui.screens.GoogleSyncAuthGate
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkspaceWelcomeScreen
import com.example.ui.theme.NovieMint
import com.example.ui.theme.NovieMintContainer
import com.example.ui.theme.NovieSlateBg
import com.example.ui.theme.NovieSurface
import com.example.ui.theme.NovieTextPrimary
import com.example.ui.theme.NovieTextSecondary
import com.example.ui.viewmodel.NovieViewModel
import com.example.ui.viewmodel.ReportsUiState

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_CHARTS = "charts"
const val ROUTE_REPORTS = "reports"
const val ROUTE_SETTINGS = "settings"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NovieDestinations = listOf(
    NavDestination(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    NavDestination(ROUTE_CHARTS, R.string.nav_charts, Icons.Filled.BarChart, Icons.Outlined.BarChart),
    NavDestination(ROUTE_REPORTS, R.string.nav_reports, Icons.Filled.Description, Icons.Outlined.Description),
    NavDestination(ROUTE_SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun NovieAppRoot() {
    val baseContext = LocalContext.current
    val appContext = baseContext.applicationContext

    val vm: NovieViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val db = NovieDatabase.getInstance(appContext)
                val prefs = UserPreferencesDataStore(appContext)
                val localRepo = LocalCashFlowRepository(db.cashFlowDao())
                val remoteRepo = FirestoreCashFlowRepository(appContext)
                NovieViewModel(
                    preferencesStore = prefs,
                    repository = HybridCashFlowRepository(localRepo, remoteRepo),
                    aiService = AiFinancialInsightsService()
                )
            }
        }
    )

    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val authUser by vm.authUser.collectAsStateWithLifecycle()
    val reportsState by vm.reportsState.collectAsStateWithLifecycle()
    val activeReport by vm.activeReport.collectAsStateWithLifecycle()
    val summary by vm.analyticsSummary.collectAsStateWithLifecycle()
    val importPreview by vm.importPreview.collectAsStateWithLifecycle()
    val isParsingFile by vm.isParsingFile.collectAsStateWithLifecycle()
    val aiInsightState by vm.aiInsightState.collectAsStateWithLifecycle()
    val bannerMessage by vm.bannerMessage.collectAsStateWithLifecycle()
    val isSyncing by vm.isSyncing.collectAsStateWithLifecycle()

    val localizedContext = remember(preferences.language, baseContext) {
        createLocalizedContext(baseContext, preferences.language)
    }
    val layoutDirection = if (preferences.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current

    var pendingExportTemplate by remember { mutableStateOf<ExcelSampleTemplate?>(null) }

    val excelOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                baseContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.importExcelFile(localizedContext, uri)
        }
    }

    val excelExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        val template = pendingExportTemplate
        pendingExportTemplate = null
        if (uri != null && template != null) {
            vm.exportTemplate(
                context = localizedContext,
                uri = uri,
                template = template,
                successMsg = localizedContext.getString(R.string.export_success)
            )
        }
    }

    val providedValues = buildList {
        add(LocalContext provides localizedContext)
        add(LocalConfiguration provides localizedContext.resources.configuration)
        add(LocalLayoutDirection provides layoutDirection)
        activityResultRegistryOwner?.let { add(LocalActivityResultRegistryOwner provides it) }
        onBackPressedDispatcherOwner?.let { add(LocalOnBackPressedDispatcherOwner provides it) }
    }.toTypedArray()

    CompositionLocalProvider(*providedValues) {
        val currentContext = LocalContext.current
        val scope = rememberCoroutineScope()
        val credentialManager = remember(baseContext) { CredentialManager.create(baseContext) }
        val snackbarHostState = remember { SnackbarHostState() }

        var showCurrencyPicker by remember { mutableStateOf(false) }
        var showLanguagePicker by remember { mutableStateOf(false) }

        LaunchedEffect(preferences.storageMode, authUser?.uid) {
            if (preferences.storageMode == StorageMode.GOOGLE_SYNC && authUser == null) {
                attemptSilentGoogleSignIn(
                    context = baseContext,
                    credentialManager = credentialManager,
                    scope = scope,
                    onSuccess = {},
                    onUnauthenticated = {}
                )
            }
        }

        LaunchedEffect(bannerMessage) {
            val msg = bannerMessage
            if (!msg.isNullOrBlank()) {
                snackbarHostState.showSnackbar(msg)
                vm.clearBannerMessage()
            }
        }

        if (!preferences.onboardingCompleted) {
            WorkspaceWelcomeScreen(
                onChoosePrivate = { vm.setStorageMode(StorageMode.PRIVATE_LOCAL) },
                onGoogleSyncAuthenticated = { vm.setStorageMode(StorageMode.GOOGLE_SYNC) },
                onAuthError = { msg -> vm.showBannerMessage(msg) }
            )
            return@CompositionLocalProvider
        }

        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD

        fun navigateToTab(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        val openExcelPicker: () -> Unit = {
            excelOpenLauncher.launch(
                arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    "text/csv",
                    "text/comma-separated-values",
                    "application/csv",
                    "text/x-csv",
                    "text/plain",
                    "application/octet-stream",
                    "*/*"
                )
            )
        }

        val triggerTemplateExport: (ExcelSampleTemplate) -> Unit = { template ->
            pendingExportTemplate = template
            excelExportLauncher.launch(template.fileName)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(NovieSlateBg)
        ) {
            val isWideScreen = maxWidth >= 700.dp

            Scaffold(
                containerColor = NovieSlateBg,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.navigationBars,
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = NovieSurface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NovieDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NovieMint,
                                        selectedTextColor = NovieMint,
                                        indicatorColor = NovieMintContainer,
                                        unselectedIconColor = NovieTextSecondary,
                                        unselectedTextColor = NovieTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding()
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = NovieSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NovieDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = NovieMint,
                                        selectedTextColor = NovieMint,
                                        indicatorColor = NovieMintContainer,
                                        unselectedIconColor = NovieTextSecondary,
                                        unselectedTextColor = NovieTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 900.dp)
                        ) {
                            NovieTopHeader(
                                currency = preferences.currency,
                                language = preferences.language,
                                storageMode = preferences.storageMode,
                                isSignedIn = authUser != null,
                                onCurrencyClick = { showCurrencyPicker = true },
                                onLanguageClick = { showLanguagePicker = true }
                            )

                            val needsGoogleGate = preferences.storageMode == StorageMode.GOOGLE_SYNC &&
                                authUser == null &&
                                currentRoute != ROUTE_SETTINGS

                            if (needsGoogleGate) {
                                GoogleSyncAuthGate(
                                    onSignedIn = {},
                                    onSwitchToPrivate = { vm.setStorageMode(StorageMode.PRIVATE_LOCAL) },
                                    onAuthError = { msg -> vm.showBannerMessage(msg) }
                                )
                            } else {
                                val reportList = (reportsState as? ReportsUiState.Success)?.reports.orEmpty()

                                if (reportsState is ReportsUiState.Loading && reportList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = NovieMint)
                                    }
                                } else {
                                    NavHost(
                                        navController = navController,
                                        startDestination = ROUTE_DASHBOARD,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        composable(ROUTE_DASHBOARD) {
                                            DashboardScreen(
                                                reports = reportList,
                                                activeReport = activeReport,
                                                summary = summary,
                                                currency = preferences.currency,
                                                language = preferences.language,
                                                isParsingFile = isParsingFile,
                                                aiInsightState = aiInsightState,
                                                onSelectReport = vm::selectReport,
                                                onPickExcelFile = openExcelPicker,
                                                onLoadTemplate = { tpl -> vm.loadSampleTemplate(tpl) },
                                                onExportTemplate = triggerTemplateExport,
                                                onAnalyzeWithAi = vm::runAiAnalysis,
                                                onOpenChartsLab = { navigateToTab(ROUTE_CHARTS) }
                                            )
                                        }
                                        composable(ROUTE_CHARTS) {
                                            ChartsLabScreen(
                                                reports = reportList,
                                                activeReport = activeReport,
                                                summary = summary,
                                                currency = preferences.currency,
                                                language = preferences.language,
                                                isParsingFile = isParsingFile,
                                                onSelectReport = vm::selectReport,
                                                onPickExcelFile = openExcelPicker,
                                                onLoadTemplate = { tpl -> vm.loadSampleTemplate(tpl) },
                                                onExportTemplate = triggerTemplateExport
                                            )
                                        }
                                        composable(ROUTE_REPORTS) {
                                            ReportsScreen(
                                                reports = reportList,
                                                activeReport = activeReport,
                                                currency = preferences.currency,
                                                language = preferences.language,
                                                convertUsingRates = preferences.convertCurrencyUsingRates,
                                                isParsingFile = isParsingFile,
                                                onSelectReport = { id ->
                                                    vm.selectReport(id)
                                                    navigateToTab(ROUTE_DASHBOARD)
                                                },
                                                onRenameReport = vm::renameReport,
                                                onDeleteReport = vm::deleteReport,
                                                onPickExcelFile = openExcelPicker
                                            )
                                        }
                                        composable(ROUTE_SETTINGS) {
                                            SettingsScreen(
                                                preferences = preferences,
                                                authUser = authUser,
                                                isSyncing = isSyncing,
                                                onSelectCurrency = vm::setCurrency,
                                                onToggleConvertRates = vm::setConvertCurrencyUsingRates,
                                                onSelectLanguage = vm::setLanguage,
                                                onSelectStorageMode = vm::setStorageMode,
                                                onSyncLocalToCloud = {
                                                    vm.syncLocalReportsToCloud { count ->
                                                        vm.showBannerMessage(
                                                            currentContext.getString(R.string.sync_success_msg, count)
                                                        )
                                                    }
                                                },
                                                onAuthError = { msg -> vm.showBannerMessage(msg) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCurrencyPicker) {
            CurrencyPickerDialog(
                selected = preferences.currency,
                onSelect = vm::setCurrency,
                onDismiss = { showCurrencyPicker = false }
            )
        }

        if (showLanguagePicker) {
            LanguagePickerDialog(
                selected = preferences.language,
                onSelect = vm::setLanguage,
                onDismiss = { showLanguagePicker = false }
            )
        }

        importPreview?.let { previewState ->
            ExcelImportPreviewDialog(
                state = previewState,
                onSelectSheet = vm::selectImportSheet,
                onUpdateTitle = vm::updateImportTitle,
                onUpdateCurrency = vm::updateImportCurrency,
                onUpdateMapping = vm::updateImportMapping,
                onPickAnotherFile = openExcelPicker,
                onConfirm = {
                    vm.commitImportPreview {
                        navigateToTab(ROUTE_DASHBOARD)
                    }
                },
                onDismiss = vm::dismissImportPreview
            )
        }
    }
}

private fun createLocalizedContext(base: Context, language: AppLanguage): Context {
    val locale = language.toLocale()
    val config = Configuration(base.resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    val localized = base.createConfigurationContext(config)
    return object : ContextWrapper(base) {
        override fun getResources(): Resources = localized.resources
        override fun getAssets(): AssetManager = localized.assets
    }
}
