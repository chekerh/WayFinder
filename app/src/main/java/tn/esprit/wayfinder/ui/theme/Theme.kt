package tn.esprit.wayfinder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WayFinderBlueDark80,
    onPrimary = WayFinderOnPrimaryDark,
    primaryContainer = WayFinderBlueDark,
    onPrimaryContainer = WayFinderBlueDark80,
    secondary = WayFinderBlueGreyDark80,
    onSecondary = WayFinderOnPrimaryDark,
    secondaryContainer = Color(0xFF2D2D2D),
    onSecondaryContainer = WayFinderBlueGreyDark80,
    tertiary = WayFinderAccentDark80,
    onTertiary = WayFinderOnPrimaryDark,
    tertiaryContainer = Color(0xFF1A3A5A),
    onTertiaryContainer = WayFinderAccentDark80,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF4A0000),
    onErrorContainer = Color(0xFFFF6B6B),
    background = WayFinderBackgroundDark,
    onBackground = WayFinderOnBackgroundDark,
    surface = WayFinderSurfaceDark,
    onSurface = WayFinderOnSurfaceDark,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF3A3A3A),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = WayFinderBlue,
    surfaceTint = WayFinderBlueDark80
)

private val LightColorScheme = lightColorScheme(
    primary = WayFinderBlue,
    onPrimary = WayFinderOnPrimary,
    primaryContainer = WayFinderBlueLight,
    onPrimaryContainer = WayFinderBlueDark,
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = WayFinderBlueDark,
    tertiary = WayFinderBlueLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1F5FE),
    onTertiaryContainer = WayFinderBlueDark,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    background = WayFinderBackground,
    onBackground = WayFinderOnBackground,
    surface = WayFinderSurface,
    onSurface = WayFinderOnSurface,
    surfaceVariant = Color(0xFFE3F2FD),
    onSurfaceVariant = Color(0xFF424242),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFBDBDBD),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = WayFinderBlueDark80,
    surfaceTint = WayFinderBlue
)

@Composable
fun WayFinderTheme(
    darkTheme: Boolean? = null,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Si darkTheme n'est pas spécifié, utiliser les préférences système
    val systemDarkTheme = isSystemInDarkTheme()
    val actualDarkTheme = darkTheme ?: systemDarkTheme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (actualDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        actualDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // FIX: Commented out deprecated statusBarColor call
            // window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !actualDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
