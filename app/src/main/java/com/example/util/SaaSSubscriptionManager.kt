package com.example.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.network.ParkSathiNetworkClient
import com.example.network.VerifyLicenseResponse

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
    val merchantName: String = "Park Sathi Merchant"
)

class SaaSSubscriptionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("parksathi_saas_subscription", Context.MODE_PRIVATE)

    companion object {
        const val KEY_PLAN_NAME = "saas_plan_name"
        const val KEY_LICENSE_KEY = "saas_license_key"
        const val KEY_ACTIVATED_AT = "saas_activated_at"
        const val KEY_EXPIRES_AT = "saas_expires_at"
        const val KEY_MERCHANT_NAME = "saas_merchant_name"

        // Demo valid license keys for testing
        val DEMO_KEYS = mapOf(
            "PARKSATHI-TRIAL-14D" to SubscriptionPlan.TRIAL,
            "PARKSATHI-MONTHLY-2026" to SubscriptionPlan.MONTHLY,
            "PARKSATHI-YEARLY-2026" to SubscriptionPlan.YEARLY,
            "UTPALA-TRIAL-14D" to SubscriptionPlan.TRIAL,
            "UTPALA-MONTHLY-2026" to SubscriptionPlan.MONTHLY,
            "UTPALA-YEARLY-2026" to SubscriptionPlan.YEARLY,
            "NEPAL-POS-PRO-30" to SubscriptionPlan.MONTHLY,
            "NEPAL-POS-ENT-365" to SubscriptionPlan.YEARLY
        )
    }

    init {
        // Initialize default trial if never activated before
        if (!prefs.contains(KEY_PLAN_NAME)) {
            val now = System.currentTimeMillis()
            val trialDays = 14
            val expiry = now + (trialDays * 24 * 60 * 60 * 1000L)
            
            prefs.edit()
                .putString(KEY_PLAN_NAME, SubscriptionPlan.TRIAL.name)
                .putString(KEY_LICENSE_KEY, "PARKSATHI-TRIAL-14D")
                .putLong(KEY_ACTIVATED_AT, now)
                .putLong(KEY_EXPIRES_AT, expiry)
                .apply()
        }
    }

    fun getSubscriptionInfo(): SubscriptionInfo {
        val planName = prefs.getString(KEY_PLAN_NAME, SubscriptionPlan.TRIAL.name) ?: SubscriptionPlan.TRIAL.name
        val key = prefs.getString(KEY_LICENSE_KEY, "PARKSATHI-TRIAL-14D") ?: "PARKSATHI-TRIAL-14D"
        val activatedAt = prefs.getLong(KEY_ACTIVATED_AT, System.currentTimeMillis())
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, System.currentTimeMillis() + (14 * 24 * 60 * 60 * 1000L))
        val merchantName = prefs.getString(KEY_MERCHANT_NAME, "Civil Mall Complex Parking") ?: "Civil Mall Complex Parking"

        val now = System.currentTimeMillis()
        val isExpired = now > expiresAt
        
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
            merchantName = merchantName
        )
    }

    suspend fun verifyLicenseOnline(inputKey: String): Pair<Boolean, String> {
        val trimmedKey = inputKey.trim().uppercase(Locale.US)
        val deviceId = ParkSathiNetworkClient.getDeviceId(context)

        val apiResponse = ParkSathiNetworkClient.safeVerifyLicense(
            licenseKey = trimmedKey,
            deviceId = deviceId
        )

        if (apiResponse.valid && apiResponse.status == "ACTIVE") {
            val matchedPlan = DEMO_KEYS[trimmedKey] ?: when {
                trimmedKey.contains("YEAR") || trimmedKey.contains("ENT") -> SubscriptionPlan.YEARLY
                else -> SubscriptionPlan.MONTHLY
            }
            if (apiResponse.merchantName != null) {
                prefs.edit().putString(KEY_MERCHANT_NAME, apiResponse.merchantName).apply()
            }
            return applySubscriptionPlan(matchedPlan, trimmedKey)
        } else {
            return Pair(false, apiResponse.message ?: "License verification failed or license is expired.")
        }
    }

    suspend fun performHeartbeatCheck(): VerifyLicenseResponse {
        val info = getSubscriptionInfo()
        val deviceId = ParkSathiNetworkClient.getDeviceId(context)
        val response = ParkSathiNetworkClient.safeVerifyLicense(
            licenseKey = info.licenseKey,
            deviceId = deviceId
        )
        if (response.valid && response.status == "ACTIVE" && response.merchantName != null) {
            prefs.edit().putString(KEY_MERCHANT_NAME, response.merchantName).apply()
        }
        return response
    }

    fun activateLicenseKey(inputKey: String): Pair<Boolean, String> {
        val trimmedKey = inputKey.trim().uppercase(Locale.US)
        
        // 1. Check mapped demo keys
        val matchedPlan = DEMO_KEYS[trimmedKey]
        if (matchedPlan != null) {
            return applySubscriptionPlan(matchedPlan, trimmedKey)
        }

        // 2. Custom Key Pattern Validation e.g., PARKSATHI-M30-XXXX or UTPALA-M30-XXXX
        return when {
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
                Pair(false, "Invalid License Key format. Try PARKSATHI-MONTHLY-2026 or PARKSATHI-YEARLY-2026")
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

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val expiryStr = sdf.format(Date(newExpiry))

        return Pair(true, "Successfully activated ${plan.displayName}! Valid until $expiryStr")
    }

    fun formattedDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        return sdf.format(Date(timeMillis))
    }
}
