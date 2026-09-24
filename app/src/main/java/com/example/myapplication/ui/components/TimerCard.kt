package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.TimerMode

@Composable
fun TimerCard(
    timeLeftInSeconds: Int,
    isRunning: Boolean,
    selectedMinutes: Int,
    timerMode: TimerMode,
    completedPomodoros: Int,
    focusedTaskTitle: String?,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onAddMinutes: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = timeLeftInSeconds / 60
    val seconds = timeLeftInSeconds % 60
    val formattedTime = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val containerColor = if (timerMode == TimerMode.WORK) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modePrefix = if (timerMode == TimerMode.WORK) "🔥" else "☕"
                Text(
                    text = "$modePrefix ${timerMode.label}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "🍅 完了: ${completedPomodoros}セット",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!focusedTaskTitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🎯 集中中: $focusedTaskTitle",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onAddMinutes(-5) },
                    enabled = !isRunning
                ) {
                    Text("-5分")
                }

                OutlinedButton(
                    onClick = { onAddMinutes(-1) },
                    enabled = !isRunning
                ) {
                    Text("-1分")
                }

                OutlinedButton(
                    onClick = { onAddMinutes(1) },
                    enabled = !isRunning
                ) {
                    Text("+1分")
                }

                OutlinedButton(
                    onClick = { onAddMinutes(5) },
                    enabled = !isRunning
                ) {
                    Text("+5分")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 25, 50).forEach { preset ->
                    FilterChip(
                        selected = selectedMinutes == preset && !isRunning,
                        onClick = { onSelectMinutes(preset) },
                        label = { Text("${preset}分") },
                        enabled = !isRunning
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isRunning) {
                    Button(
                        onClick = onStopTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("ストップ")
                    }
                } else {
                    Button(
                        onClick = onStartTimer
                    ) {
                        Text("スタート")
                    }
                }

                OutlinedButton(
                    onClick = onResetTimer
                ) {
                    Text("リセット")
                }
            }
        }
    }
}
