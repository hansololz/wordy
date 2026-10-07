package com.deezus.wordy.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deezus.wordy.R
import com.deezus.wordy.core.LetterMark
import com.deezus.wordy.ui.theme.WordyColors

private const val TopRow = "qwertyuiop"
private const val MiddleRow = "asdfghjkl"
private const val BottomRow = "zxcvbnm"

private val KeySpacing = 5.dp
private val KeyShape = RoundedCornerShape(8.dp)

// Roughly how long a submitted row takes to finish flipping.
private const val MARK_REVEAL_DELAY_MILLIS = 700

@Composable
fun Keyboard(
  keyMarks: Map<Char, LetterMark>,
  enabled: Boolean,
  keyHeight: Dp,
  onLetter: (Char) -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
    KeyRow {
      TopRow.forEach { LetterKey(it, keyMarks[it], enabled, keyHeight, onLetter) }
    }
    KeyRow {
      Spacer(Modifier.weight(0.5f))
      MiddleRow.forEach { LetterKey(it, keyMarks[it], enabled, keyHeight, onLetter) }
      Spacer(Modifier.weight(0.5f))
    }
    KeyRow {
      Spacer(Modifier.weight(1f))
      BottomRow.forEach { LetterKey(it, keyMarks[it], enabled, keyHeight, onLetter) }
      DeleteKey(enabled, keyHeight, onDelete)
    }
  }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(KeySpacing),
    content = content,
  )
}

@Composable
private fun RowScope.LetterKey(
  letter: Char,
  mark: LetterMark?,
  enabled: Boolean,
  height: Dp,
  onLetter: (Char) -> Unit,
) {
  val targetBackground = when {
    !enabled -> WordyColors.Surface
    mark == LetterMark.Correct -> WordyColors.Correct
    mark == LetterMark.Present -> WordyColors.Present
    mark == LetterMark.Absent -> WordyColors.Absent
    else -> WordyColors.Key
  }
  val targetTextColor = when {
    !enabled -> WordyColors.TextDisabled
    mark == LetterMark.Absent -> WordyColors.TextDisabled
    mark != null -> WordyColors.OnMark
    else -> WordyColors.OnKey
  }

  // A key takes its new colour only after the tiles of the guess have flipped over.
  val colorSpec = if (enabled && mark != null) {
    tween<Color>(durationMillis = 250, delayMillis = MARK_REVEAL_DELAY_MILLIS)
  } else {
    tween(durationMillis = 150)
  }
  val background by animateColorAsState(targetBackground, colorSpec, label = "keyBackground")
  val textColor by animateColorAsState(targetTextColor, colorSpec, label = "keyText")

  val label = letter.uppercaseChar().toString()
  val markDescription = when (mark) {
    LetterMark.Correct -> stringResource(R.string.mark_correct)
    LetterMark.Present -> stringResource(R.string.mark_present)
    LetterMark.Absent -> stringResource(R.string.mark_absent)
    null -> null
  }

  KeyBox(
    background = background,
    enabled = enabled,
    height = height,
    weight = 1f,
    onClick = { onLetter(letter) },
    modifier = Modifier.semantics {
      contentDescription = label
      if (markDescription != null) stateDescription = markDescription
    },
  ) {
    Text(text = label, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun RowScope.DeleteKey(enabled: Boolean, height: Dp, onDelete: () -> Unit) {
  KeyBox(
    background = if (enabled) WordyColors.Key else WordyColors.Surface,
    enabled = enabled,
    height = height,
    weight = 2f,
    onClick = onDelete,
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_round_backspace_24),
      contentDescription = stringResource(R.string.cd_delete_letter),
      tint = if (enabled) WordyColors.OnKey else WordyColors.TextDisabled,
    )
  }
}

@Composable
private fun RowScope.KeyBox(
  background: Color,
  enabled: Boolean,
  height: Dp,
  weight: Float,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = modifier
      .weight(weight)
      .height(height)
      .clip(KeyShape)
      .background(background)
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}
