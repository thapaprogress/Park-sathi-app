package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "activation_records")
data class ActivationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val licenseKey: String,
    val planName: String,
    val activatedAtMillis: Long,
    val expiresAtMillis: Long,
    val status: String,
    val deviceHardwareId: String,
    val merchantName: String
)

@Dao
interface ActivationDao {
    @Query("SELECT * FROM activation_records ORDER BY activatedAtMillis DESC")
    fun getAllActivations(): Flow<List<ActivationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivation(record: ActivationRecord)

    @Query("SELECT * FROM activation_records WHERE licenseKey = :key LIMIT 1")
    suspend fun getByKey(key: String): ActivationRecord?

    @Query("UPDATE activation_records SET status = 'EXPIRED' WHERE expiresAtMillis < :nowMillis")
    suspend fun markExpired(nowMillis: Long = System.currentTimeMillis())
}
