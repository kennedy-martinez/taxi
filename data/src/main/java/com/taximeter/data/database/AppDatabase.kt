package com.taximeter.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PriceConfigEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun priceConfigDao(): PriceConfigDao
}