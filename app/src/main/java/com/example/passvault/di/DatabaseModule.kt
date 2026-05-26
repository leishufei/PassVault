package com.example.passvault.di

import android.content.Context
import androidx.room.Room
import com.example.passvault.data.local.CategoryDao
import com.example.passvault.data.local.PasswordDao
import com.example.passvault.data.local.PasswordDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePasswordDatabase(
        @ApplicationContext context: Context
    ): PasswordDatabase {
        return Room.databaseBuilder(
            context,
            PasswordDatabase::class.java,
            "password_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePasswordDao(database: PasswordDatabase): PasswordDao {
        return database.passwordDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: PasswordDatabase): CategoryDao {
        return database.categoryDao()
    }
}
