package com.example.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ParkSathiApiService {

    // 1. SaaS License Verification & Heartbeat
    @POST("api/v1/license/verify")
    suspend fun verifyLicense(
        @Body request: VerifyLicenseRequest
    ): Response<VerifyLicenseResponse>

    // 2. Fonepay Payment Initiate
    @POST("api/v1/payments/fonepay/initiate")
    suspend fun initiateFonepayPayment(
        @Body request: FonepayInitiateRequest
    ): Response<FonepayInitiateResponse>

    // 2b. Fonepay Status Polling
    @GET("api/v1/payments/fonepay/status")
    suspend fun checkFonepayStatus(
        @Query("traceId") traceId: String
    ): Response<FonepayStatusResponse>

    // 3. IRD CBMS Fiscal Sync
    @POST("api/v1/ird/cbms/sync")
    suspend fun syncIrdCbmsInvoice(
        @Body request: IrdCbmsSyncRequest
    ): Response<IrdCbmsSyncResponse>

    // 4. SUNMI MDM Configuration & Tariff Sync
    @GET("api/v1/mdm/config")
    suspend fun getMdmConfig(
        @Query("deviceId") deviceId: String
    ): Response<MdmConfigResponse>
}
