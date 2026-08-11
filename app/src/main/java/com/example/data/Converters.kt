package com.example.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromVehicleType(value: VehicleType): String {
        return value.name
    }

    @TypeConverter
    fun toVehicleType(value: String): VehicleType {
        return VehicleType.valueOf(value)
    }

    @TypeConverter
    fun fromTicketStatus(value: TicketStatus): String {
        return value.name
    }

    @TypeConverter
    fun toTicketStatus(value: String): TicketStatus {
        return TicketStatus.valueOf(value)
    }
}
