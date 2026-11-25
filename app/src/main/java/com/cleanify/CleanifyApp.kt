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

package com.cleanify

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CleanifyApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isConstrained = am?.isLowRamDevice == true || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val memoryCachePercent = if (isConstrained) 0.15 else 0.25
        val diskCacheBytes = if (isConstrained) 150L * 1024 * 1024 else 512L * 1024 * 1024

        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskCacheBytes)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false) // Don't rely on network response headers for caching
            .allowRgb565(true) // Use RGB_565 for better memory usage
            .build()
    }
}
