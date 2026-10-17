package com.example.sdui.data.di

import com.example.sdui.data.repository.DefaultUiSchemaRepository
import com.example.sdui.domain.repository.UiSchemaRepository
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
  abstract fun bindUiSchemaRepository(
    implementation: DefaultUiSchemaRepository,
  ): UiSchemaRepository
}
