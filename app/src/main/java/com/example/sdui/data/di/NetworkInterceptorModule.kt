package com.example.sdui.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkInterceptorModule {
  @Multibinds
  abstract fun interceptors(): Set<Interceptor>
}
