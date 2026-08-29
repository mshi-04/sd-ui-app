package com.example.sdui.data.di

import com.example.sdui.BuildConfig
import com.example.sdui.di.DevIdentityConfigured
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Module
@InstallIn(SingletonComponent::class)
object DevIdentityModule {
  @Provides
  @DevIdentityConfigured
  fun provideDevIdentityConfigured(): Boolean =
    BuildConfig.DEV_OID.isNotBlank() || BuildConfig.DEV_ROLES.isNotBlank()

  @Provides
  @Singleton
  @IntoSet
  fun provideDevIdentityInterceptor(): Interceptor =
    DevIdentityInterceptor(
      oid = BuildConfig.DEV_OID,
      roles = BuildConfig.DEV_ROLES,
    )
}

private class DevIdentityInterceptor(
  private val oid: String,
  private val roles: String,
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request =
      chain.request().newBuilder()
        .apply {
          if (oid.isNotBlank()) header(OID_HEADER, oid)
          if (roles.isNotBlank()) header(ROLES_HEADER, roles)
        }
        .build()
    return chain.proceed(request)
  }

  private companion object {
    const val OID_HEADER = "X-Dev-Oid"
    const val ROLES_HEADER = "X-Dev-Roles"
  }
}
