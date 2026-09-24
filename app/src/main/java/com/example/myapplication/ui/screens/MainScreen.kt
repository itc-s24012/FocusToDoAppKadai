package com.example.myapplication.ui.screens

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.Priority
import com.example.myapplication.data.Task
import com.example.myapplication.ui.FilterOption
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.components.TaskItem
import com.example.myapplication.ui.components.TimerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val rawTasks by viewModel.rawTasks.collectAsState()
    val timeLeftInSeconds by viewModel.timeLeftInSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val selectedMinutes by viewModel.selectedMinutes.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val completedPomodoros by viewModel.completedPomodoroCount.collectAsState()
    val focusedTaskId by viewModel.focusedTaskId.collectAsState()
    val currentFilter by viewModel.filterOption.collectAsState()

    var textState by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    val context = LocalContext.current

    val focusedTaskTitle = remember(rawTasks, focusedTaskId) {
        rawTasks.find { it.id == focusedTaskId }?.title
    }

    LaunchedEffect(Unit) {
        viewModel.timerFinishedEvent.collect {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Focus & ToDo") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                TimerCard(
                    timeLeftInSeconds = timeLeftInSeconds,
                    isRunning = isTimerRunning,
                    selectedMinutes = selectedMinutes,
                    timerMode = timerMode,
                    completedPomodoros = completedPomodoros,
                    focusedTaskTitle = focusedTaskTitle,
                    onStartTimer = { viewModel.startTimer() },
                    onStopTimer = { viewModel.stopTimer() },
                    onResetTimer = { viewModel.resetTimer() },
                    onSelectMinutes = { viewModel.setTimerMinutes(it) },
                    onAddMinutes = { viewModel.addTimerMinutes(it) }
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = textState,
                            onValueChange = {
                                if (it.length <= 15) textState = it
                            },
                            label = { Text("新しいタスク (最大15文字)") },
                            supportingText = { Text("${textState.length}/15") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.addTask(textState, newTaskPriority)
                                textState = ""
                            },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("追加")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "優先度:", style = MaterialTheme.typography.bodySmall)
                        Priority.entries.forEach { priority ->
                            FilterChip(
                                selected = newTaskPriority == priority,
                                onClick = { newTaskPriority = priority },
                                label = { Text(priority.label) }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterOption.entries.forEach { option ->
                            FilterChip(
                                selected = currentFilter == option,
                                onClick = { viewModel.setFilterOption(option) },
                                label = { Text(option.label) }
                            )
                        }
                    }

                    if (rawTasks.any { it.isCompleted }) {
                        TextButton(onClick = { viewModel.clearCompletedTasks() }) {
                            Text("完了を消去", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentFilter == FilterOption.COMPLETED) "完了済みのタスクはありません" else "タスクがありません。追加してください",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        isFocused = task.id == focusedTaskId,
                        onToggleTask = { viewModel.toggleTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onEditTask = { editingTask = it },
                        onToggleFocus = { viewModel.setFocusedTaskId(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    editingTask?.let { task ->
        var editTitle by remember { mutableStateOf(task.title) }
        var editPriority by remember { mutableStateOf(task.priority) }

        AlertDialog(
            onDismissRequest = { editingTask = null },
            title = { Text("タスクの編集") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = {
                            if (it.length <= 15) editTitle = it
                        },
                        label = { Text("タスク名 (最大15文字)") },
                        supportingText = { Text("${editTitle.length}/15") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "優先度", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Priority.entries.forEach { priority ->
                            FilterChip(
                                selected = editPriority == priority,
                                onClick = { editPriority = priority },
                                label = { Text(priority.label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTask(task.id, editTitle, editPriority)
                        editingTask = null
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTask = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
