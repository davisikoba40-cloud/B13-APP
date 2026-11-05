package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

private val BlackWhiteScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = DarkCard,
    onPrimaryContainer = PureWhite,
    secondary = GrayLight,
    onSecondary = PureBlack,
    secondaryContainer = DarkBorder,
    onSecondaryContainer = OffWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = GrayLight,
    outline = DarkBorder,
    outlineVariant = GrayMuted
)

private val PinkWhiteScheme = darkColorScheme(
    primary = PinkWhite,
    onPrimary = PinkDarkBg,
    primaryContainer = PinkCard,
    onPrimaryContainer = PinkSoft,
    secondary = PinkAccent,
    onSecondary = PureWhite,
    secondaryContainer = PinkBorder,
    onSecondaryContainer = PinkWhite,
    background = PinkDarkBg,
    onBackground = PinkWhite,
    surface = PinkCard,
    onSurface = PinkWhite,
    surfaceVariant = PinkBorder,
    onSurfaceVariant = PinkSoft,
    outline = PinkAccent,
    outlineVariant = PinkBorder
)

private val BlueWhiteScheme = darkColorScheme(
    primary = BlueWhite,
    onPrimary = BlueDarkBg,
    primaryContainer = BlueCard,
    onPrimaryContainer = BlueSoft,
    secondary = BlueAccent,
    onSecondary = PureWhite,
    secondaryContainer = BlueBorder,
    onSecondaryContainer = BlueWhite,
    background = BlueDarkBg,
    onBackground = BlueWhite,
    surface = BlueCard,
    onSurface = BlueWhite,
    surfaceVariant = BlueBorder,
    onSurfaceVariant = BlueSoft,
    outline = BlueAccent,
    outlineVariant = BlueBorder
)

private val DarkStealthScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFF18181B),
    onPrimaryContainer = PureWhite,
    secondary = Color(0xFFA1A1AA),
    onSecondary = PureBlack,
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = Color(0xFF09090B),
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF18181B),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF3F3F46),
    outlineVariant = Color(0xFF27272A)
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.BLACK_WHITE,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.BLACK_WHITE -> BlackWhiteScheme
        AppThemeMode.PINK_WHITE -> PinkWhiteScheme
        AppThemeMode.BLUE_WHITE -> BlueWhiteScheme
        AppThemeMode.DARK_STEALTH -> DarkStealthScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
