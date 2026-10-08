package com.deezus.wordy.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.deezus.wordy.R
import com.deezus.wordy.ui.components.rememberHaptic
import com.deezus.wordy.ui.theme.WordyColors

private val TileSpacing = 6.dp
private val MaxTileSize = 62.dp
private val LookUpColumnWidth = 40.dp

private const val FLIP_MILLIS = 420
private const val FLIP_STAGGER_MILLIS = 110

/** The grid of guesses, scaled to fit the space it is given. */
@Composable
fun Board(
  rows: List<BoardRow>,
  onLookUp: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val performHaptic = rememberHaptic()

  BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
    val columns = rows.firstOrNull()?.tiles?.size ?: return@BoxWithConstraints

    // A look-up button sits to the right of each row; the same width is reserved on the left so
    // the tiles stay centred.
    val widthForTiles = maxWidth - LookUpColumnWidth * 2 - TileSpacing * (columns - 1)
    val heightForTiles = maxHeight - TileSpacing * (rows.size - 1)
    val tileSize = min(widthForTiles / columns, heightForTiles / rows.size)
      .coerceIn(1.dp, MaxTileSize)

    Column(verticalArrangement = Arrangement.spacedBy(TileSpacing)) {
      for (row in rows) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Spacer(Modifier.width(LookUpColumnWidth))

          Row(horizontalArrangement = Arrangement.spacedBy(TileSpacing)) {
            row.tiles.forEachIndexed { column, tile ->
              TileView(tile, column, tileSize)
            }
          }

          Box(Modifier.width(LookUpColumnWidth), contentAlignment = Alignment.Center) {
            if (row.submittedWord != null) {
              IconButton(
                onClick = { performHaptic(); onLookUp(row.submittedWord) },
                modifier = Modifier.size(LookUpColumnWidth),
              ) {
                Icon(
                  painter = painterResource(R.drawable.ic_round_search_24),
                  contentDescription = stringResource(
                    R.string.cd_look_up,
                    row.submittedWord.uppercase(),
                  ),
                  tint = WordyColors.TextMuted,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TileView(tile: Tile, column: Int, size: Dp) {
  val isMarked = tile.style == TileStyle.Correct ||
    tile.style == TileStyle.Present ||
    tile.style == TileStyle.Absent

  // 0 shows the letter as typed and 1 shows its result. A tile flips over when its row is
  // submitted, one column after another; tiles that are already marked when first shown do not.
  val flip = remember { Animatable(if (isMarked) 1f else 0f) }
  LaunchedEffect(isMarked) {
    if (isMarked) {
      flip.animateTo(
        targetValue = 1f,
        animationSpec = tween(FLIP_MILLIS, column * FLIP_STAGGER_MILLIS, FastOutSlowInEasing),
      )
    } else {
      flip.snapTo(0f)
    }
  }
  val showsResult = flip.value >= 0.5f
  val style = if (isMarked && !showsResult) TileStyle.Typed else tile.style

  // A letter pops slightly as it is typed.
  val typedLetter = if (tile.style == TileStyle.Typed) tile.letter else null
  var previousTypedLetter by remember { mutableStateOf(typedLetter) }
  val pop = remember { Animatable(1f) }
  LaunchedEffect(typedLetter) {
    val wasEmpty = previousTypedLetter == null
    previousTypedLetter = typedLetter
    if (typedLetter != null && wasEmpty) {
      pop.snapTo(1.12f)
      pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    } else {
      // Also settles a pop that was cut short, for example by submitting straight after typing.
      pop.snapTo(1f)
    }
  }

  val background = when (style) {
    TileStyle.Empty -> WordyColors.TileEmpty
    TileStyle.Typed -> WordyColors.SurfaceBrighter
    TileStyle.AutoCompleted -> WordyColors.CorrectContainer
    TileStyle.Correct -> WordyColors.Correct
    TileStyle.Present -> WordyColors.Present
    TileStyle.Absent -> WordyColors.Absent
  }
  val textColor = when (style) {
    TileStyle.AutoCompleted -> WordyColors.OnCorrectContainer
    TileStyle.Absent -> WordyColors.OnAbsent
    TileStyle.Correct, TileStyle.Present -> WordyColors.OnMark
    TileStyle.Empty, TileStyle.Typed -> WordyColors.Text
  }
  val borderColor = if (tile.isCursor) WordyColors.Accent else Color.Transparent

  val letter = tile.letter?.uppercaseChar()?.toString()
  val description = when {
    letter == null -> stringResource(R.string.tile_empty)
    tile.style == TileStyle.Correct -> stringResource(R.string.tile_correct, letter)
    tile.style == TileStyle.Present -> stringResource(R.string.tile_present, letter)
    tile.style == TileStyle.Absent -> stringResource(R.string.tile_absent, letter)
    tile.style == TileStyle.AutoCompleted -> stringResource(R.string.tile_auto_completed, letter)
    else -> letter
  }

  val shape = RoundedCornerShape(size * 0.18f)
  Box(
    modifier = Modifier
      .size(size)
      .graphicsLayer {
        // Turns edge-on at the halfway point, where the face is swapped.
        rotationX = (if (showsResult) 1f - flip.value else flip.value) * 180f
        cameraDistance = 12f * density
        scaleX = pop.value
        scaleY = pop.value
      }
      .clip(shape)
      .background(background)
      .border(2.dp, borderColor, shape)
      .clearAndSetSemantics { contentDescription = description },
    contentAlignment = Alignment.Center,
  ) {
    if (letter != null) {
      // Sized from the tile rather than the user's font scale so the letter always fits.
      val fontSize = with(LocalDensity.current) { (size * 0.5f).toSp() }
      Text(text = letter, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
  }
}
