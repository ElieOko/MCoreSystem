package elieoko.app.mcoresystem.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BlueDarkPrimary,
    onPrimary = BlueDarkBackground,
    primaryContainer = BluePrimaryDark,
    onPrimaryContainer = BluePrimaryLight,
    secondary = BlueDarkSecondary,
    onSecondary = BlueDarkBackground,
    tertiary = BlueDarkTertiary,
    background = BlueDarkBackground,
    onBackground = Color(0xFFE8EEF5),
    surface = BlueDarkSurface,
    onSurface = Color(0xFFE8EEF5),
    surfaceVariant = BlueDarkSurfaceVariant,
    onSurfaceVariant = BluePrimaryLight,
    outline = BlueOutline
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryLight,
    onPrimaryContainer = BluePrimaryDark,
    secondary = BlueSecondary,
    onSecondary = BlueOnPrimary,
    tertiary = BlueTertiary,
    background = BlueBackground,
    onBackground = BlueOnBackground,
    surface = BlueSurface,
    onSurface = BlueOnSurface,
    surfaceVariant = BlueSurfaceVariant,
    onSurfaceVariant = BluePrimaryDark,
    outline = BlueOutline
)

@Composable
fun MCoreSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
