package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// 1. License Verification & Heartbeat
@JsonClass(generateAdapter = true)
data class VerifyLicenseRequest(
    @Json(name = "serialKey") val serialKey: String,
    @Json(name = "licenseKey") val licenseKey: String = serialKey,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "deviceModel") val deviceModel: String,
    @Json(name = "appVersion") val appVersion: String,
    @Json(name = "merchantId") val merchantId: String
)

@JsonClass(generateAdapter = true)
data class VerifyLicenseResponse(
    @Json(name = "valid") val valid: Boolean,
    @Json(name = "status") val status: String, // e.g. "ACTIVE", "EXPIRED"
    @Json(name = "merchantName") val merchantName: String? = "Park Sathi Merchant",
    @Json(name = "plan") val plan: String? = "MONTHLY_PRO",
    @Json(name = "allowedGates") val allowedGates: Int? = 5,
    @Json(name = "expiresAt") val expiresAt: String? = null,
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
