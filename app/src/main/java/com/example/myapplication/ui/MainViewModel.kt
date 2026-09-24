package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Priority
import com.example.myapplication.data.Task
import com.example.myapplication.data.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class TimerMode(val label: String, val defaultMinutes: Int) {
    WORK("作業時間", 25),
    BREAK("休憩時間", 5)
}

enum class FilterOption(val label: String) {
    ALL("すべて"),
    ACTIVE("未完了"),
    COMPLETED("完了済み")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)
    val rawTasks: StateFlow<List<Task>> = repository.tasks

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _focusedTaskId = MutableStateFlow<Int?>(null)
    val focusedTaskId: StateFlow<Int?> = _focusedTaskId.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.WORK)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _completedPomodoroCount = MutableStateFlow(0)
    val completedPomodoroCount: StateFlow<Int> = _completedPomodoroCount.asStateFlow()

    private val _selectedMinutes = MutableStateFlow(25)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    private val _timeLeftInSeconds = MutableStateFlow(25 * 60)
    val timeLeftInSeconds: StateFlow<Int> = _timeLeftInSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerFinishedEvent = MutableSharedFlow<Unit>()
    val timerFinishedEvent: SharedFlow<Unit> = _timerFinishedEvent.asSharedFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            combine(rawTasks, _filterOption) { taskList, filter ->
                when (filter) {
                    FilterOption.ALL -> taskList
                    FilterOption.ACTIVE -> taskList.filterNot { it.isCompleted }
                    FilterOption.COMPLETED -> taskList.filter { it.isCompleted }
                }
            }.collect { filtered ->
                _tasks.value = filtered
            }
        }
    }

    fun addTask(title: String, priority: Priority = Priority.MEDIUM) {
        val trimmedTitle = title.trim().take(15)
        if (trimmedTitle.isBlank()) return
        if (_filterOption.value == FilterOption.COMPLETED) {
            _filterOption.value = FilterOption.ALL
        }
        repository.addTask(trimmedTitle, priority)
    }

    fun toggleTask(id: Int) {
        repository.toggleTask(id)
    }

    fun updateTask(id: Int, newTitle: String, newPriority: Priority) {
        val trimmedTitle = newTitle.trim().take(15)
        if (trimmedTitle.isBlank()) return
        repository.updateTask(id, trimmedTitle, newPriority)
    }

    fun deleteTask(id: Int) {
        if (_focusedTaskId.value == id) {
            _focusedTaskId.value = null
        }
        repository.deleteTask(id)
    }

    fun clearCompletedTasks() {
        val completedIds = rawTasks.value.filter { it.isCompleted }.map { it.id }.toSet()
        if (_focusedTaskId.value in completedIds) {
            _focusedTaskId.value = null
        }
        repository.clearCompletedTasks()
    }

    fun setFilterOption(option: FilterOption) {
        _filterOption.value = option
    }

    fun setFocusedTaskId(id: Int?) {
        _focusedTaskId.value = if (_focusedTaskId.value == id) null else id
    }

    fun setTimerMinutes(minutes: Int) {
        val validMinutes = minutes.coerceAtLeast(1)
        stopTimer()
        _selectedMinutes.value = validMinutes
        _timeLeftInSeconds.value = validMinutes * 60
    }

    fun addTimerMinutes(deltaMinutes: Int) {
        val newMinutes = (_selectedMinutes.value + deltaMinutes).coerceAtLeast(1)
        setTimerMinutes(newMinutes)
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        if (_timeLeftInSeconds.value <= 0) {
            _timeLeftInSeconds.value = _selectedMinutes.value * 60
        }
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_timeLeftInSeconds.value > 0) {
                delay(1000L)
                _timeLeftInSeconds.value -= 1
            }
            _isTimerRunning.value = false
            onTimerComplete()
        }
    }

    private suspend fun onTimerComplete() {
        _timerFinishedEvent.emit(Unit)
        if (_timerMode.value == TimerMode.WORK) {
            _completedPomodoroCount.value += 1
            _timerMode.value = TimerMode.BREAK
            val breakMinutes = TimerMode.BREAK.defaultMinutes
            _selectedMinutes.value = breakMinutes
            _timeLeftInSeconds.value = breakMinutes * 60
        } else {
            _timerMode.value = TimerMode.WORK
            val workMinutes = TimerMode.WORK.defaultMinutes
            _selectedMinutes.value = workMinutes
            _timeLeftInSeconds.value = workMinutes * 60
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
    }

    fun resetTimer() {
        stopTimer()
        _timeLeftInSeconds.value = _selectedMinutes.value * 60
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
