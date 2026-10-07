package com.deezus.wordy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deezus.wordy.R
import com.deezus.wordy.ui.theme.WordyColors

/** A word in a list, with an optional result badge and a bookmark toggle. Tapping looks it up. */
@Composable
fun WordCard(
  word: String,
  isBookmarked: Boolean,
  onClick: () -> Unit,
  onToggleBookmark: () -> Unit,
  modifier: Modifier = Modifier,
  summary: String? = null,
  detail: String? = null,
  badge: (@Composable () -> Unit)? = null,
) {
  val shape = MaterialTheme.shapes.large
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .clickable(onClickLabel = stringResource(R.string.action_look_up), onClick = onClick),
    shape = shape,
    color = WordyColors.Surface,
  ) {
    Row(
      modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = word.uppercase(),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp,
        )
        if (summary != null) {
          Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = WordyColors.TextMuted,
          )
        }
        if (detail != null) {
          Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = WordyColors.TextDisabled,
            letterSpacing = 1.sp,
          )
        }
      }
      badge?.invoke()
      BookmarkButton(word, isBookmarked, onToggleBookmark)
    }
  }
}

/** A small coloured label, such as the points earned for a word. */
@Composable
fun Badge(text: String, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
  Text(
    text = text,
    modifier = modifier
      .clip(CircleShape)
      .background(containerColor)
      .padding(horizontal = 10.dp, vertical = 4.dp),
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.SemiBold,
    color = contentColor,
  )
}
