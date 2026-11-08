package io.github.dwzg.rustle.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dwzg.rustle.core.Achievement
import io.github.dwzg.rustle.core.Stats
import io.github.dwzg.rustle.course.fraction
import io.github.dwzg.rustle.ui.components.Ferris
import io.github.dwzg.rustle.ui.components.FerrisMood
import io.github.dwzg.rustle.ui.components.XpBar
import io.github.dwzg.rustle.ui.theme.Brand

@Composable
fun ProfileScreen(
    stats: Stats,
    achievements: List<Achievement>,
    unlockAll: Boolean,
    onUnlockAll: (Boolean) -> Unit,
    onReset: () -> Unit,
    contentPadding: PaddingValues,
) {
    val level = stats.level
    val unlocked = achievements.count { it.unlocked }
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Ferris(size = 120.dp, mood = FerrisMood.Happy)
            Text("Level ${level.level}", style = MaterialTheme.typography.headlineMedium)
            Text(level.title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            XpBar(level.fraction, Modifier.fillMaxWidth(0.8f))
            Text(
                "${stats.xp} XP total · ${level.span - level.into} XP to level ${level.level + 1}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        val accuracy = stats.accuracy?.let { "$it%" } ?: "–"
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("🔥", "${stats.streak}", "streak", Modifier.weight(1f))
            Stat("🏆", "${stats.bestStreak}", "best streak", Modifier.weight(1f))
            Stat("🎯", accuracy, "accuracy", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("📘", "${stats.lessonsDone}/${stats.lessonsTotal}", "lessons", Modifier.weight(1f))
            Stat("⭐", "${stats.perfect}", "perfect", Modifier.weight(1f))
            Stat("⚡", "${stats.rushBest}", "rush best", Modifier.weight(1f))
        }

        Text("Achievements  $unlocked/${achievements.size}", style = MaterialTheme.typography.titleLarge)
        achievements.forEach { a ->
            val got = a.unlocked
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (got) Brand.Gold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(a.icon, fontSize = 28.sp, modifier = Modifier.alpha(if (got) 1f else 0.3f))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.title, fontWeight = FontWeight.ExtraBold, color = if (got) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(a.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (got) Text("✓", color = Brand.Gold, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }

        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Explorer mode", fontWeight = FontWeight.ExtraBold)
                Text("Unlock every lesson, for those who already know some Rust", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = unlockAll, onCheckedChange = onUnlockAll)
        }
        TextButton(onClick = { confirmReset = true }) {
            Text("Reset all progress", color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Rustle is an unofficial learning app. Ferris the crab is a community mascot of Rust. " +
                "Code runs on the official Rust Playground.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Ferris(size = 64.dp, mood = FerrisMood.Sad) },
            title = { Text("Reset everything?") },
            text = { Text("XP, streaks, stars and achievements will be gone. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("Reset", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stat(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 20.sp)
        Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.Unspecified)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
