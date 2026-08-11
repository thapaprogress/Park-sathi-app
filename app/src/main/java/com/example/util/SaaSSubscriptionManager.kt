package com.example.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.network.ParkSathiNetworkClient
import com.example.network.VerifyLicenseResponse
import com.example.data.ActivationRecord
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class SubscriptionPlan(val displayName: String, val priceNpr: Int, val durationDays: Int) {
    TRIAL("14-Day Free Trial", 0, 14),
    MONTHLY("Monthly Pro SaaS", 1500, 30),
    YEARLY("Yearly Enterprise SaaS", 15000, 365),
    EXPIRED("Expired Plan", 0, 0)
}

data class SubscriptionInfo(
    val plan: SubscriptionPlan,
    val licenseKey: String,
    val activatedAtMillis: Long,
    val expiresAtMillis: Long,
    val isExpired: Boolean,
    val daysRemaining: Int,
    val maxGatesAllowed: Int = 10,
    val merchantName: String = "Park Sathi Merchant",
    val isGraceExpired: Boolean = false,
    val daysOffline: Int = 0,
    val maxOfflineGraceDays: Int = 7,
    val graceDaysRemaining: Int = 7,
    val deviceIdLocked: String? = null
)

class SaaSSubscriptionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("parksathi_saas_subscription", Context.MODE_PRIVATE)

    companion object {
        const val KEY_PLAN_NAME = "saas_plan_name"
        const val KEY_LICENSE_KEY = "saas_license_key"
        const val KEY_ACTIVATED_AT = "saas_activated_at"
        const val KEY_EXPIRES_AT = "saas_expires_at"
        const val KEY_MERCHANT_NAME = "saas_merchant_name"
        const val KEY_LAST_ONLINE_VERIFIED_AT = "saas_last_online_verified_at"
        const val KEY_DEVICE_ID_LOCKED = "saas_device_id_locked"
        const val KEY_MAX_OFFLINE_GRACE_DAYS = "saas_max_offline_grace_days"
    }

    init {
        // Initialize default 14-day free trial on first install
        if (!prefs.contains(KEY_PLAN_NAME)) {
            val now = System.currentTimeMillis()
            val trialDays = 14
            val expiry = now + (trialDays * 24 * 60 * 60 * 1000L)
            
            prefs.edit()
                .putString(KEY_PLAN_NAME, SubscriptionPlan.TRIAL.name)
                .putString(KEY_LICENSE_KEY, "PARKSATHI-TRIAL-14D")
                .putLong(KEY_ACTIVATED_AT, now)
                .putLong(KEY_EXPIRES_AT, expiry)
                .putLong(KEY_LAST_ONLINE_VERIFIED_AT, now)
                .putInt(KEY_MAX_OFFLINE_GRACE_DAYS, 7)
                .apply()

            recordActivationInRoom("PARKSATHI-TRIAL-14D", SubscriptionPlan.TRIAL, now, expiry)
        }
    }

    fun getSubscriptionInfo(): SubscriptionInfo {
        val planName = prefs.getString(KEY_PLAN_NAME, SubscriptionPlan.TRIAL.name) ?: SubscriptionPlan.TRIAL.name
        val key = prefs.getString(KEY_LICENSE_KEY, "PARKSATHI-TRIAL-14D") ?: "PARKSATHI-TRIAL-14D"
        val activatedAt = prefs.getLong(KEY_ACTIVATED_AT, System.currentTimeMillis())
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000L))
        val merchantName = prefs.getString(KEY_MERCHANT_NAME, "Civil Mall Complex Parking") ?: "Civil Mall Complex Parking"
        val lastVerifiedAt = prefs.getLong(KEY_LAST_ONLINE_VERIFIED_AT, activatedAt)
        val maxGraceDays = prefs.getInt(KEY_MAX_OFFLINE_GRACE_DAYS, 7)
        val lockedDevice = prefs.getString(KEY_DEVICE_ID_LOCKED, null)

        val now = System.currentTimeMillis()
        val isExpiredByTime = now > expiresAt

        val diffOfflineMillis = if (now >= lastVerifiedAt) now - lastVerifiedAt else 0L
        val daysOffline = (diffOfflineMillis / (1000 * 60 * 60 * 24)).toInt()
        val isGraceExpired = daysOffline > maxGraceDays
        val graceDaysRemaining = if (maxGraceDays >= daysOffline) maxGraceDays - daysOffline else 0

        val isExpired = isExpiredByTime || isGraceExpired

        val storedPlan = try {
            SubscriptionPlan.valueOf(planName)
        } catch (e: Exception) {
            SubscriptionPlan.TRIAL
        }

        val effectivePlan = if (isExpired) SubscriptionPlan.EXPIRED else storedPlan

        val diffMillis = expiresAt - now
        val daysRemaining = if (diffMillis > 0) {
            (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        } else {
            0
        }

        return SubscriptionInfo(
            plan = effectivePlan,
            licenseKey = key,
            activatedAtMillis = activatedAt,
            expiresAtMillis = expiresAt,
            isExpired = isExpired,
            daysRemaining = daysRemaining,
            maxGatesAllowed = if (effectivePlan == SubscriptionPlan.YEARLY) 25 else 5,
            merchantName = merchantName,
            isGraceExpired = isGraceExpired,
            daysOffline = daysOffline,
            maxOfflineGraceDays = maxGraceDays,
            graceDaysRemaining = graceDaysRemaining,
            deviceIdLocked = lockedDevice
        )
    }

    suspend fun verifyLicenseOnline(inputKey: String): Pair<Boolean, String> {
        val trimmedKey = inputKey.trim().uppercase(Locale.US)
        val deviceId = ParkSathiNetworkClient.getDeviceId(context)

        val info = getSubscriptionInfo()

        val apiResponse = ParkSathiNetworkClient.safeVerifyLicense(
            licenseKey = trimmedKey,
            deviceId = deviceId,
            daysOffline = info.daysOffline
        )

        val now = System.currentTimeMillis()

        return when (apiResponse.status) {
            "ACTIVE" -> {
                prefs.edit()
                    .putLong(KEY_LAST_ONLINE_VERIFIED_AT, now)
                    .putInt(KEY_MAX_OFFLINE_GRACE_DAYS, apiResponse.maxOfflineGraceDays ?: 7)
                    .apply()
                if (apiResponse.merchantName != null) {
                    prefs.edit().putString(KEY_MERCHANT_NAME, apiResponse.merchantName).apply()
                }
                if (apiResponse.deviceIdLocked != null) {
                    prefs.edit().putString(KEY_DEVICE_ID_LOCKED, apiResponse.deviceIdLocked).apply()
                }

                val plan = when {
                    trimmedKey.contains("YEAR") || trimmedKey.contains("ENT") || apiResponse.plan?.contains("YEAR") == true -> SubscriptionPlan.YEARLY
                    trimmedKey.contains("TRIAL") -> SubscriptionPlan.TRIAL
                    else -> SubscriptionPlan.MONTHLY
                }

                applySubscriptionPlan(plan, trimmedKey)
            }
            "DEVICE_MISMATCH" -> {
                Pair(false, "⛔ License Key is locked to another device (${apiResponse.deviceIdLocked ?: "Registered Device"}). Single-device license policy.")
            }
            "CLOCK_ROLLBACK_DETECTED" -> {
                Pair(false, "⏰ System clock rollback detected! Please update device date and time settings.")
            }
            "REAUTH_REQUIRED" -> {
                Pair(false, "🔑 Re-authentication required by SaaS Admin. Please enter key again.")
            }
            "EXPIRED_OR_INVALID", "EXPIRED" -> {
                Pair(false, apiResponse.message ?: "License key is expired or invalid.")
            }
            else -> {
                if (apiResponse.valid) {
                    prefs.edit().putLong(KEY_LAST_ONLINE_VERIFIED_AT, now).apply()
                    applySubscriptionPlan(SubscriptionPlan.MONTHLY, trimmedKey)
                } else {
                    Pair(false, apiResponse.message ?: "License verification failed.")
                }
            }
        }
    }

    suspend fun performHeartbeatCheck(): VerifyLicenseResponse {
        val info = getSubscriptionInfo()
        val deviceId = ParkSathiNetworkClient.getDeviceId(context)
        val response = ParkSathiNetworkClient.safeVerifyLicense(
            licenseKey = info.licenseKey,
            deviceId = deviceId,
            daysOffline = info.daysOffline
        )
        if (response.valid && response.status == "ACTIVE") {
            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_LAST_ONLINE_VERIFIED_AT, now)
                .putInt(KEY_MAX_OFFLINE_GRACE_DAYS, response.maxOfflineGraceDays ?: 7)
                .apply()
            if (response.merchantName != null) {
                prefs.edit().putString(KEY_MERCHANT_NAME, response.merchantName).apply()
            }
        }
        return response
    }

    fun activateLicenseKey(inputKey: String): Pair<Boolean, String> {
        val trimmedKey = inputKey.trim().uppercase(Locale.US)
        
        return when {
            trimmedKey == "PARKSATHI-TRIAL-14D" || trimmedKey == "UTPALA-TRIAL-14D" -> {
                applySubscriptionPlan(SubscriptionPlan.TRIAL, trimmedKey)
            }
            trimmedKey.startsWith("PARKSATHI-M") || trimmedKey.startsWith("UTPALA-M") || trimmedKey.contains("MONTH") -> {
                applySubscriptionPlan(SubscriptionPlan.MONTHLY, trimmedKey)
            }
            trimmedKey.startsWith("PARKSATHI-Y") || trimmedKey.startsWith("UTPALA-Y") || trimmedKey.contains("YEAR") -> {
                applySubscriptionPlan(SubscriptionPlan.YEARLY, trimmedKey)
            }
            trimmedKey.length >= 8 -> {
                applySubscriptionPlan(SubscriptionPlan.MONTHLY, trimmedKey)
            }
            else -> {
                Pair(false, "Invalid License Key format. Request a valid key via WhatsApp or Website.")
            }
        }
    }

    fun applySubscriptionPlan(plan: SubscriptionPlan, licenseKey: String): Pair<Boolean, String> {
        val now = System.currentTimeMillis()
        val currentExpiry = prefs.getLong(KEY_EXPIRES_AT, now)
        val baseTime = if (currentExpiry > now) currentExpiry else now

        val durationMillis = plan.durationDays * 24 * 60 * 60 * 1000L
        val newExpiry = baseTime + durationMillis

        prefs.edit()
            .putString(KEY_PLAN_NAME, plan.name)
            .putString(KEY_LICENSE_KEY, licenseKey)
            .putLong(KEY_ACTIVATED_AT, now)
            .putLong(KEY_EXPIRES_AT, newExpiry)
            .apply()

        recordActivationInRoom(licenseKey, plan, now, newExpiry)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val expiryStr = sdf.format(Date(newExpiry))

        return Pair(true, "Successfully activated ${plan.displayName}! Valid until $expiryStr")
    }

    private fun recordActivationInRoom(licenseKey: String, plan: SubscriptionPlan, activatedAt: Long, expiresAt: Long) {
        val merchantName = prefs.getString(KEY_MERCHANT_NAME, "Civil Mall Complex Parking") ?: "Civil Mall Complex Parking"
        val deviceId = ParkSathiNetworkClient.getDeviceId(context)
        val record = ActivationRecord(
            licenseKey = licenseKey,
            planName = plan.displayName,
            activatedAtMillis = activatedAt,
            expiresAtMillis = expiresAt,
            status = if (expiresAt > System.currentTimeMillis()) "ACTIVE" else "EXPIRED",
            deviceHardwareId = deviceId,
            merchantName = merchantName
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(context).activationDao().insertActivation(record)
            } catch (e: Exception) {
                // handle safely
            }
        }
    }

    fun formattedDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        return sdf.format(Date(timeMillis))
    }
}
