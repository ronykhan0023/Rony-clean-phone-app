package com.cleanify.util

import android.content.Context
import android.net.Uri
import java.security.MessageDigest

object NativeHasher {
    private const val BUFFER_SIZE = 64 * 1024

    fun hashBytes(data: ByteArray): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            bytesToHex(digest.digest(data))
        } catch (e: Exception) {
            null
        }
    }

    fun hashFile(context: Context, uri: Uri): String? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            stream.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
                bytesToHex(digest.digest())
            }
        } catch (e: Exception) {
            null
        }
    }

    fun hashFilePartial(context: Context, uri: Uri, bytesToRead: Int): String? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            stream.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(minOf(BUFFER_SIZE, bytesToRead.coerceAtLeast(1)))
                var remaining = bytesToRead
                while (remaining > 0) {
                    val toRead = minOf(buffer.size, remaining)
                    val read = input.read(buffer, 0, toRead)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                    remaining -= read
                }
                bytesToHex(digest.digest())
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef"
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
