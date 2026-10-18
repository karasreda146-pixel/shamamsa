package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ChurchLightColorScheme = lightColorScheme(
    primary = ChurchNavyPrimary,
    onPrimary = ChurchNavyOnPrimary,
    primaryContainer = ChurchNavyContainer,
    onPrimaryContainer = ChurchNavyOnContainer,
    secondary = CopticGoldSecondary,
    onSecondary = CopticGoldOnSecondary,
    secondaryContainer = CopticGoldContainer,
    onSecondaryContainer = CopticGoldOnContainer,
    tertiary = ChurchAccent,
    background = ChurchBackground,
    onBackground = ChurchOnSurface,
    surface = ChurchSurface,
    onSurface = ChurchOnSurface,
    surfaceVariant = ChurchSurfaceVariant,
    onSurfaceVariant = ChurchOnSurfaceVariant,
    outline = ChurchOutline
)

@Composable
fun ChurchDeaconsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChurchLightColorScheme,
        typography = Typography,
        content = content
    )
}
