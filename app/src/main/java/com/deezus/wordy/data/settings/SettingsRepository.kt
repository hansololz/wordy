package com.deezus.wordy.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.deezus.wordy.core.GameMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.time.Duration.Companion.days

/** Which number the game screen shows as the score. [id] is persisted and must never change. */
enum class ScoreDisplay(val id: String) {
  Total("TOTAL_SCORE"),
  Average("AVERAGE_SCORE"),
  TotalWithoutHints("TOTAL_SCORE_WITHOUT_HINTS"),
  AverageWithoutHints("AVERAGE_SCORE_WITHOUT_HINTS");

  companion object {
    fun fromId(id: String?): ScoreDisplay? = entries.firstOrNull { it.id == id }
  }
}

data class UserSettings(
  val mode: GameMode = GameMode.Default,
  val scoreDisplay: ScoreDisplay = ScoreDisplay.Total,
  val hapticsEnabled: Boolean = false,
  val autoCompleteEnabled: Boolean = true,
)

data class ScoreStats(
  val totalScore: Long = 0,
  val gamesWon: Long = 0,
  val totalScoreWithoutHints: Long = 0,
  val gamesWonWithoutHints: Long = 0,
) {
  /** Average points per win, or null before the first win. */
  val averageScore: Double?
    get() = average(totalScore, gamesWon)

  val averageScoreWithoutHints: Double?
    get() = average(totalScoreWithoutHints, gamesWonWithoutHints)

  private fun average(score: Long, games: Long): Double? =
    if (games > 0) score.toDouble() / games else null
}

/**
 * User preferences and lifetime score totals. Key names match the SharedPreferences file used
 * before 3.0, so [SharedPreferencesMigration] carries existing values over unchanged.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

  private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
    if (error is IOException) emit(emptyPreferences()) else throw error
  }

  val settings: Flow<UserSettings> = preferences.map { prefs ->
    UserSettings(
      mode = GameMode.fromId(prefs[Keys.CurrentGame]) ?: GameMode.Default,
      scoreDisplay = ScoreDisplay.fromId(prefs[Keys.ScoreDisplay]) ?: ScoreDisplay.Total,
      hapticsEnabled = prefs[Keys.HapticsEnabled] ?: false,
      autoCompleteEnabled = prefs[Keys.AutoCompleteEnabled] ?: true,
    )
  }.distinctUntilChanged()

  val stats: Flow<ScoreStats> = preferences.map { prefs ->
    ScoreStats(
      totalScore = prefs[Keys.TotalScore] ?: 0,
      gamesWon = prefs[Keys.GamesWon] ?: 0,
      totalScoreWithoutHints = prefs[Keys.TotalScoreWithoutHints] ?: 0,
      gamesWonWithoutHints = prefs[Keys.GamesWonWithoutHints] ?: 0,
    )
  }.distinctUntilChanged()

  suspend fun setMode(mode: GameMode) {
    dataStore.edit { it[Keys.CurrentGame] = mode.id }
  }

  suspend fun setScoreDisplay(scoreDisplay: ScoreDisplay) {
    dataStore.edit { it[Keys.ScoreDisplay] = scoreDisplay.id }
  }

  suspend fun setHapticsEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.HapticsEnabled] = enabled }
  }

  suspend fun setAutoCompleteEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.AutoCompleteEnabled] = enabled }
  }

  suspend fun recordWin(score: Int, usedHint: Boolean) {
    dataStore.edit { prefs ->
      prefs[Keys.TotalScore] = (prefs[Keys.TotalScore] ?: 0) + score
      prefs[Keys.GamesWon] = (prefs[Keys.GamesWon] ?: 0) + 1
      if (!usedHint) {
        prefs[Keys.TotalScoreWithoutHints] = (prefs[Keys.TotalScoreWithoutHints] ?: 0) + score
        prefs[Keys.GamesWonWithoutHints] = (prefs[Keys.GamesWonWithoutHints] ?: 0) + 1
      }
    }
  }

  /**
   * Whether it is time to ask for a store review. The first call only starts the clock, so new
   * players are not asked straight away.
   */
  suspend fun isReviewDue(now: Long): Boolean {
    var due = false
    dataStore.edit { prefs ->
      val askAt = prefs[Keys.AskReviewAt] ?: 0
      if (askAt == 0L) {
        prefs[Keys.AskReviewAt] = now + FirstReviewDelay.inWholeMilliseconds
      } else {
        due = now > askAt
      }
    }
    return due
  }

  suspend fun onReviewRequested(now: Long) {
    dataStore.edit { it[Keys.AskReviewAt] = now + ReviewInterval.inWholeMilliseconds }
  }

  /** Whether a pre-3.0 install finished filling its word pool database for [mode]. */
  suspend fun wasLegacyWordPoolSeeded(mode: GameMode): Boolean =
    preferences.first()[Keys.legacyWordPoolSeeded(mode)] ?: false

  private object Keys {
    val TotalScore = longPreferencesKey("SETTINGS_TOTAL_SCORE")
    val GamesWon = longPreferencesKey("SETTINGS_TOTAL_GAMES_WON")
    val TotalScoreWithoutHints = longPreferencesKey("SETTINGS_TOTAL_SCORE_WITHOUT_HINT")
    val GamesWonWithoutHints = longPreferencesKey("SETTINGS_TOTAL_GAMES_WON_WITHOUT_HINT")
    val ScoreDisplay = stringPreferencesKey("SETTINGS_SCORE_VIEW_PREFERENCE")
    val HapticsEnabled = booleanPreferencesKey("SETTINGS_IS_HAPTIC_FEEDBACK_ENABLED")
    val AutoCompleteEnabled = booleanPreferencesKey("SETTINGS_IS_AUTO_COMPLETE_ENABLED")
    val CurrentGame = stringPreferencesKey("SETTINGS_CURRENT_GAME")

    // Long.MAX_VALUE means the player previously chose never to be asked.
    val AskReviewAt = longPreferencesKey("SETTINGS_ASK_REVIEW_TIME")

    fun legacyWordPoolSeeded(mode: GameMode) =
      booleanPreferencesKey("SETTINGS_IS_WORD_SET_SAVED_ENGLISH_${mode.wordLength}")
  }

  companion object {
    private const val LEGACY_SHARED_PREFERENCES = "WORDY_SCORE"
    private val FirstReviewDelay = 4.days
    private val ReviewInterval = 60.days

    fun createDataStore(context: Context, scope: CoroutineScope): DataStore<Preferences> =
      PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        migrations = listOf(SharedPreferencesMigration(context, LEGACY_SHARED_PREFERENCES)),
        scope = scope,
        produceFile = { context.preferencesDataStoreFile("settings") },
      )
  }
}
