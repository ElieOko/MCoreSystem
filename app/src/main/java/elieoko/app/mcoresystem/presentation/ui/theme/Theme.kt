package elieoko.app.mcoresystem.presentation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OrangeDarkPrimary,
    onPrimary = OrangeOnBackground,
    primaryContainer = OrangePrimaryDark,
    onPrimaryContainer = OrangePrimaryLight,
    secondary = OrangeDarkSecondary,
    onSecondary = OrangeOnBackground,
    tertiary = OrangeDarkTertiary,
    background = OrangeDarkBackground,
    onBackground = Color(0xFFF5EDE4),
    surface = OrangeDarkSurface,
    onSurface = Color(0xFFF5EDE4),
    surfaceVariant = OrangeDarkSurfaceVariant,
    onSurfaceVariant = OrangePrimaryLight,
    outline = OrangeOutline
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = OrangeOnPrimary,
    primaryContainer = OrangePrimaryLight,
    onPrimaryContainer = OrangePrimaryDark,
    secondary = OrangeSecondary,
    onSecondary = OrangeOnPrimary,
    tertiary = OrangeTertiary,
    background = OrangeBackground,
    onBackground = OrangeOnBackground,
    surface = OrangeSurface,
    onSurface = OrangeOnSurface,
    surfaceVariant = OrangeSurfaceVariant,
    onSurfaceVariant = OrangePrimaryDark,
    outline = OrangeOutline
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
