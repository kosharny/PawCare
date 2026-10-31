package com.example.pawcare.di

import android.content.Context
import androidx.room.Room
import com.example.pawcare.core.notifications.AndroidReminderScheduler
import com.example.pawcare.core.notifications.ReminderScheduler
import com.example.pawcare.data.local.PawCareDatabase
import com.example.pawcare.data.repository.LocalCareRepository
import com.example.pawcare.domain.repository.CareRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun articles(value: com.example.pawcare.data.repository.BundledArticlesRepository): com.example.pawcare.domain.repository.ArticlesRepository
    @Binds abstract fun repository(value: LocalCareRepository): CareRepository
    @Binds abstract fun scheduler(value: AndroidReminderScheduler): ReminderScheduler
}
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): PawCareDatabase =
        Room.databaseBuilder(context, PawCareDatabase::class.java, "pawcare.db").build()
    @Provides @Singleton fun zones(): com.example.pawcare.core.time.ZoneSource = com.example.pawcare.core.time.ZoneSource { java.time.ZoneId.systemDefault() }
    @Provides @Singleton fun clock(): Clock = Clock.systemUTC()
}
