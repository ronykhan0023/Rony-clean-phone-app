package com.cleanify.ui.screens.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.data.repository.AppLocale
import com.cleanify.ui.screens.settings.SettingsViewModel
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.isDark
import com.cleanify.util.rememberIsUsingGestureNavigation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@Composable
fun AppearanceSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val currentLocale by viewModel.currentLocale.collectAsStateWithLifecycle()
    val useDynamicColors by viewModel.useDynamicColors.collectAsStateWithLifecycle()
    val accentColorKey by viewModel.accentColorKey.collectAsStateWithLifecycle()
    val immersiveMode by viewModel.immersiveMode.collectAsStateWithLifecycle()
    val hideFromGallery by viewModel.hideFromGallery.collectAsStateWithLifecycle()
    val eyeSaverEnabled by viewModel.eyeSaverEnabled.collectAsStateWithLifecycle()
    val eyeSaverIntensity by viewModel.eyeSaverIntensity.collectAsStateWithLifecycle()
    val supportsDynamicColors = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val isGestureMode = rememberIsUsingGestureNavigation()
    val isDarkActive = currentTheme.isDark

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Theme & Language ──
        FrostedSettingsGroup(titleRes = R.string.theme_section_header) {
            SettingSwitch(
                titleRes = R.string.dark_mode_title,
                descriptionRes = R.string.dark_mode_desc,
                checked = isDarkActive,
                onCheckedChange = { viewModel.setDarkModeEnabled(it) },
                settingId = "dark_mode"
            )
            SettingsPickerItem(
                titleRes = R.string.theme_title,
                description = stringResource(getThemeDescriptionRes(currentTheme)),
                options = AppTheme.entries,
                selectedOption = currentTheme,
                onOptionSelected = { viewModel.setTheme(it) },
                getDisplayName = ::getThemeDisplayName,
                settingId = "theme"
            )
            SettingSwitch(
                titleRes = R.string.eye_saver_title,
                descriptionRes = R.string.eye_saver_desc,
                checked = eyeSaverEnabled,
                onCheckedChange = { viewModel.setEyeSaverEnabled(it) },
                settingId = "eye_saver"
            )
            AnimatedVisibility(visible = eyeSaverEnabled) {
                val intensityPercent = ((eyeSaverIntensity - 0.10f) / 0.60f * 100f).roundToInt().coerceIn(0, 100)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.eye_saver_intensity_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.eye_saver_intensity_desc, intensityPercent),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Slider(
                            value = eyeSaverIntensity,
                            onValueChange = { viewModel.setEyeSaverIntensity(it) },
                            valueRange = 0.10f..0.70f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("eye_saver_intensity_slider")
                        )
                    }
                }
            }
            SettingsPickerItem(
                titleRes = R.string.language_title,
                description = stringResource(R.string.language_desc),
                options = AppLocale.entries,
                selectedOption = currentLocale,
                onOptionSelected = { viewModel.setAppLocale(it) },
                getDisplayName = ::getAppLocaleDisplayName,
                settingId = "language"
            )
            SettingSwitch(
                R.string.dynamic_colors_title,
                if (supportsDynamicColors) stringResource(R.string.dynamic_colors_desc) else stringResource(R.string.dynamic_colors_req_android_12),
                useDynamicColors,
                { viewModel.setUseDynamicColors(it) },
                supportsDynamicColors,
                settingId = "dynamic_colors"
            )
            AnimatedVisibility(visible = !useDynamicColors || !supportsDynamicColors) {
                AccentColorSetting(currentAccentKey = accentColorKey, onClick = viewModel::showAccentColorDialog, settingId = "accent_color")
            }
        }

        // ── Folder Behavior ──
        val folderSelectionMode by viewModel.folderSelectionMode.collectAsStateWithLifecycle()
        val showFavoritesInSetup by viewModel.showFavoritesInSetup.collectAsStateWithLifecycle()
        val searchAutofocusEnabled by viewModel.searchAutofocusEnabled.collectAsStateWithLifecycle()

        FrostedSettingsGroup(titleRes = R.string.folder_behavior_section_header) {
            SettingsPickerItem(
                R.string.folder_selection_mode_title,
                getFolderSelectionModeDescriptionRes(folderSelectionMode),
                com.cleanify.data.repository.FolderSelectionMode.entries,
                folderSelectionMode,
                { viewModel.setFolderSelectionMode(it) },
                ::getFolderSelectionModeDisplayName,
                settingId = "folder_selection_mode"
            )
            SettingSwitch(
                R.string.show_favorites_setup_title,
                R.string.show_favorites_setup_desc,
                showFavoritesInSetup,
                { viewModel.setShowFavoritesInSetup(it) },
                settingId = "show_favorites_setup"
            )
            SettingSwitch(
                R.string.search_autofocus_title,
                R.string.search_autofocus_desc,
                searchAutofocusEnabled,
                { viewModel.setSearchAutofocusEnabled(it) },
                settingId = "search_autofocus"
            )
        }

        // ── Accessibility ──
        FrostedSettingsGroup(titleRes = R.string.accessibility_section_header) {
            SettingSwitch(
                titleRes = R.string.immersive_mode_title,
                description = stringResource(R.string.immersive_mode_desc),
                checked = immersiveMode,
                onCheckedChange = { viewModel.setImmersiveMode(it) },
                settingId = "immersive_mode"
            )
            SettingSwitch(
                R.string.hide_from_gallery_title,
                R.string.hide_from_gallery_desc,
                hideFromGallery,
                { viewModel.setHideFromGallery(it) },
                settingId = "hide_from_gallery"
            )
        }

        Spacer(Modifier.height(if (isGestureMode) 0.dp else 32.dp))
    }
}
