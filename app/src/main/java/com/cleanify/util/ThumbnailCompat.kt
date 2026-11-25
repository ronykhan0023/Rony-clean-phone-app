package com.cleanify.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size

object ThumbnailCompat {

    /**
     * Loads a scaled thumbnail [Bitmap] for [uri] across all supported Android versions.
     * Uses [android.content.ContentResolver.loadThumbnail] on Android 10+ (API 29+), and falls
     * back to downsampled [BitmapFactory] / [MediaMetadataRetriever] / legacy MediaStore
     * thumbnails on Android 9 (API 28).
     */
    fun loadThumbnail(
        context: Context,
        uri: Uri,
        sizePx: Int,
        isVideo: Boolean = false,
        filePath: String? = null,
        preferredConfig: Bitmap.Config = Bitmap.Config.RGB_565
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
        }

        if (isVideo) {
            return loadVideoThumbnailLegacy(context, uri, sizePx, filePath)
        }

        return loadImageThumbnailLegacy(context, uri, sizePx, filePath, preferredConfig)
            ?: loadMediaStoreThumbnailLegacy(context, uri, isVideo = false)
    }

    private fun loadImageThumbnailLegacy(
        context: Context,
        uri: Uri,
        sizePx: Int,
        filePath: String?,
        preferredConfig: Bitmap.Config
    ): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (!filePath.isNullOrBlank()) {
                BitmapFactory.decodeFile(filePath, boundsOptions)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }
            }

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return null
            }

            val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, sizePx, sizePx)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = preferredConfig
            }

            if (!filePath.isNullOrBlank()) {
                BitmapFactory.decodeFile(filePath, decodeOptions)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadVideoThumbnailLegacy(
        context: Context,
        uri: Uri,
        sizePx: Int,
        filePath: String?
    ): Bitmap? {
        loadMediaStoreThumbnailLegacy(context, uri, isVideo = true)?.let { return it }

        val retriever = MediaMetadataRetriever()
        return try {
            if (!filePath.isNullOrBlank()) {
                retriever.setDataSource(filePath)
            } else {
                retriever.setDataSource(context, uri)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    0L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    sizePx,
                    sizePx
                )
            } else {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadMediaStoreThumbnailLegacy(
        context: Context,
        uri: Uri,
        isVideo: Boolean
    ): Bitmap? {
        if (uri.scheme != "content") return null
        return try {
            val id = ContentUris.parseId(uri)
            if (isVideo) {
                MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver,
                    id,
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    null
                )
            } else {
                MediaStore.Images.Thumbnails.getThumbnail(
                    context.contentResolver,
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
