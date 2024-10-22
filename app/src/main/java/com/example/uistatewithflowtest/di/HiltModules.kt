package com.example.uistatewithflowtest.di

import com.example.uistatewithflowtest.repository.ManualReactionPushDataSource
import com.example.uistatewithflowtest.repository.RawMessageRepository
import com.example.uistatewithflowtest.repository.ReactionPullDataSource
import com.example.uistatewithflowtest.repository.ReactionPushDataSource
import com.example.uistatewithflowtest.repository.ScrapRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltModule {

    @Provides
    @Singleton
    fun provideRawMessageRepository(): RawMessageRepository {
        return RawMessageRepository(10000, 500)
    }

    @Provides
    fun provideReactionPushDataSource(manualReactionPushDataSource: ManualReactionPushDataSource): ReactionPushDataSource {
        return manualReactionPushDataSource
    }

    @Provides
    fun provideReactionPullDataSource(): ReactionPullDataSource {
        return ReactionPullDataSource(1500)
    }

    @Provides
    fun providerScrapRepository(): ScrapRepository {
        return ScrapRepository(1000)
    }
}