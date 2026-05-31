package com.example.passvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.passvault.data.model.Category
import com.example.passvault.data.model.PasswordEntry

@Database(
    entities = [PasswordEntry::class, Category::class],
    version = 4,
    exportSchema = false
)
abstract class PasswordDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    abstract fun categoryDao(): CategoryDao
}
