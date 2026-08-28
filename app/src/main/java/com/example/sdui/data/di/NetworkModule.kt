package com.example.sdui.data.di

import com.example.sdui.data.remote.SdUiApi
import com.example.sdui.di.ApiBaseUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
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
  fun provideRetrofit(
    json: Json,
    @ApiBaseUrl baseUrl: String,
  ): Retrofit =
    Retrofit.Builder()
      .baseUrl(baseUrl)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()

  @Provides
  @Singleton
  fun provideSdUiApi(retrofit: Retrofit): SdUiApi = retrofit.create(SdUiApi::class.java)
}
