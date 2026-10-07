package com.deezus.wordy.data.dictionary

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap

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

    fun create(): DictionaryApi {
      val json = Json { ignoreUnknownKeys = true }
      return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(DictionaryApi::class.java)
    }
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

class DictionaryRepository(private val api: DictionaryApi) {

  // Definitions do not change, so anything fetched once is reused for the rest of the session.
  private val cache = ConcurrentHashMap<String, WordDefinition>()

  suspend fun lookUp(word: String): DefinitionResult {
    val key = word.lowercase()
    cache[key]?.let { return DefinitionResult.Found(it) }

    return try {
      val meanings = api.lookUp(key).flatMap { it.meanings }.mapNotNull { it.toMeaning() }
      if (meanings.isEmpty()) {
        DefinitionResult.NotFound
      } else {
        val definition = WordDefinition(key, meanings)
        cache[key] = definition
        DefinitionResult.Found(definition)
      }
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
}
