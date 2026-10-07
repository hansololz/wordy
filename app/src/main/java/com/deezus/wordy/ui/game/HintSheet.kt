package com.deezus.wordy.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.deezus.wordy.R
import com.deezus.wordy.ui.theme.WordyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HintSheet(onDismiss: () -> Unit, onHint: (HintType) -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = stringResource(R.string.hint_title),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      HintOption(
        title = stringResource(R.string.hint_next_letter_title),
        description = stringResource(R.string.hint_next_letter_description),
        color = WordyColors.Correct,
        onClick = { onHint(HintType.RevealNextLetter) },
      )
      HintOption(
        title = stringResource(R.string.hint_absent_letter_title),
        description = stringResource(R.string.hint_absent_letter_description),
        color = WordyColors.Accent,
        onClick = { onHint(HintType.RevealAbsentLetter) },
      )
      HintOption(
        title = stringResource(R.string.hint_all_absent_letters_title),
        description = stringResource(R.string.hint_all_absent_letters_description),
        color = WordyColors.Present,
        onClick = { onHint(HintType.RevealAllAbsentLetters) },
      )
    }
  }
}

@Composable
private fun HintOption(title: String, description: String, color: Color, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = WordyColors.SurfaceBright,
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(Modifier.size(12.dp).clip(CircleShape).background(color))
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = WordyColors.TextMuted,
        )
      }
    }
  }
}
