package com.example.taskmanagement

data class Task(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String? = ""
)
