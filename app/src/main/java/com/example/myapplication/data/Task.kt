package com.example.myapplication.data

enum class Priority(val label: String) {
    HIGH("高"),
    MEDIUM("中"),
    LOW("低")
}

data class Task(
    val id: Int,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM
)
