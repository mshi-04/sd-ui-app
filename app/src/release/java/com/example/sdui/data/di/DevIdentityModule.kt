package com.example.sdui.data.di

import com.example.sdui.di.DevIdentityConfigured
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DevIdentityModule {
  @Provides
  @DevIdentityConfigured
  fun provideDevIdentityConfigured(): Boolean = false
}
