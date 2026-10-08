package com.deezus.wordy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The app's palette. The app is always dark, so these are plain constants rather than values that
 * switch with the system theme. They match the iOS app, except the accent, which is orange here to
 * match the launcher icon.
 */
object WordyColors {
  val Background = Color(0xFF313338)
  val Surface = Color(0xFF2B2D31)
  val TileEmpty = Color(0xFF27292D)
  val SurfaceDim = Color(0xFF1E1F22)
  val SurfaceBright = Color(0xFF3A3C43)
  val SurfaceBrighter = Color(0xFF454850)

  val Text = Color(0xFFF2F3F5)
  val TextMuted = Color(0xFFA3A9B2)
  val TextDisabled = Color(0xFF6D6F78)

  val Accent = Color(0xFFD48540)
  val OnAccent = Color(0xFFFFFFFF)
  val AccentContainer = Color(0xFF55321A)
  val OnAccentContainer = Color(0xFFFFD6B8)
  val HintContainer = Color(0xFF27497A)
  val OnHintContainer = Color(0xFFCFE4FF)

  val Correct = Color(0xFF3BA55C)
  val Present = Color(0xFFC98209)
  val Absent = Color(0xFF1E1F22)
  val OnMark = Color(0xFFFFFFFF)
  val OnAbsent = Color(0xFFB5BAC1)

  val CorrectContainer = Color(0xFF244A33)
  val OnCorrectContainer = Color(0xFF8EDDA6)
  val WarningContainer = Color(0xFF54421F)
  val OnWarningContainer = Color(0xFFFFCB6B)
  val ErrorContainer = Color(0xFF4F2E32)
  val OnErrorContainer = Color(0xFFFFB3B3)

  val Key = Color(0xFF565962)
  val OnKey = Color(0xFFF2F3F5)
}

private val ColorScheme = darkColorScheme(
  primary = WordyColors.Accent,
  onPrimary = WordyColors.OnAccent,
  primaryContainer = WordyColors.Accent,
  onPrimaryContainer = WordyColors.OnAccent,
  secondary = Color(0xFFC4C7D0),
  onSecondary = Color(0xFF2B2D31),
  secondaryContainer = WordyColors.Accent,
  onSecondaryContainer = WordyColors.OnAccent,
  tertiary = WordyColors.OnCorrectContainer,
  onTertiary = Color(0xFF0C3018),
  tertiaryContainer = WordyColors.CorrectContainer,
  onTertiaryContainer = WordyColors.OnCorrectContainer,
  error = WordyColors.OnErrorContainer,
  onError = Color(0xFF4A1216),
  errorContainer = WordyColors.ErrorContainer,
  onErrorContainer = WordyColors.OnErrorContainer,
  background = WordyColors.Background,
  onBackground = WordyColors.Text,
  surface = WordyColors.Background,
  onSurface = WordyColors.Text,
  surfaceVariant = WordyColors.SurfaceBright,
  onSurfaceVariant = WordyColors.TextMuted,
  surfaceContainerLowest = WordyColors.SurfaceDim,
  surfaceContainerLow = WordyColors.Surface,
  surfaceContainer = WordyColors.Surface,
  surfaceContainerHigh = WordyColors.Surface,
  surfaceContainerHighest = WordyColors.SurfaceBright,
  inverseSurface = WordyColors.Text,
  inverseOnSurface = WordyColors.SurfaceDim,
  inversePrimary = WordyColors.Accent,
  outline = Color(0xFF7A7E88),
  outlineVariant = Color(0xFF43464D),
  scrim = Color(0xFF000000),
)

private val WordyShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(18.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun WordyTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = ColorScheme, shapes = WordyShapes, content = content)
}
