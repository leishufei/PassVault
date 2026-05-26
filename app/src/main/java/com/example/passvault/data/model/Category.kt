package com.example.passvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
