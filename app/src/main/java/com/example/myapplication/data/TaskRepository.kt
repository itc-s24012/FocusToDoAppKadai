package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(context: Context? = null) {
    private val prefs: SharedPreferences? = context?.applicationContext?.getSharedPreferences("focus_todo_prefs", Context.MODE_PRIVATE)

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private var nextId = 1

    init {
        loadTasks()
    }

    private fun loadTasks() {
        if (prefs == null) return
        val jsonString = prefs.getString("saved_tasks", null) ?: return
        try {
            val jsonArray = JSONArray(jsonString)
            val loadedTasks = mutableListOf<Task>()
            var maxId = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getInt("id")
                val title = obj.getString("title")
                val isCompleted = obj.optBoolean("isCompleted", false)
                val priorityName = obj.optString("priority", Priority.MEDIUM.name)
                val priority = try { Priority.valueOf(priorityName) } catch (e: Exception) { Priority.MEDIUM }

                loadedTasks.add(Task(id = id, title = title, isCompleted = isCompleted, priority = priority))
                if (id > maxId) maxId = id
            }
            _tasks.value = loadedTasks
            nextId = maxId + 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTasks() {
        if (prefs == null) return
        try {
            val jsonArray = JSONArray()
            _tasks.value.forEach { task ->
                val obj = JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("isCompleted", task.isCompleted)
                    put("priority", task.priority.name)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_tasks", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addTask(title: String, priority: Priority = Priority.MEDIUM) {
        if (title.isBlank()) return
        _tasks.value = _tasks.value + Task(id = nextId++, title = title, priority = priority)
        saveTasks()
    }

    fun toggleTask(id: Int) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == id) task.copy(isCompleted = !task.isCompleted) else task
        }
        saveTasks()
    }

    fun updateTask(id: Int, newTitle: String, newPriority: Priority) {
        if (newTitle.isBlank()) return
        _tasks.value = _tasks.value.map { task ->
            if (task.id == id) task.copy(title = newTitle, priority = newPriority) else task
        }
        saveTasks()
    }

    fun deleteTask(id: Int) {
        _tasks.value = _tasks.value.filterNot { it.id == id }
        saveTasks()
    }

    fun clearCompletedTasks() {
        _tasks.value = _tasks.value.filterNot { it.isCompleted }
        saveTasks()
    }
}
