package com.taximeter.data.di

import com.taximeter.data.strategy.LuggageStrategy
import com.taximeter.domain.strategy.SupplementStrategy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StrategyModule {

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindLuggageStrategy(impl: LuggageStrategy): SupplementStrategy
}