package com.example.util

import com.example.data.VehicleType
import kotlin.math.ceil

data class NepalTariffConfig(
    val freeMinutes: Int = 15,         // Free first 15 mins parking
    val gracePeriodMinutes: Int = 5,   // 5 mins grace period after checkout
    val dailyMaxCap: Double = 300.0,   // Daily fee cap in NPR
    val isVatInclusive: Boolean = true, // 13% VAT standard in Nepal
    val vatRatePercent: Double = 13.0
) {
    fun calculateParkingFee(
        checkInTimeMillis: Long,
        checkOutTimeMillis: Long,
        hourlyRate: Double,
        hasCafeSeal: Boolean = false
    ): TariffCalculationResult {
        if (hasCafeSeal) {
            return TariffCalculationResult(
                grossAmount = 0.0,
                discountAmount = 0.0,
                netAmount = 0.0,
                vatAmount = 0.0,
                finalTotal = 0.0,
                billableHours = 0.0,
                durationMinutes = ((checkOutTimeMillis - checkInTimeMillis) / 60000).toInt(),
                isFreeTierApplied = true,
                freeMinutesApplied = freeMinutes
            )
        }

        val durationMillis = (checkOutTimeMillis - checkInTimeMillis).coerceAtLeast(0)
        val totalMinutes = (durationMillis / (1000 * 60)).toInt()

        // Check if within free first X minutes
        if (totalMinutes <= freeMinutes) {
            return TariffCalculationResult(
                grossAmount = 0.0,
                discountAmount = 0.0,
                netAmount = 0.0,
                vatAmount = 0.0,
                finalTotal = 0.0,
                billableHours = 0.0,
                durationMinutes = totalMinutes,
                isFreeTierApplied = true,
                freeMinutesApplied = freeMinutes
            )
        }

        // Billable duration after free minutes
        val billableMinutes = totalMinutes - freeMinutes
        // Round up to full billable hours
        val billableHours = ceil(billableMinutes.toDouble() / 60.0).coerceAtLeast(1.0)
        
        var calculatedTotal = billableHours * hourlyRate

        // Apply daily cap if exceeded
        var discountAmount = 0.0
        if (calculatedTotal > dailyMaxCap && dailyMaxCap > 0) {
            discountAmount = calculatedTotal - dailyMaxCap
            calculatedTotal = dailyMaxCap
        }

        // VAT calculation (13% IRD Nepal standard)
        val netAmount: Double
        val vatAmount: Double

        if (isVatInclusive) {
            // Gross includes VAT: Net = Total / 1.13, VAT = Total - Net
            netAmount = calculatedTotal / (1.0 + (vatRatePercent / 100.0))
            vatAmount = calculatedTotal - netAmount
        } else {
            // VAT added on top
            netAmount = calculatedTotal
            vatAmount = calculatedTotal * (vatRatePercent / 100.0)
            calculatedTotal += vatAmount
        }

        return TariffCalculationResult(
            grossAmount = billableHours * hourlyRate,
            discountAmount = discountAmount,
            netAmount = roundTwoDecimals(netAmount),
            vatAmount = roundTwoDecimals(vatAmount),
            finalTotal = roundTwoDecimals(calculatedTotal),
            billableHours = billableHours,
            durationMinutes = totalMinutes,
            isFreeTierApplied = false,
            freeMinutesApplied = freeMinutes
        )
    }

    private fun roundTwoDecimals(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }
}

data class TariffCalculationResult(
    val grossAmount: Double,
    val discountAmount: Double,
    val netAmount: Double,
    val vatAmount: Double,
    val finalTotal: Double,
    val billableHours: Double,
    val durationMinutes: Int,
    val isFreeTierApplied: Boolean,
    val freeMinutesApplied: Int
)
