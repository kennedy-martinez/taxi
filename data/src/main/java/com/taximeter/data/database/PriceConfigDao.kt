package com.taximeter.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: PriceConfigEntity)

    @Query("SELECT * FROM price_config WHERE id = 1")
    fun getConfigFlow(): Flow<PriceConfigEntity?>

    @Query("SELECT COUNT(*) FROM price_config")
    suspend fun getConfigCount(): Int
}