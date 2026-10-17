package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReminderModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val type: String, // 'bill', 'meter', 'custom'
    val dayOfMonth: Int, // 1 to 28
    val hour: Int, // 0 to 23
    val minute: Int, // 0 to 59
    val enabled: Boolean = true
)
