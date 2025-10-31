package com.taximeter.data.di

import com.taximeter.data.repository.TaximeterRepositoryImpl
import com.taximeter.domain.repository.TaximeterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaximeterRepository(
        impl: TaximeterRepositoryImpl
    ): TaximeterRepository
}