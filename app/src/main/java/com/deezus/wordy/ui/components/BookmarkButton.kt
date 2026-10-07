package com.deezus.wordy.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.deezus.wordy.R
import com.deezus.wordy.ui.theme.WordyColors

@Composable
fun BookmarkButton(
  word: String,
  isBookmarked: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  IconToggleButton(checked = isBookmarked, onCheckedChange = { onToggle() }, modifier = modifier) {
    Icon(
      painter = painterResource(
        if (isBookmarked) R.drawable.ic_round_bookmark_24 else R.drawable.ic_round_bookmark_border_24
      ),
      contentDescription = stringResource(R.string.cd_bookmark_word, word.uppercase()),
      tint = if (isBookmarked) MaterialTheme.colorScheme.primary else WordyColors.TextMuted,
    )
  }
}
