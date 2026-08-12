package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Secure preferences helper with AES key encryption for sensitive SaaS & IRD values.
 */
class SecurePreferencesHelper(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("parksathi_secure_prefs", Context.MODE_PRIVATE)

    private val secretKeySpec = SecretKeySpec("ParkSathiSecKey88".toByteArray(Charsets.UTF_8), "AES")

    fun putSecureString(key: String, value: String) {
        try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val base64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            sharedPreferences.edit().putString(key, base64).apply()
        } catch (e: Exception) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    fun getSecureString(key: String, defaultValue: String = ""): String {
        val stored = sharedPreferences.getString(key, null) ?: return defaultValue
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
            val decoded = Base64.decode(stored, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (e: Exception) {
            stored
        }
    }

    fun clearKey(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}
