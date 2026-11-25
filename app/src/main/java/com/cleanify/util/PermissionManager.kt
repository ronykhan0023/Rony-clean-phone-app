/*
 * Cleanify
 * Copyright (c) 2025 LoopOtto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.cleanify.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object PermissionManager {

    val LEGACY_STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Checks if the app has full file access for the current Android version:
     * - Android 11+ (API 30+): MANAGE_EXTERNAL_STORAGE
     * - Android 9–10 (API 28–29): READ_EXTERNAL_STORAGE & WRITE_EXTERNAL_STORAGE
     */
    fun hasAllFilesAccess(context: Context? = null): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else if (context != null) {
            LEGACY_STORAGE_PERMISSIONS.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            false
        }
    }

    /**
     * Creates an intent to open the appropriate storage permission settings screen.
     * - Android 11+ (API 30+): All Files Access settings page
     * - Android 9–10 (API 28–29): App Details settings page
     */
    fun createAllFilesAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
    }

    /**
     * Registers an activity result launcher for the All Files Access permission.
     * Call this in onCreate() of your activity.
     */
    fun registerAllFilesAccessLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            val granted = hasAllFilesAccess(activity)
            onResult(granted)
        }
    }

    /**
     * Launches the All Files Access permission request.
     */
    fun requestAllFilesAccess(
        context: Context,
        launcher: ActivityResultLauncher<Intent>
    ) {
        val intent = createAllFilesAccessIntent(context)
        launcher.launch(intent)
    }
}