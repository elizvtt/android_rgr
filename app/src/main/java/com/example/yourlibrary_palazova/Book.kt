package com.example.yourlibrary_palazova

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val rating: Int = 0,
    val favorites: Boolean = false,
    val quotes: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val coverUri: String? = null
)
