package com.example.sdui.data.di

import com.example.sdui.data.auth.AccessTokenProvider
import com.example.sdui.data.auth.MsalAuthClient
import com.example.sdui.data.remote.AuthorizationInterceptor
import com.example.sdui.data.repository.DefaultAuthRepository
import com.example.sdui.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
  @Binds
  @Singleton
  abstract fun bindAuthRepository(implementation: DefaultAuthRepository): AuthRepository

  @Binds
  @Singleton
  abstract fun bindAccessTokenProvider(implementation: MsalAuthClient): AccessTokenProvider

  @Binds
  @IntoSet
  abstract fun bindAuthorizationInterceptor(
    implementation: AuthorizationInterceptor,
  ): Interceptor
}
