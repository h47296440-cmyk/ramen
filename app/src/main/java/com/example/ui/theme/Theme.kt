package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RamenRed,
    secondary = BrothGold,
    tertiary = EggYolk,
    background = WarmCharcoal,
    surface = CharcoalCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = NoodleIvory,
    onSurface = RicePaper
)

private val LightColorScheme = lightColorScheme(
    primary = RamenRed,
    secondary = WoodBrown,
    tertiary = BrothGold,
    background = RicePaper,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = WarmCharcoal,
    onSurface = WarmCharcoal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamicColor by default to enforce our premium handcrafted Ramen shop aesthetic!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
