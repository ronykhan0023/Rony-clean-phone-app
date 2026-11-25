package com.cleanify.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.cleanify.R

object WhatsAppLauncher {

    fun sanitizeDigits(number: String): String = number.filter { it.isDigit() }

    fun openWhatsAppChat(context: Context, rawNumber: String): Boolean {
        val digits = sanitizeDigits(rawNumber)
        if (digits.isEmpty()) {
            Toast.makeText(context, R.string.whatsapp_invalid_number, Toast.LENGTH_SHORT).show()
            return false
        }

        val uri = Uri.parse("https://wa.me/$digits")
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b", null)
        for (pkg in packages) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    if (pkg != null) setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Try next target
            }
        }

        Toast.makeText(context, R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show()
        return false
    }
}
