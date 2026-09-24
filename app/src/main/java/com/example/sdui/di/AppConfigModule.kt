package com.example.sdui.di

import com.example.sdui.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
  @Provides
  @ApiScope
  fun provideApiScope(): String = BuildConfig.API_SCOPE
}
