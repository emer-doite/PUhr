package PUhr.di

import PUhr.data.db.VaultDatabaseProvider
import PUhr.data.repository.VaultRepositoryImpl
import PUhr.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    companion object {

        @Provides
        @Singleton
        fun provideVaultDatabaseProvider(
            provider: VaultDatabaseProvider,
        ): VaultDatabaseProvider = provider
    }
}
