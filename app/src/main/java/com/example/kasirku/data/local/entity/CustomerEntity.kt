package com.example.kasirku.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index(value = ["name"])] // Index untuk pencarian/pengurutan nama
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String?, // Nullable karena opsional
    val address: String?      // Nullable karena opsional
)