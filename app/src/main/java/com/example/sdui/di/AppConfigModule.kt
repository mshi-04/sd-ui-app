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
  @ApiBaseUrl
  fun provideApiBaseUrl(): String = BuildConfig.API_BASE_URL
}
