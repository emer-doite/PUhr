package PUhr.di

import PUhr.core.crypto.ArgonKeyDeriver
import PUhr.core.crypto.KeyDeriver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {
    @Binds
    @Singleton
    abstract fun bindKeyDeriver(impl: ArgonKeyDeriver): KeyDeriver
}
