package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// 1. License Verification & Heartbeat
@JsonClass(generateAdapter = true)
data class VerifyLicenseRequest(
    @Json(name = "license_key") val licenseKey: String,
    @Json(name = "serialKey") val serialKey: String = licenseKey,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "app_version") val appVersion: String = "v3.2.1-IRD",
    @Json(name = "client_timestamp_ms") val clientTimestampMs: Long = System.currentTimeMillis(),
    @Json(name = "days_offline") val daysOffline: Int = 0,
    @Json(name = "deviceModel") val deviceModel: String = "SUNMI V2/V3 POS",
    @Json(name = "merchantId") val merchantId: String = "PRAJNA-WORLD-01"
)

@JsonClass(generateAdapter = true)
data class VerifyLicenseResponse(
    @Json(name = "status") val status: String, // ACTIVE | EXPIRED_OR_INVALID | DEVICE_MISMATCH | CLOCK_ROLLBACK_DETECTED | REAUTH_REQUIRED
    @Json(name = "valid") val valid: Boolean = false,
    @Json(name = "device_id_locked") val deviceIdLocked: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "max_offline_grace_days") val maxOfflineGraceDays: Int? = 7,
    @Json(name = "api_base_url") val apiBaseUrl: String? = null,
    @Json(name = "merchantName") val merchantName: String? = "Park Sathi Merchant",
    @Json(name = "plan") val plan: String? = "MONTHLY_PRO",
    @Json(name = "allowedGates") val allowedGates: Int? = 5,
    @Json(name = "supportPhone") val supportPhone: String? = "+977-9765985999",
    @Json(name = "supportAddress") val supportAddress: String? = "Samakhushi Chowk, Kathmandu",
    @Json(name = "supportWebsite") val supportWebsite: String? = "Prajnaworld.com",
    @Json(name = "message") val message: String? = null
)

// 2. Fonepay Payment Integration
@JsonClass(generateAdapter = true)
data class FonepayInitiateRequest(
    @Json(name = "merchantId") val merchantId: String,
    @Json(name = "amountNpr") val amountNpr: Double,
    @Json(name = "ticketId") val ticketId: String,
    @Json(name = "vehicleNumber") val vehicleNumber: String
)

@JsonClass(generateAdapter = true)
data class FonepayInitiateResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "qrPayload") val qrPayload: String,
    @Json(name = "traceId") val traceId: String,
    @Json(name = "status") val status: String // e.g. "PENDING"
)

@JsonClass(generateAdapter = true)
data class FonepayStatusResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "status") val status: String, // "PENDING", "SUCCESS", "FAILED"
    @Json(name = "traceId") val traceId: String,
    @Json(name = "transactionId") val transactionId: String? = null,
    @Json(name = "message") val message: String? = null
)

// 3. IRD CBMS Fiscal Tax Sync
@JsonClass(generateAdapter = true)
data class IrdCbmsSyncRequest(
    @Json(name = "fiscalYear") val fiscalYear: String,
    @Json(name = "billNo") val billNo: String,
    @Json(name = "customerPan") val customerPan: String,
    @Json(name = "totalAmountNpr") val totalAmountNpr: Double,
    @Json(name = "taxableAmountNpr") val taxableAmountNpr: Double,
    @Json(name = "vatAmountNpr") val vatAmountNpr: Double,
    @Json(name = "paymentMethod") val paymentMethod: String,
    @Json(name = "deviceId") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class IrdCbmsSyncResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "cbmsVerificationUrl") val cbmsVerificationUrl: String? = null,
    @Json(name = "ackCode") val ackCode: String? = null
)

// 4. SUNMI MDM & OTA Tariff Config
@JsonClass(generateAdapter = true)
data class VehicleTariffDetail(
    @Json(name = "hourlyRateNpr") val hourlyRateNpr: Double = 20.0,
    @Json(name = "dailyCapNpr") val dailyCapNpr: Double = 150.0
)

@JsonClass(generateAdapter = true)
data class TariffMap(
    @Json(name = "BIKE") val bike: VehicleTariffDetail? = VehicleTariffDetail(20.0, 150.0),
    @Json(name = "CAR") val car: VehicleTariffDetail? = VehicleTariffDetail(50.0, 500.0),
    @Json(name = "TRUCK") val truck: VehicleTariffDetail? = VehicleTariffDetail(100.0, 1000.0)
)

@JsonClass(generateAdapter = true)
data class MdmConfigResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "freeMinutes") val freeMinutes: Int? = 10,
    @Json(name = "gracePeriodMinutes") val gracePeriodMinutes: Int? = 5,
    @Json(name = "graceMinutes") val graceMinutes: Int? = 5,
    @Json(name = "bikeRate") val bikeRate: Double? = 20.0,
    @Json(name = "carRate") val carRate: Double? = 50.0,
    @Json(name = "truckRate") val truckRate: Double? = 100.0,
    @Json(name = "dailyMaxCap") val dailyMaxCap: Double? = 300.0,
    @Json(name = "gateBarrierDurationSec") val gateBarrierDurationSec: Int? = 5,
    @Json(name = "tariffs") val tariffs: TariffMap? = null,
    @Json(name = "latestAppVersion") val latestAppVersion: String? = "v3.2.1-IRD",
    @Json(name = "updateUrl") val updateUrl: String? = null
)
