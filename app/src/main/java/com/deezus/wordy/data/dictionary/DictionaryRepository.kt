package com.deezus.wordy.data.dictionary

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

data class WordDefinition(val word: String, val meanings: List<Meaning>)

data class Meaning(
  val partOfSpeech: String,
  val senses: List<Sense>,
  val synonyms: List<String>,
)

data class Sense(val definition: String, val example: String?)

sealed interface DefinitionResult {
  data class Found(val definition: WordDefinition) : DefinitionResult
  data object NotFound : DefinitionResult
  data object NetworkError : DefinitionResult
  data object ServerError : DefinitionResult
}

interface DictionaryApi {
  @GET("api/v2/entries/en/{word}")
  suspend fun lookUp(@Path("word") word: String): List<EntryDto>

  companion object {
    private const val BASE_URL = "https://api.dictionaryapi.dev/"

    fun create(client: OkHttpClient = DictionaryHttp.client): DictionaryApi =
      DictionaryHttp.retrofit(BASE_URL, client).create(DictionaryApi::class.java)
  }
}

/**
 * Wiktionary's own REST API, used when [DictionaryApi] is unavailable. It returns definitions as
 * HTML snippets keyed by language code, and asks clients to identify themselves.
 */
interface WiktionaryApi {
  @Headers("User-Agent: Wordy (Android word game)")
  @GET("api/rest_v1/page/definition/{word}")
  suspend fun lookUp(@Path("word") word: String): Map<String, List<WiktionaryEntryDto>>

  companion object {
    private const val BASE_URL = "https://en.wiktionary.org/"

    fun create(client: OkHttpClient = DictionaryHttp.client): WiktionaryApi =
      DictionaryHttp.retrofit(BASE_URL, client).create(WiktionaryApi::class.java)
  }
}

internal object DictionaryHttp {
  /**
   * Kept short: when a dictionary is down its edge proxy can take tens of seconds to give up, and
   * the player should be shown the other dictionary's answer long before that.
   */
  private val CALL_TIMEOUT = 6.seconds

  val client: OkHttpClient by lazy {
    OkHttpClient.Builder().callTimeout(CALL_TIMEOUT.toJavaDuration()).build()
  }

  fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit {
    val json = Json { ignoreUnknownKeys = true }
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .build()
  }
}

@Serializable
data class EntryDto(val meanings: List<MeaningDto> = emptyList())

@Serializable
data class MeaningDto(
  val partOfSpeech: String = "",
  val definitions: List<DefinitionDto> = emptyList(),
  val synonyms: List<String> = emptyList(),
)

@Serializable
data class DefinitionDto(val definition: String = "", val example: String? = null)

@Serializable
data class WiktionaryEntryDto(
  val partOfSpeech: String = "",
  val definitions: List<WiktionaryDefinitionDto> = emptyList(),
)

@Serializable
data class WiktionaryDefinitionDto(val definition: String = "", val examples: List<String> = emptyList())

class DictionaryRepository(
  private val api: DictionaryApi,
  private val fallbackApi: WiktionaryApi,
) {

  // Definitions do not change, so anything fetched once is reused for the rest of the session.
  private val cache = ConcurrentHashMap<String, WordDefinition>()

  // Once the primary dictionary has failed and Wiktionary has answered instead, later lookups go
  // straight to Wiktionary rather than waiting out the primary's timeout every time.
  @Volatile private var primaryUnavailable = false

  suspend fun lookUp(word: String): DefinitionResult {
    val key = word.lowercase()
    cache[key]?.let { return DefinitionResult.Found(it) }

    val primary = if (primaryUnavailable) null else fetch(key) { api.lookUp(key).toMeanings() }
    if (primary is DefinitionResult.Found) return primary.also { cache[key] = it.definition }

    val fallback = fetch(key) { fallbackApi.lookUp(key).toMeanings() }
    if (fallback is DefinitionResult.Found) {
      cache[key] = fallback.definition
      if (primary != null && primary != DefinitionResult.NotFound) primaryUnavailable = true
    }
    return fallback
  }

  private fun List<EntryDto>.toMeanings(): List<Meaning> =
    flatMap { it.meanings }.mapNotNull { it.toMeaning() }

  private fun Map<String, List<WiktionaryEntryDto>>.toMeanings(): List<Meaning> =
    this[ENGLISH].orEmpty().mapNotNull { it.toMeaning() }

  private suspend fun fetch(word: String, meanings: suspend () -> List<Meaning>): DefinitionResult {
    return try {
      val found = meanings()
      if (found.isEmpty()) DefinitionResult.NotFound else DefinitionResult.Found(WordDefinition(word, found))
    } catch (error: CancellationException) {
      throw error
    } catch (error: HttpException) {
      if (error.code() == HttpURLConnection.HTTP_NOT_FOUND) {
        DefinitionResult.NotFound
      } else {
        DefinitionResult.ServerError
      }
    } catch (error: IOException) {
      DefinitionResult.NetworkError
    } catch (error: SerializationException) {
      DefinitionResult.ServerError
    }
  }

  private fun MeaningDto.toMeaning(): Meaning? {
    val senses = definitions
      .filter { it.definition.isNotBlank() }
      .map { Sense(it.definition, it.example?.takeIf(String::isNotBlank)) }
    if (senses.isEmpty()) return null
    return Meaning(partOfSpeech, senses, synonyms.filter(String::isNotBlank))
  }

  private fun WiktionaryEntryDto.toMeaning(): Meaning? {
    val senses = definitions.mapNotNull { dto ->
      val definition = dto.definition.htmlToText().takeIf(String::isNotBlank) ?: return@mapNotNull null
      val example = dto.examples.asSequence().map { it.htmlToText() }.firstOrNull(String::isNotBlank)
      Sense(definition, example)
    }
    if (senses.isEmpty()) return null
    return Meaning(partOfSpeech.lowercase(), senses, synonyms = emptyList())
  }

  private companion object {
    const val ENGLISH = "en"

    val HTML_TAG = Regex("<[^>]+>")
    val WHITESPACE = Regex("\\s+")
    val ENTITIES = mapOf("&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&#39;" to "'", "&nbsp;" to " ")

    fun String.htmlToText(): String {
      var text = replace(HTML_TAG, "")
      ENTITIES.forEach { (entity, char) -> text = text.replace(entity, char) }
      return text.replace(WHITESPACE, " ").trim()
    }
  }
}
