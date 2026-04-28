package com.example.kasirku.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String, // Nama Outlet / User
    val email: String,

    // --- FIELD BARU ---
    val phoneNumber: String? = null,
    val address: String? = null,
    val logo : String? = null,
    val footerNote: String? = null, // Optional (Terima Kasih, dll)

    @ColumnInfo(name = "password_hash")
    val passHash: String
)