package com.deezus.wordy.data.words

import android.content.res.AssetManager
import com.deezus.wordy.core.GameMode
import com.deezus.wordy.data.db.UsedWordDao
import com.deezus.wordy.data.db.UsedWordEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/** Reads the bundled word lists. Every word in a list is both a valid guess and a possible answer. */
class WordListLoader(
  private val assets: AssetManager,
  private val ioDispatcher: CoroutineDispatcher,
) {
  suspend fun load(): Map<GameMode, Set<String>> = withContext(ioDispatcher) {
    GameMode.entries.associateWith { mode ->
      assets.open(assetPath(mode)).bufferedReader().useLines { lines ->
        lines.map { it.trim().lowercase() }
          .filter { it.length == mode.wordLength }
          .toCollection(LinkedHashSet())
      }
    }
  }

  private fun assetPath(mode: GameMode) = "words/english${mode.wordLength}.txt"
}

/**
 * Validates guesses and hands out answers. Word lists are held in memory so lookups are
 * synchronous; only the set of already-played words is persisted.
 */
class WordRepository(
  private val usedWordDao: UsedWordDao,
  private val applicationScope: CoroutineScope,
) {
  @Volatile
  private var words: Map<GameMode, Set<String>> = emptyMap()
  private val usedWords = ConcurrentHashMap<GameMode, MutableSet<String>>()

  /** Must complete before any other method is called. */
  suspend fun initialize(wordLists: Map<GameMode, Set<String>>) {
    val used = usedWordDao.getAll().groupBy({ GameMode.fromId(it.mode) }, { it.word })
    for (mode in GameMode.entries) {
      usedWords[mode] = ConcurrentHashMap.newKeySet<String>().apply { addAll(used[mode].orEmpty()) }
    }
    words = wordLists
  }

  fun isValidWord(word: String): Boolean = words.values.any { word in it }

  /** Picks a word that has not been played yet, starting over once every word has been used. */
  fun nextAnswer(mode: GameMode, random: Random = Random.Default): String {
    val all = words.getValue(mode)
    val used = usedWords.getValue(mode)

    var available = all.filterNot { it in used }
    if (available.isEmpty()) {
      used.clear()
      applicationScope.launch { usedWordDao.deleteByMode(mode.id) }
      available = all.toList()
    }

    return available.random(random)
  }

  fun markUsed(mode: GameMode, word: String) {
    if (usedWords.getValue(mode).add(word)) {
      applicationScope.launch { usedWordDao.insert(UsedWordEntity(word, mode.id)) }
    }
  }
}
