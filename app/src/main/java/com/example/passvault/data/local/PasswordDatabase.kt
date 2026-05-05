package com.example.passvault.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.passvault.data.model.PasswordEntry

@Database(
    entities = [PasswordEntry::class],
    version = 2,
    exportSchema = false
)
abstract class PasswordDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
}
