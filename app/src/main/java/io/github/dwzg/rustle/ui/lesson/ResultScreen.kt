package io.github.dwzg.rustle.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dwzg.rustle.core.Reward
import io.github.dwzg.rustle.ui.components.ChunkyButton
import io.github.dwzg.rustle.ui.components.Confetti
import io.github.dwzg.rustle.ui.components.Ferris
import io.github.dwzg.rustle.ui.components.FerrisMood
import io.github.dwzg.rustle.ui.theme.Brand
import kotlinx.coroutines.delay

@Composable
fun ResultScreen(
    reward: Reward,
    headline: String,
    onDone: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    BackHandler(onBack = onDone)
    val great = reward.exercises == 0 || reward.firstTryCorrect * 10 >= reward.exercises * 7
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (great) Confetti(Modifier.fillMaxSize(), seed = reward.hashCode())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Ferris(size = 150.dp, mood = if (great) FerrisMood.Party else FerrisMood.Happy)
            Spacer(Modifier.height(12.dp))
            Text(headline, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            reward.stars?.let {
                Spacer(Modifier.height(12.dp))
                Stars(it)
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Tile("XP", "+${reward.xp}", Brand.Gold, Modifier.weight(1f))
                if (reward.exercises > 0) {
                    Tile("Accuracy", "${reward.firstTryCorrect * 100 / reward.exercises}%", Brand.Success, Modifier.weight(1f))
                }
                Tile("Combo", "🔥${reward.bestCombo}", Brand.Flame, Modifier.weight(1f))
            }
            extra?.let {
                Spacer(Modifier.height(16.dp))
                it()
            }
            if (reward.leveledUp) {
                Spacer(Modifier.height(16.dp))
                Banner("⬆️", "Level ${reward.after.level}!", reward.after.title, MaterialTheme.colorScheme.primary)
            }
            reward.newAchievements.forEach {
                Spacer(Modifier.height(12.dp))
                Banner(it.icon, "Achievement unlocked", it.title, Brand.Gold)
            }
            if (reward.streak > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "🔥 ${reward.streak} day streak",
                    color = Brand.Flame,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))
            ChunkyButton("Continue", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Stars(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val scale = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(250L + i * 220L)
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
            }
            Text(
                "★",
                fontSize = 52.sp,
                color = if (i < count) Brand.Gold else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.scale(scale.value),
            )
        }
    }
}

@Composable
private fun Tile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.13f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), color = color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Black, fontSize = 24.sp)
    }
}

@Composable
private fun Banner(icon: String, kicker: String, title: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 30.sp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(kicker.uppercase(), color = color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
