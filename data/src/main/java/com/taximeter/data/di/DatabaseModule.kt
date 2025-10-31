package com.taximeter.data.di

import android.content.Context
import androidx.room.Room
import com.taximeter.data.database.AppDatabase
import com.taximeter.data.database.PriceConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "taximeter_database"
        ).build()
    }

    @Provides
    @Singleton
    fun providePriceConfigDao(database: AppDatabase): PriceConfigDao {
        return database.priceConfigDao()
    }
}