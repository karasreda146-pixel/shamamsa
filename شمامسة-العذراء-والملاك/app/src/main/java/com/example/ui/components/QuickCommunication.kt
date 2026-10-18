package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object QuickCommunication {

    /**
     * Formats an Egyptian phone number for WhatsApp international standard.
     * e.g., "01234567890" becomes "201234567890"
     */
    fun formatPhoneNumberForWhatsApp(phone: String): String {
        val clean = phone.replace(Regex("[^0-9+]"), "")
        return when {
            clean.startsWith("+") -> clean.substring(1)
            clean.startsWith("00") -> clean.substring(2)
            clean.startsWith("0") && clean.length == 11 -> "2" + clean
            clean.length == 10 && clean.startsWith("1") -> "20" + clean
            else -> clean
        }
    }

    /**
     * Opens direct WhatsApp conversation with the deacon or guardian.
     */
    fun openWhatsAppChat(context: Context, phone: String, deaconName: String, isParent: Boolean = false) {
        val formatted = formatPhoneNumberForWhatsApp(phone)
        if (formatted.isBlank()) {
            Toast.makeText(context, "رقم الهاتف غير متاح", Toast.LENGTH_SHORT).show()
            return
        }

        val greeting = if (isParent) {
            "سلام ونعمة لحضرتك، من خدمة شمامسة كنيسة العذراء والملاك ميخائيل - الأخلاص بخصوص الشماس $deaconName."
        } else {
            "سلام ونعمة يا شماس $deaconName، نتمنى لك كل بركة ونعمة ودوام الخدمة المباركة بكنيسة العذراء والملاك ميخائيل بالإخلاص."
        }

        try {
            val encodedText = URLEncoder.encode(greeting, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=$encodedText")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Opens phone dialer directly with the number.
     */
    fun dialPhoneNumber(context: Context, phone: String) {
        val clean = phone.trim()
        if (clean.isBlank()) {
            Toast.makeText(context, "رقم الهاتف غير متاح", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$clean")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر إجراء الاتصال: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
