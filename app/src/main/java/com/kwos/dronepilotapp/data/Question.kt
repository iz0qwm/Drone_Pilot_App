package com.kwos.dronepilotapp.data

import java.io.Serializable

data class Question(
    val questionText: String = "",
    val options: List<String> = listOf(),
    val correctOptionIndex: Int = 0,
    val explanation: String = ""
) : Serializable

