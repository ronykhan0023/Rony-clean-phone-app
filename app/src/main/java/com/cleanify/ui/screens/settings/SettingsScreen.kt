package com.cleanify.ui.screens.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.data.repository.AppLocale
import com.cleanify.data.repository.DuplicateScanScope
import com.cleanify.data.repository.PreferencesRepository
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.ui.components.FolderSearchDialog
import com.cleanify.ui.screens.settings.sections.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.cleanify.ui.components.CleanifySearchBar
import com.cleanify.ui.theme.isDark
import com.cleanify.util.Formatters
import com.cleanify.util.WhatsAppLauncher
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.io.File

private data class SettingsCategory(
    val id: String,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

private val categories = listOf(
    SettingsCategory("appearance", R.string.appearance_section_title, R.string.appearance_section_desc, Icons.Default.Palette),
    SettingsCategory("sorting", R.string.gestures_section_title, R.string.gestures_section_desc, Icons.AutoMirrored.Filled.Sort),
    SettingsCategory("storage_management", R.string.storage_management_title, R.string.storage_management_desc, Icons.Default.SdCard),
    SettingsCategory("duplicates", R.string.duplicate_finder_section_title, R.string.duplicate_finder_section_desc, Icons.Default.ContentCopy),
    SettingsCategory("cleaner", R.string.cleaner_section_title, R.string.cleaner_section_desc, Icons.Default.CleaningServices),
    SettingsCategory("help", R.string.help_support_section_title, R.string.help_support_section_desc, Icons.AutoMirrored.Filled.HelpOutline),
    SettingsCategory("about", R.string.about_section_title, R.string.about_section_desc, Icons.Default.Info)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToLibraries: () -> Unit,
    onNavigateToCleanerSettings: () -> Unit = {},
    initialPage: String? = null,
    initialHighlight: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val folderSearchState by viewModel.folderSearchManager.state.collectAsStateWithLifecycle()
    val displayedUnindexedFiles by viewModel.displayedUnindexedFiles.collectAsStateWithLifecycle()
    val accentColorKey by viewModel.accentColorKey.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isGestureMode = com.cleanify.util.rememberIsUsingGestureNavigation()

    val mainMenuScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(initial = 0) }
    var targetHighlightItem by remember { mutableStateOf<String?>(initialHighlight) }
    var currentPage by rememberSaveable { mutableStateOf<String?>(initialPage) }

    LaunchedEffect(initialPage) {
        if (initialPage == "cleaner") {
            onNavigateToCleanerSettings()
            onNavigateUp()
        } else if (initialPage != null) {
            currentPage = initialPage
        }
    }

    LaunchedEffect(initialHighlight) {
        if (initialHighlight != null) {
            targetHighlightItem = initialHighlight
            kotlinx.coroutines.delay(2500)
            targetHighlightItem = null
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = uiState.isSearchActive || uiState.searchQuery.isNotEmpty()) {
        viewModel.toggleSearch()
        keyboardController?.hide()
    }

    BackHandler(enabled = currentPage != null && !uiState.isSearchActive && uiState.searchQuery.isEmpty()) {
        if (initialPage != null) {
            onNavigateUp()
        } else {
            currentPage = null
        }
    }

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.toastMessageShown()
        }
    }

    val defaultExportFilename = stringResource(R.string.export_filename_default)
    val exportFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportTargetFavorites(it) } }
    val importFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importTargetFavorites(it) } }

    val pageTitle = currentPage?.let { id ->
        when (id) {
            "appearance" -> R.string.appearance_section_title
            "sorting" -> R.string.gestures_section_title
            "behavior" -> R.string.behavior_section_title
            "storage_management" -> R.string.storage_management_title
            "duplicates" -> R.string.duplicate_finder_section_title
            "media_storage" -> R.string.media_storage_section_title
            "help" -> R.string.help_support_section_title
            "about" -> R.string.about_section_title
            else -> null
        }?.let { stringResource(it) }
    }

    Scaffold(
                topBar = {
            TopAppBar(
                title = {
                    Text(if (currentPage != null) (pageTitle ?: "") else stringResource(R.string.settings))
                },
                navigationIcon = {
                    BackNavigationIcon(
                        onClick = { if (currentPage != null) { if (initialPage != null) onNavigateUp() else currentPage = null } else onNavigateUp() },
                        contentDescription = stringResource(R.string.navigate_back)
                    )
                },
                actions = {
                    val isSearching = uiState.isSearchActive || uiState.searchQuery.isNotEmpty()
                    IconButton(onClick = viewModel::toggleSearch) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = if (isSearching) stringResource(R.string.close_search) else stringResource(R.string.search_settings_icon_desc),
                            tint = if (isSearching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AnimatedVisibility(
                visible = uiState.isSearchActive || uiState.searchQuery.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CleanifySearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    placeholder = stringResource(R.string.search_settings_placeholder),
                    height = 48.dp,
                    focusRequester = focusRequester,
                    onSearchAction = { keyboardController?.hide() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                CompositionLocalProvider(LocalHighlightedSettingId provides targetHighlightItem) {
                    if (debouncedSearchQuery.isNotBlank()) {
                        SettingsMainMenu(
                            categories = categories,
                            searchQuery = debouncedSearchQuery,
                            viewModel = viewModel,
                            onCategoryClick = { catId ->
                                keyboardController?.hide()
                                viewModel.toggleSearch()
                                currentPage = catId
                            },
                            onCleanerClick = {
                                keyboardController?.hide()
                                viewModel.toggleSearch()
                                onNavigateToCleanerSettings()
                            },
                            onSettingClick = { item ->
                                keyboardController?.hide()
                                viewModel.toggleSearch()
                                if (item.sectionId == "cleaner") {
                                    onNavigateToCleanerSettings()
                                } else {
                                    currentPage = item.sectionId
                                    targetHighlightItem = item.id
                                    scope.launch {
                                        kotlinx.coroutines.delay(2500)
                                        targetHighlightItem = null
                                    }
                                }
                            },
                            scrollState = rememberScrollState()
                        )
                    } else {
                        AnimatedContent(
                            targetState = currentPage,
                            transitionSpec = {
                                if (targetState != null && initialState == null) {
                                    (slideInHorizontally(animationSpec = tween(220)) { width -> width / 4 } + fadeIn(animationSpec = tween(220)))
                                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { width -> -width / 4 } + fadeOut(animationSpec = tween(180)))
                                } else if (targetState == null && initialState != null) {
                                    (slideInHorizontally(animationSpec = tween(220)) { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { width -> width / 4 } + fadeOut(animationSpec = tween(180)))
                                } else {
                                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
                                }
                            },
                            label = "SettingsPageTransition"
                        ) { page ->
                            when (page) {
                                "appearance" -> AppearanceSection(
                                    viewModel = viewModel,
                                    onNavigateUp = { if (initialPage != null) onNavigateUp() else currentPage = null }
                                )
                                "sorting", "behavior", "media_storage" -> SortingSection(viewModel = viewModel)
                                "storage_management" -> StorageManagementSection(
                                    viewModel = viewModel,
                                    onNavigateToCleaner = onNavigateToCleanerSettings
                                )
                                "duplicates" -> DuplicatesSection(viewModel = viewModel)
                                "help" -> HelpSection(
                                    viewModel = viewModel,
                                    exportFavoritesLauncher = exportFavoritesLauncher,
                                    importFavoritesLauncher = importFavoritesLauncher,
                                    defaultExportFilename = defaultExportFilename
                                )
                                "about" -> AboutSection(
                                    viewModel = viewModel,
                                    onNavigateToLibraries = onNavigateToLibraries
                                )
                                else -> SettingsMainMenu(
                                    categories = categories,
                                    searchQuery = "",
                                    viewModel = viewModel,
                                    onCategoryClick = { currentPage = it },
                                    onCleanerClick = onNavigateToCleanerSettings,
                                    onSettingClick = { item ->
                                        if (item.sectionId == "cleaner") {
                                            onNavigateToCleanerSettings()
                                        } else {
                                            currentPage = item.sectionId
                                            targetHighlightItem = item.id
                                            scope.launch {
                                                kotlinx.coroutines.delay(2500)
                                                targetHighlightItem = null
                                            }
                                        }
                                    },
                                    scrollState = mainMenuScrollState
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showUnindexedFilesDialog) {
        UnindexedFilesDialog(
            filePaths = displayedUnindexedFiles,
            totalUnindexedCount = uiState.unindexedFilePaths.size,
            showHidden = uiState.showHiddenUnindexedFiles,
            onToggleShowHidden = viewModel::toggleShowHiddenUnindexedFiles,
            onDismiss = viewModel::dismissUnindexedFilesDialog
        )
    }
    if (uiState.showAccentColorDialog) {
        AccentColorDialog(
            currentAccentKey = accentColorKey,
            onDismiss = viewModel::dismissAccentColorDialog,
            onColorSelected = viewModel::setAccentColor
        )
    }
    if (uiState.showDuplicateScanScopeDialog) {
        val duplicateScanScope = viewModel.duplicateScanScope.collectAsStateWithLifecycle().value
        val duplicateScanIncludeList = viewModel.duplicateScanIncludeList.collectAsStateWithLifecycle().value
        val duplicateScanExcludeList = viewModel.duplicateScanExcludeList.collectAsStateWithLifecycle().value
        val title = if (duplicateScanScope == DuplicateScanScope.INCLUDE_LIST) stringResource(R.string.manage_include_list) else stringResource(R.string.manage_exclude_list)
        val folderList = if (duplicateScanScope == DuplicateScanScope.INCLUDE_LIST) duplicateScanIncludeList else duplicateScanExcludeList
        DuplicateScanScopeManagementDialog(
            title = title,
            folderList = folderList.toList(),
            onDismiss = viewModel::dismissDuplicateScanScopeDialog,
            onAddFolder = { viewModel.showDuplicateScanScopeFolderSearch(duplicateScanScope == DuplicateScanScope.INCLUDE_LIST) },
            onRemoveFolder = viewModel::removeFolderFromScanScopeList
        )
    }
    if (uiState.showDuplicateScanScopeFolderSearch) {
        FolderSearchDialog(
            state = folderSearchState,
            title = stringResource(R.string.add_folder),
            searchLabel = stringResource(R.string.search_hint) + "\u2026",
            confirmButtonText = stringResource(R.string.confirm),
            autoConfirmOnSelection = false,
            onDismiss = viewModel::dismissFolderSearchDialog,
            onQueryChanged = viewModel.folderSearchManager::updateSearchQuery,
            onFolderSelected = viewModel::onPathSelected,
            onConfirm = {
                val selectedPath = folderSearchState.browsePath
                if (selectedPath != null) viewModel.addFolderToScanScopeList(selectedPath)
            },
            onSearch = { scope.launch { viewModel.folderSearchManager.selectSingleResultOrSelf() } },
            formatListItemTitle = Formatters::pathForDisplay
        )
    }
    if (uiState.showDefaultPathSearchDialog) {
        FolderSearchDialog(
            state = folderSearchState,
            title = stringResource(R.string.default_album_location_title),
            searchLabel = stringResource(R.string.search_hint) + "\u2026",
            confirmButtonText = stringResource(R.string.confirm),
            autoConfirmOnSelection = false,
            onDismiss = viewModel::dismissFolderSearchDialog,
            onQueryChanged = viewModel.folderSearchManager::updateSearchQuery,
            onFolderSelected = viewModel::onPathSelected,
            onConfirm = viewModel::confirmDefaultPathSelection,
            onSearch = { scope.launch { viewModel.folderSearchManager.selectSingleResultOrSelf() } },
            formatListItemTitle = Formatters::pathForDisplay
        )
    }
    if (uiState.showForgetMediaSearchDialog) {
        FolderSearchDialog(
            state = folderSearchState,
            title = stringResource(R.string.forget_sorted_media_title),
            searchLabel = stringResource(R.string.search_hint) + "\u2026",
            confirmButtonText = stringResource(R.string.forget_action),
            autoConfirmOnSelection = true,
            onDismiss = viewModel::dismissFolderSearchDialog,
            onQueryChanged = viewModel.folderSearchManager::updateSearchQuery,
            onFolderSelected = { path -> viewModel.forgetSortedMediaInFolder(path) },
            onConfirm = {},
            onSearch = { scope.launch { viewModel.folderSearchManager.selectSingleResultOrSelf() } },
            formatListItemTitle = Formatters::pathForDisplay
        )
    }
    if (uiState.showConfirmForgetFolderDialog) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("forgetFolder") },
            showDontAskAgain = true,
            dontAskAgainChecked = uiState.dontAskAgainForgetFolder,
            onDontAskAgainChanged = { viewModel.onDontAskAgainChanged("forgetFolder", it) },
            title = { Text(stringResource(R.string.forget_confirm_title)) },
            text = { Text(stringResource(R.string.forget_confirm_body, File(uiState.folderToForget ?: "").name)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("forgetFolder") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmForgetSortedMediaInFolder) { Text(stringResource(R.string.confirm)) }
            }
        )
    }
    if (uiState.showResetDialogsConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("resetWarnings") },
            title = { Text(stringResource(R.string.reset_all_warnings_title)) },
            text = { Text(stringResource(R.string.reset_all_warnings_body)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("resetWarnings") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmResetDialogWarnings) { Text(stringResource(R.string.reset)) }
            }
        )
    }
    if (uiState.showResetHistoryConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("resetHistory") },
            showDontAskAgain = true,
            dontAskAgainChecked = uiState.dontAskAgainResetHistory,
            onDontAskAgainChanged = { viewModel.onDontAskAgainChanged("resetHistory", it) },
            title = { Text(stringResource(R.string.reset_history_title)) },
            text = { Text(stringResource(R.string.reset_history_body)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("resetHistory") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmResetHistory) { Text(stringResource(R.string.reset)) }
            }
        )
    }
    if (uiState.showResetSourceFavoritesConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("resetSource") },
            showDontAskAgain = true,
            dontAskAgainChecked = uiState.dontAskAgainResetSourceFavorites,
            onDontAskAgainChanged = { viewModel.onDontAskAgainChanged("resetSource", it) },
            title = { Text(stringResource(R.string.reset_source_favs_title)) },
            text = { Text(stringResource(R.string.reset_source_favs_body)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("resetSource") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmClearSourceFavorites) { Text(stringResource(R.string.reset)) }
            }
        )
    }
    if (uiState.showResetTargetFavoritesConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("resetTarget") },
            showDontAskAgain = true,
            dontAskAgainChecked = uiState.dontAskAgainResetTargetFavorites,
            onDontAskAgainChanged = { viewModel.onDontAskAgainChanged("resetTarget", it) },
            title = { Text(stringResource(R.string.reset_target_favs_title)) },
            text = { Text(stringResource(R.string.reset_target_favs_body)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("resetTarget") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmClearTargetFavorites) { Text(stringResource(R.string.reset)) }
            }
        )
    }

    val missingFolders = uiState.missingImportedFolders
    if (missingFolders != null) {
        AppDialog(
            onDismissRequest = { viewModel.dismissMissingFoldersDialog() },
            title = { Text(stringResource(R.string.some_folders_not_found_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.some_folders_not_found_body))
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
                        missingFolders.forEach { path ->
                            Text(".../${path.takeLast(35)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { viewModel.dismissMissingFoldersDialog() }) { Text(stringResource(R.string.skip)) }
                Button(onClick = { viewModel.createAndImportMissingFolders() }) { Text(stringResource(R.string.create)) }
            }
        )
    }
}

@Composable
private fun SettingsMainMenu(
    categories: List<SettingsCategory>,
    searchQuery: String,
    viewModel: SettingsViewModel,
    onCategoryClick: (String) -> Unit,
    onCleanerClick: () -> Unit = {},
    onSettingClick: (SearchableSettingItem) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()
) {
    val resources = LocalResources.current
    val isSearching = searchQuery.isNotBlank()

    val filteredSettings = remember(searchQuery) {
        if (!isSearching) emptyList() else {
            allSearchableSettings.filter { item ->
                val title = resources.getString(item.titleRes)
                val desc = item.descRes?.let { resources.getString(it) } ?: item.customDesc ?: ""
                title.contains(searchQuery, ignoreCase = true) ||
                    desc.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredCategories = remember(searchQuery) {
        if (!isSearching) categories else {
            categories.filter { cat ->
                val title = resources.getString(cat.titleRes)
                val desc = resources.getString(cat.descRes)
                title.contains(searchQuery, ignoreCase = true) ||
                    desc.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isSearching && filteredSettings.isEmpty() && filteredCategories.isEmpty()) {
            Text(
                text = stringResource(R.string.no_settings_found, searchQuery),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        // 1. Search Results
        if (isSearching) {
            if (filteredSettings.isNotEmpty()) {
                FrostedSettingsGroup(title = "Settings (${filteredSettings.size})") {
                    filteredSettings.forEachIndexed { index, setting ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSettingClick(setting) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = setting.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${stringResource(setting.sectionTitleRes)} ›",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = stringResource(setting.titleRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                val desc = setting.descRes?.let { stringResource(it) } ?: setting.customDesc
                                if (!desc.isNullOrBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        if (index < filteredSettings.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            if (filteredCategories.isNotEmpty()) {
                FrostedSettingsGroup(title = "Sections (${filteredCategories.size})") {
                    filteredCategories.forEachIndexed { index, category ->
                        SettingsCategoryRow(
                            category = category,
                            onClick = {
                                if (category.id == "cleaner") onCleanerClick() else onCategoryClick(category.id)
                            }
                        )
                        if (index < filteredCategories.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        } else {
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            val currentLocale by viewModel.currentLocale.collectAsStateWithLifecycle()
            val eyeSaverEnabled by viewModel.eyeSaverEnabled.collectAsStateWithLifecycle()
            val isDarkActive = currentTheme.isDark
            val context = LocalContext.current
            var showLanguageMenu by remember { mutableStateOf(false) }

            // Display & Comfort Quick Controls (Dark Mode + Eye Saver + Language)
            FrostedSettingsGroup(title = stringResource(R.string.display_modes_section_header)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setDarkModeEnabled(!isDarkActive) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkActive) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = stringResource(R.string.dark_mode_title),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dark_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.dark_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkActive,
                        onCheckedChange = { viewModel.setDarkModeEnabled(it) },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setEyeSaverEnabled(!eyeSaverEnabled) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (eyeSaverEnabled) Color(0xFFFF9800).copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = stringResource(R.string.eye_saver_title),
                            tint = if (eyeSaverEnabled) Color(0xFFE65100) else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.eye_saver_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.eye_saver_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = eyeSaverEnabled,
                        onCheckedChange = { viewModel.setEyeSaverEnabled(it) },
                        modifier = Modifier.testTag("eye_saver_switch")
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageMenu = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("quick_language_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(R.string.language_title),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.language_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = getAppLocaleDisplayName(currentLocale),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = stringResource(R.string.language_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        AppLocale.entries.forEach { localeOption ->
                            val isSelected = localeOption == currentLocale
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = getAppLocaleDisplayName(localeOption),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Spacer(Modifier.width(12.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.setAppLocale(localeOption)
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Contact with me (WhatsApp icon only — number stays hidden and opens WhatsApp directly)
            FrostedSettingsGroup(title = stringResource(R.string.whatsapp_contact_section_header)) {
                val openWhatsApp: () -> Unit = {
                    WhatsAppLauncher.openWhatsAppChat(
                        context,
                        PreferencesRepository.DEFAULT_WHATSAPP_NUMBER
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = openWhatsApp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = openWhatsApp,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF25D366),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("whatsapp_contact_icon_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_whatsapp),
                                contentDescription = stringResource(R.string.contact_with_me_title),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.contact_with_me_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.whatsapp_tap_to_open),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // 2. Normal grouped categories
            val preferenceCategories = remember(filteredCategories) {
                filteredCategories.filter { it.id in listOf("appearance", "sorting") }
            }
            val toolCategories = remember(filteredCategories) {
                filteredCategories.filter { it.id in listOf("storage_management", "duplicates", "cleaner") }
            }
            val supportCategories = remember(filteredCategories) {
                filteredCategories.filter { it.id in listOf("help", "about") }
            }

            if (preferenceCategories.isNotEmpty()) {
                FrostedSettingsGroup(title = "Preferences") {
                    preferenceCategories.forEachIndexed { index, category ->
                        SettingsCategoryRow(
                            category = category,
                            onClick = { onCategoryClick(category.id) }
                        )
                        if (index < preferenceCategories.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            if (toolCategories.isNotEmpty()) {
                FrostedSettingsGroup(title = "Tools") {
                    toolCategories.forEachIndexed { index, category ->
                        SettingsCategoryRow(
                            category = category,
                            onClick = {
                                if (category.id == "cleaner") onCleanerClick() else onCategoryClick(category.id)
                            }
                        )
                        if (index < toolCategories.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            if (supportCategories.isNotEmpty()) {
                FrostedSettingsGroup(title = "Support & About") {
                    supportCategories.forEachIndexed { index, category ->
                        SettingsCategoryRow(
                            category = category,
                            onClick = { onCategoryClick(category.id) }
                        )
                        if (index < supportCategories.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(category.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun UnindexedFilesDialog(filePaths: List<String>, totalUnindexedCount: Int, showHidden: Boolean, onToggleShowHidden: () -> Unit, onDismiss: () -> Unit) {
    val groupedFiles = remember(filePaths) { filePaths.groupBy { File(it).parent ?: "Unknown Location" } }
    val unknownLocation = stringResource(R.string.unknown_location)
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.unindexed_files_dialog_title)) },
        text = {
            Column {
                if (filePaths.isEmpty() && totalUnindexedCount > 0) Text(stringResource(R.string.unindexed_files_ideal_state), style = MaterialTheme.typography.bodyMedium)
                else if (filePaths.isEmpty()) Text(stringResource(R.string.no_unindexed_files_found), style = MaterialTheme.typography.bodyMedium)
                if (filePaths.isNotEmpty()) {
                    LazyColumn(Modifier.weight(1f, false).fillMaxWidth().heightIn(max = 350.dp)) {
                        item {
                            val desc = if (showHidden) stringResource(R.string.unindexed_files_system_desc) else stringResource(R.string.unindexed_files_user_desc)
                            Text(desc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        groupedFiles.forEach { (directory, files) ->
                            item {
                                Text(if (directory == "Unknown Location") unknownLocation else directory, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                HorizontalDivider()
                            }
                            items(files) { fp -> Text("${File(fp).parentFile?.name ?: "..."}/${File(fp).name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().clickable(onClick = onToggleShowHidden).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showHidden, onCheckedChange = { onToggleShowHidden() })
                    Text(stringResource(R.string.show_hidden_temp_files))
                }
            }
        },
        buttons = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}
