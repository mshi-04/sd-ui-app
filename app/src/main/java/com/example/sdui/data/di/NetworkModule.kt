package com.example.sdui.data.di

import com.example.sdui.BuildConfig
import com.example.sdui.data.remote.SdUiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides
  @Singleton
  fun provideJson(): Json = Json { ignoreUnknownKeys = true }

  @Provides
  @Singleton
  fun provideOkHttpClient(interceptors: Set<@JvmSuppressWildcards Interceptor>): OkHttpClient =
    OkHttpClient.Builder()
      .apply { interceptors.forEach(::addInterceptor) }
      .build()

  @Provides
  @Singleton
  fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
    Retrofit.Builder()
      .baseUrl(BuildConfig.API_BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()

  @Provides
  @Singleton
  fun provideSdUiApi(retrofit: Retrofit): SdUiApi = retrofit.create(SdUiApi::class.java)
}
