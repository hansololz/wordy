package com.deezus.wordy.data.game

import android.content.Context
import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.core.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** The latest game for each word length, so switching length or leaving the app loses nothing. */
@Serializable
data class SavedGames(val games: Map<GameMode, GameState> = emptyMap()) {
  operator fun get(mode: GameMode): GameState? = games[mode]

  fun with(game: GameState): SavedGames = copy(games = games + (game.mode to game))
}

object SavedGamesSerializer : Serializer<SavedGames> {
  private val json = Json { ignoreUnknownKeys = true }

  override val defaultValue = SavedGames()

  override suspend fun readFrom(input: InputStream): SavedGames =
    try {
      json.decodeFromString(SavedGames.serializer(), input.readBytes().decodeToString())
    } catch (error: SerializationException) {
      throw CorruptionException("Cannot read saved games", error)
    } catch (error: IllegalArgumentException) {
      throw CorruptionException("Cannot read saved games", error)
    }

  override suspend fun writeTo(t: SavedGames, output: OutputStream) {
    output.write(json.encodeToString(SavedGames.serializer(), t).encodeToByteArray())
  }
}

class SavedGameStore(
  private val dataStore: DataStore<SavedGames>,
  applicationScope: CoroutineScope,
) {
  // A single writer keeps saves in order; conflation means only the newest state hits the disk.
  private val pendingSaves = Channel<SavedGames>(Channel.CONFLATED)

  // Read back by load() so a save that has not reached the disk yet is never missed.
  @Volatile
  private var latest: SavedGames? = null

  init {
    applicationScope.launch {
      for (games in pendingSaves) {
        try {
          dataStore.updateData { games }
        } catch (error: IOException) {
          // The in-memory copy is still current, and the next save tries the disk again.
          Log.w(TAG, "Could not save games", error)
        }
      }
    }
  }

  suspend fun load(): SavedGames = latest ?: dataStore.data.first()

  fun save(games: SavedGames) {
    latest = games
    pendingSaves.trySend(games)
  }

  companion object {
    private const val TAG = "SavedGameStore"

    fun createDataStore(context: Context, scope: CoroutineScope): DataStore<SavedGames> =
      DataStoreFactory.create(
        serializer = SavedGamesSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { SavedGames() },
        scope = scope,
        produceFile = { context.dataStoreFile("saved_games.json") },
      )
  }
}
