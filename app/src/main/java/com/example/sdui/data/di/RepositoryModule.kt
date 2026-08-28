package com.example.sdui.data.di

import com.example.sdui.data.repository.DefaultServerStatusRepository
import com.example.sdui.domain.repository.ServerStatusRepository
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
  abstract fun bindServerStatusRepository(
    implementation: DefaultServerStatusRepository,
  ): ServerStatusRepository
}
