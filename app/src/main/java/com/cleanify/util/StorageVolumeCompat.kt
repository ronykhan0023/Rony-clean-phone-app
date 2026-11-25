package com.cleanify.util

import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import android.os.storage.StorageVolume
import java.io.File

object StorageVolumeCompat {

    /**
     * Resolves the filesystem root [File] for a [StorageVolume] across all Android versions.
     * [StorageVolume.getDirectory] was added in Android 11 (API 30); on Android 9 (API 28) and
     * Android 10 (API 29), this falls back to [Environment.getExternalStorageDirectory] for the
     * primary volume, `/storage/<uuid>` for removable volumes, or hidden API reflection.
     */
    @SuppressLint("DiscouragedPrivateApi")
    fun resolveDirectory(volume: StorageVolume): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return volume.directory
        }

        if (volume.isPrimary) {
            return Environment.getExternalStorageDirectory()
        }

        volume.uuid?.takeIf { it.isNotBlank() }?.let { uuid ->
            val candidate = File("/storage/$uuid")
            if (candidate.exists()) return candidate
        }

        return try {
            val method = StorageVolume::class.java.getMethod("getPathFile")
            method.invoke(volume) as? File
        } catch (_: Exception) {
            try {
                val method = StorageVolume::class.java.getMethod("getPath")
                (method.invoke(volume) as? String)?.let { File(it) }
            } catch (_: Exception) {
                null
            }
        }
    }
}
