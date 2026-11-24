package com.aurum.finanzas.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Paleta "Aurum": neutros cálidos (marfil, arena, grafito) con acento dorado champán.
 */
@Immutable
data class AurumPalette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val gold: Color,
    val goldSoft: Color,
    val sage: Color,
    val expense: Color,
    val income: Color,
    val heroTop: Color,
    val heroBottom: Color,
    val heroInk: Color,
    val heroMuted: Color,
)

val LightPalette = AurumPalette(
    isDark = false,
    bg = Color(0xFFF5F2ED),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEDE8E0),
    line = Color(0xFFE5DFD5),
    ink = Color(0xFF15171B),
    muted = Color(0xFF6F6A62),
    faint = Color(0xFFADA79C),
    gold = Color(0xFFB08A4E),
    goldSoft = Color(0xFFF3EADB),
    sage = Color(0xFF4F7C6E),
    expense = Color(0xFFB5544A),
    income = Color(0xFF3F7A66),
    heroTop = Color(0xFF1D2026),
    heroBottom = Color(0xFF2E323B),
    heroInk = Color(0xFFF6F1E7),
    heroMuted = Color(0xFFA7A196),
)

val DarkPalette = AurumPalette(
    isDark = true,
    bg = Color(0xFF0D0E11),
    surface = Color(0xFF16181C),
    surfaceAlt = Color(0xFF202328),
    line = Color(0xFF2A2D33),
    ink = Color(0xFFF2EEE7),
    muted = Color(0xFF9C968C),
    faint = Color(0xFF625D56),
    gold = Color(0xFFD2AE74),
    goldSoft = Color(0xFF2A2418),
    sage = Color(0xFF7DB3A1),
    expense = Color(0xFFE2867A),
    income = Color(0xFF79BFA6),
    heroTop = Color(0xFF262A31),
    heroBottom = Color(0xFF15171B),
    heroInk = Color(0xFFF6F1E7),
    heroMuted = Color(0xFF9C968C),
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

object Aurum {
    val colors: AurumPalette
        @Composable @ReadOnlyComposable get() = LocalPalette.current
}

/** Estilos de texto. Los montos grandes usan tipografía con serifa para un look elegante. */
object AType {
    val display = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-1).sp)
    val displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp)
    val title = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp)
    val heading = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp)
    val bodyStrong = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val amount = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, fontFeatureSettings = "tnum")
    val label = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.5.sp, letterSpacing = 1.3.sp)
    val caption = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp)
}

@Composable
fun AurumTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val p = if (dark) DarkPalette else LightPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    val scheme = if (dark) darkColorScheme(
        primary = p.gold, onPrimary = Color(0xFF15171B),
        background = p.bg, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink,
        surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.muted,
        outline = p.line,
    ) else lightColorScheme(
        primary = p.gold, onPrimary = Color.White,
        background = p.bg, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink,
        surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.muted,
        outline = p.line,
    )

    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalPalette provides p) {
            content()
        }
    }
}
