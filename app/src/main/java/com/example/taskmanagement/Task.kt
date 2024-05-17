package com.example.taskmanagement

data class Task(
    var id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String? = null
)
