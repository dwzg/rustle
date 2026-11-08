package io.github.dwzg.rustle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Brand {
    val Rust = Color(0xFFF74C00)
    val RustLight = Color(0xFFFF8A4C)
    val RustDeep = Color(0xFFB83A00)
    val Gold = Color(0xFFFFC23D)
    val Success = Color(0xFF34C77B)
    val Error = Color(0xFFFF4D6A)
    val Flame = Color(0xFFFF9500)
}

/** Extra colours the Material scheme doesn't cover. */
@Immutable
data class RustleColors(
    val success: Color,
    val onSuccessContainer: Color,
    val successContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val locked: Color,
    val node: Color,
    val nodeShadow: Color,
)

private val DarkExtras = RustleColors(
    success = Brand.Success,
    successContainer = Color(0xFF12301F),
    onSuccessContainer = Color(0xFF8BF0B7),
    error = Brand.Error,
    errorContainer = Color(0xFF3A141C),
    onErrorContainer = Color(0xFFFFA8B6),
    locked = Color(0xFF2A2F3A),
    node = Brand.Rust,
    nodeShadow = Brand.RustDeep,
)

private val LightExtras = RustleColors(
    success = Color(0xFF1FA463),
    successContainer = Color(0xFFD7F7E5),
    onSuccessContainer = Color(0xFF0B5133),
    error = Color(0xFFE0284A),
    errorContainer = Color(0xFFFFE0E5),
    onErrorContainer = Color(0xFF7A0F22),
    locked = Color(0xFFE1E3E8),
    node = Brand.Rust,
    nodeShadow = Brand.RustDeep,
)

val LocalRustleColors = staticCompositionLocalOf { DarkExtras }

private val DarkScheme = darkColorScheme(
    primary = Brand.RustLight,
    onPrimary = Color(0xFF2B0E00),
    primaryContainer = Color(0xFF4A1C05),
    onPrimaryContainer = Color(0xFFFFD2BC),
    secondary = Brand.Gold,
    onSecondary = Color(0xFF2E2100),
    tertiary = Color(0xFF6CD4FF),
    background = Color(0xFF0E1014),
    onBackground = Color(0xFFE9EAEE),
    surface = Color(0xFF0E1014),
    onSurface = Color(0xFFE9EAEE),
    surfaceVariant = Color(0xFF1E222B),
    onSurfaceVariant = Color(0xFFA9AEBB),
    surfaceContainer = Color(0xFF171A21),
    surfaceContainerHigh = Color(0xFF1E222B),
    surfaceContainerHighest = Color(0xFF262B36),
    surfaceContainerLow = Color(0xFF13161C),
    outline = Color(0xFF3A404D),
    outlineVariant = Color(0xFF2A2F3A),
)

private val LightScheme = lightColorScheme(
    primary = Brand.Rust,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF3A1200),
    secondary = Color(0xFFB98500),
    onSecondary = Color.White,
    tertiary = Color(0xFF00658C),
    background = Color(0xFFFAF8F6),
    onBackground = Color(0xFF1B1C20),
    surface = Color(0xFFFAF8F6),
    onSurface = Color(0xFF1B1C20),
    surfaceVariant = Color(0xFFF0ECE8),
    onSurfaceVariant = Color(0xFF5C5F68),
    surfaceContainer = Color(0xFFF3EFEB),
    surfaceContainerHigh = Color(0xFFEDE8E3),
    surfaceContainerHighest = Color(0xFFE6E0DA),
    surfaceContainerLow = Color(0xFFF7F3F0),
    outline = Color(0xFFCBC5BF),
    outlineVariant = Color(0xFFE0DAD4),
)

val Mono = FontFamily.Monospace

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Black),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Black),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp),
    )
}

val CodeTextStyle = TextStyle(fontFamily = Mono, fontSize = 13.5.sp, lineHeight = 20.sp)

@Composable
fun RustleTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalRustleColors provides if (dark) DarkExtras else LightExtras,
    ) {
        val scheme = if (dark) DarkScheme else LightScheme
        MaterialTheme(colorScheme = scheme, typography = AppTypography) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides scheme.onBackground,
                content = content,
            )
        }
    }
}

object RustleTheme {
    val colors: RustleColors
        @Composable get() = LocalRustleColors.current
}
