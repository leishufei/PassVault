package com.example.passwordmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.passwordmanager.data.model.PasswordEntry

@Database(
    entities = [PasswordEntry::class],
    version = 1,
    exportSchema = false
)
abstract class PasswordDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
}
