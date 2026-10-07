package com.deezus.wordy.data

import com.deezus.wordy.data.dictionary.DefinitionResult
import com.deezus.wordy.data.dictionary.DictionaryApi
import com.deezus.wordy.data.dictionary.DictionaryRepository
import com.deezus.wordy.data.dictionary.EntryDto
import com.deezus.wordy.data.dictionary.Meaning
import com.deezus.wordy.data.dictionary.Sense
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class DictionaryRepositoryTest {

  private val json = Json { ignoreUnknownKeys = true }

  /** Answers every lookup by running [respond], and counts how often it was asked. */
  private class FakeApi(private val respond: () -> List<EntryDto>) : DictionaryApi {
    var calls = 0

    override suspend fun lookUp(word: String): List<EntryDto> {
      calls++
      return respond()
    }
  }

  private fun entries(body: String): List<EntryDto> =
    json.decodeFromString(ListSerializer(EntryDto.serializer()), body)

  private fun httpError(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "".toResponseBody()))

  @Test
  fun `a response is reduced to meanings, examples and synonyms`() = runTest {
    val repository = DictionaryRepository(FakeApi { entries(CRANE_RESPONSE) })

    val result = repository.lookUp("CRANE") as DefinitionResult.Found

    assertEquals("crane", result.definition.word)
    assertEquals(
      listOf(
        Meaning(
          partOfSpeech = "noun",
          senses = listOf(
            Sense("A large bird with long legs and a long neck.", example = null),
            Sense("A machine for lifting heavy loads.", example = "The crane lifted the beam."),
          ),
          synonyms = listOf("derrick"),
        ),
        Meaning(
          partOfSpeech = "verb",
          senses = listOf(Sense("To stretch one's neck to see.", example = null)),
          synonyms = emptyList(),
        ),
      ),
      result.definition.meanings,
    )
  }

  @Test
  fun `a definition is only fetched once`() = runTest {
    val api = FakeApi { entries(CRANE_RESPONSE) }
    val repository = DictionaryRepository(api)

    repository.lookUp("crane")
    repository.lookUp("Crane")

    assertEquals(1, api.calls)
  }

  @Test
  fun `failures are not cached`() = runTest {
    var fail = true
    val api = FakeApi { if (fail) throw IOException("offline") else entries(CRANE_RESPONSE) }
    val repository = DictionaryRepository(api)

    assertEquals(DefinitionResult.NetworkError, repository.lookUp("crane"))
    fail = false
    assertEquals(2, (repository.lookUp("crane") as DefinitionResult.Found).definition.meanings.size)
  }

  @Test
  fun `errors are mapped to what the player can do about them`() = runTest {
    suspend fun resultOf(error: Exception) =
      DictionaryRepository(FakeApi { throw error }).lookUp("crane")

    assertEquals(DefinitionResult.NotFound, resultOf(httpError(404)))
    assertEquals(DefinitionResult.ServerError, resultOf(httpError(522)))
    assertEquals(DefinitionResult.ServerError, resultOf(httpError(429)))
    assertEquals(DefinitionResult.NetworkError, resultOf(IOException("timeout")))
    assertEquals(DefinitionResult.ServerError, resultOf(SerializationException("bad json")))
  }

  @Test
  fun `an entry without usable definitions counts as not found`() = runTest {
    val body = """[{"word":"crane","meanings":[{"partOfSpeech":"noun","definitions":[{"definition":" "}]}]}]"""

    assertEquals(DefinitionResult.NotFound, DictionaryRepository(FakeApi { entries(body) }).lookUp("crane"))
  }

  private companion object {
    // Trimmed from a real api.dictionaryapi.dev response, including fields the app ignores.
    const val CRANE_RESPONSE = """
      [
        {
          "word": "crane",
          "phonetic": "/kɹeɪn/",
          "phonetics": [{"text": "/kɹeɪn/", "audio": ""}],
          "meanings": [
            {
              "partOfSpeech": "noun",
              "definitions": [
                {"definition": "A large bird with long legs and a long neck.", "synonyms": [], "antonyms": []},
                {"definition": "A machine for lifting heavy loads.", "synonyms": [], "antonyms": [], "example": "The crane lifted the beam."}
              ],
              "synonyms": ["derrick"],
              "antonyms": []
            }
          ],
          "license": {"name": "CC BY-SA 3.0", "url": "https://creativecommons.org/licenses/by-sa/3.0"},
          "sourceUrls": ["https://en.wiktionary.org/wiki/crane"]
        },
        {
          "word": "crane",
          "meanings": [
            {
              "partOfSpeech": "verb",
              "definitions": [{"definition": "To stretch one's neck to see.", "synonyms": [], "antonyms": []}],
              "synonyms": [],
              "antonyms": []
            }
          ]
        }
      ]
    """
  }
}
