package com.deezus.wordy.di

import android.content.Context
import com.deezus.wordy.data.bookmarks.BookmarkRepository
import com.deezus.wordy.data.db.WordyDatabase
import com.deezus.wordy.data.dictionary.DictionaryApi
import com.deezus.wordy.data.dictionary.DictionaryRepository
import com.deezus.wordy.data.dictionary.WiktionaryApi
import com.deezus.wordy.data.game.SavedGameStore
import com.deezus.wordy.data.history.HistoryRepository
import com.deezus.wordy.data.legacy.LegacyDataMigrator
import com.deezus.wordy.data.settings.SettingsRepository
import com.deezus.wordy.data.words.WordListLoader
import com.deezus.wordy.data.words.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/** The app's object graph, created once by [com.deezus.wordy.WordyApplication]. */
class AppContainer(context: Context) {

  /** Outlives any screen; use it for writes that must finish even if the UI goes away. */
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  val clock: () -> Long = System::currentTimeMillis

  private val ioDispatcher = Dispatchers.IO
  private val ioScope = CoroutineScope(SupervisorJob() + ioDispatcher)

  private val database by lazy { WordyDatabase.create(context) }

  val settingsRepository by lazy {
    SettingsRepository(SettingsRepository.createDataStore(context, ioScope), ioScope)
  }

  val savedGameStore by lazy {
    SavedGameStore(SavedGameStore.createDataStore(context, ioScope), applicationScope)
  }

  val wordRepository by lazy { WordRepository(database.usedWordDao(), applicationScope) }

  val historyRepository by lazy { HistoryRepository(database.gameRecordDao()) }

  val bookmarkRepository by lazy { BookmarkRepository(database.bookmarkDao(), clock) }

  val dictionaryRepository by lazy { DictionaryRepository(DictionaryApi.create(), WiktionaryApi.create()) }

  private val startup: Deferred<Unit> = applicationScope.async {
    val wordLists = WordListLoader(context.assets, ioDispatcher).load()
    LegacyDataMigrator(context, database, settingsRepository, ioDispatcher)
      .migrateIfNeeded(wordLists)
    wordRepository.initialize(wordLists)
  }

  /** True once word lists are loaded and any pre-3.0 data has been migrated. */
  val isStartupComplete: Boolean get() = startup.isCompleted

  suspend fun awaitStartup() = startup.await()
}
