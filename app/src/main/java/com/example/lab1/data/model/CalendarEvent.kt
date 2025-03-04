package com.example.lab1.data.model

import java.util.Date

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startDate: Date,
    val endDate: Date
) 