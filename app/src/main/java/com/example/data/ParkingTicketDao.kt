package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingTicketDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTicket(ticket: ParkingTicket)

    @Update
    suspend fun updateTicket(ticket: ParkingTicket)

    @Query("SELECT * FROM parking_tickets WHERE ticketId = :ticketId")
    suspend fun getTicketById(ticketId: String): ParkingTicket?

    @Query("SELECT * FROM parking_tickets WHERE status = 'ACTIVE' AND vehicleNumber LIKE '%' || :query || '%'")
    fun getActiveTicketsByVehicleNumberFlow(query: String): Flow<List<ParkingTicket>>

    @Query("SELECT * FROM parking_tickets WHERE status = 'ACTIVE' AND vehicleNumber LIKE '%' || :query || '%'")
    suspend fun getActiveTicketsByVehicleNumber(query: String): List<ParkingTicket>

    @Query("SELECT * FROM parking_tickets WHERE status = 'ACTIVE'")
    fun getActiveTicketsFlow(): Flow<List<ParkingTicket>>

    @Query("SELECT * FROM parking_tickets ORDER BY checkInTime DESC")
    fun getAllTicketsFlow(): Flow<List<ParkingTicket>>

    @Query("SELECT * FROM parking_tickets WHERE status = 'COMPLETED' ORDER BY checkOutTime DESC")
    fun getCompletedTicketsFlow(): Flow<List<ParkingTicket>>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN totalAmount ELSE 0.0 END), 0.0) as totalRevenue,
            SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as activeCount,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount
        FROM parking_tickets
        WHERE checkInTime >= :startOfDay
    """)
    fun getDailySummaryFlow(startOfDay: Long): Flow<DailySummary>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN totalAmount ELSE 0.0 END), 0.0) as totalRevenue,
            SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as activeCount,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount
        FROM parking_tickets
        WHERE checkInTime >= :startOfDay
    """)
    suspend fun getDailySummary(startOfDay: Long): DailySummary
    @Query("SELECT * FROM parking_tickets WHERE isSyncedWithCloud = 0")
    suspend fun getUnsyncedTickets(): List<ParkingTicket>

    @Query("UPDATE parking_tickets SET isSyncedWithCloud = 1 WHERE ticketId IN (:ticketIds)")
    suspend fun markTicketsSynced(ticketIds: List<String>)

    @Query("SELECT COUNT(*) FROM parking_tickets WHERE isSyncedWithCloud = 0")
    fun getUnsyncedCountFlow(): Flow<Int>
}

data class DailySummary(
    val totalRevenue: Double,
    val activeCount: Int,
    val completedCount: Int
)
