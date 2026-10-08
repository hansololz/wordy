package com.deezus.wordy.data

import com.deezus.wordy.data.dictionary.DefinitionResult
import com.deezus.wordy.data.dictionary.DictionaryApi
import com.deezus.wordy.data.dictionary.DictionaryRepository
import com.deezus.wordy.data.dictionary.EntryDto
import com.deezus.wordy.data.dictionary.Meaning
import com.deezus.wordy.data.dictionary.Sense
import com.deezus.wordy.data.dictionary.WiktionaryApi
import com.deezus.wordy.data.dictionary.WiktionaryEntryDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

  /** Answers every Wiktionary lookup by running [respond], and counts how often it was asked. */
  private class FakeWiktionary(
    private val respond: () -> Map<String, List<WiktionaryEntryDto>>,
  ) : WiktionaryApi {
    var calls = 0

    override suspend fun lookUp(word: String): Map<String, List<WiktionaryEntryDto>> {
      calls++
      return respond()
    }
  }

  private fun entries(body: String): List<EntryDto> =
    json.decodeFromString(ListSerializer(EntryDto.serializer()), body)

  private fun wiktionaryEntries(body: String): Map<String, List<WiktionaryEntryDto>> =
    json.decodeFromString(MapSerializer(String.serializer(), ListSerializer(WiktionaryEntryDto.serializer())), body)

  private fun httpError(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "".toResponseBody()))

  /** A Wiktionary that has never heard of the word, so the primary dictionary decides the result. */
  private fun unhelpfulWiktionary() = FakeWiktionary { throw httpError(404) }

  private fun repository(api: DictionaryApi, fallback: WiktionaryApi = unhelpfulWiktionary()) =
    DictionaryRepository(api, fallback)

  @Test
  fun `a response is reduced to meanings, examples and synonyms`() = runTest {
    val repository = repository(FakeApi { entries(CRANE_RESPONSE) })

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
    val repository = repository(api)

    repository.lookUp("crane")
    repository.lookUp("Crane")

    assertEquals(1, api.calls)
  }

  @Test
  fun `failures are not cached`() = runTest {
    var fail = true
    val api = FakeApi { if (fail) throw IOException("offline") else entries(CRANE_RESPONSE) }
    val repository = repository(api, FakeWiktionary { throw IOException("offline") })

    assertEquals(DefinitionResult.NetworkError, repository.lookUp("crane"))
    fail = false
    assertEquals(2, (repository.lookUp("crane") as DefinitionResult.Found).definition.meanings.size)
  }

  @Test
  fun `errors are mapped to what the player can do about them`() = runTest {
    // Both dictionaries fail the same way, so the mapping of that failure is what is under test.
    suspend fun resultOf(error: Exception) =
      DictionaryRepository(FakeApi { throw error }, FakeWiktionary { throw error }).lookUp("crane")

    assertEquals(DefinitionResult.NotFound, resultOf(httpError(404)))
    assertEquals(DefinitionResult.ServerError, resultOf(httpError(522)))
    assertEquals(DefinitionResult.ServerError, resultOf(httpError(429)))
    assertEquals(DefinitionResult.NetworkError, resultOf(IOException("timeout")))
    assertEquals(DefinitionResult.ServerError, resultOf(SerializationException("bad json")))
  }

  @Test
  fun `an entry without usable definitions counts as not found`() = runTest {
    val body = """[{"word":"crane","meanings":[{"partOfSpeech":"noun","definitions":[{"definition":" "}]}]}]"""

    assertEquals(DefinitionResult.NotFound, repository(FakeApi { entries(body) }).lookUp("crane"))
  }

  @Test
  fun `wiktionary answers when the primary dictionary is down`() = runTest {
    val repository = repository(FakeApi { throw httpError(522) }, FakeWiktionary { wiktionaryEntries(WIKTIONARY_CRANE) })

    val result = repository.lookUp("crane") as DefinitionResult.Found

    assertEquals("crane", result.definition.word)
    assertEquals(
      listOf(
        Meaning(
          partOfSpeech = "noun",
          senses = listOf(
            Sense("Any bird of the family Gruidae, large birds with long legs & a long neck.", example = null),
            Sense("A machine for lifting heavy loads.", example = "The crane lifted the beam."),
          ),
          synonyms = emptyList(),
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
  fun `wiktionary is also tried for words the primary dictionary does not know`() = runTest {
    val repository = repository(FakeApi { throw httpError(404) }, FakeWiktionary { wiktionaryEntries(WIKTIONARY_CRANE) })

    assertTrue(repository.lookUp("crane") is DefinitionResult.Found)
  }

  @Test
  fun `a dead primary dictionary is skipped for the rest of the session`() = runTest {
    val api = FakeApi { throw IOException("timeout") }
    val wiktionary = FakeWiktionary { wiktionaryEntries(WIKTIONARY_CRANE) }
    val repository = repository(api, wiktionary)

    repository.lookUp("crane")
    repository.lookUp("plinth")

    assertEquals(1, api.calls)
    assertEquals(2, wiktionary.calls)
  }

  @Test
  fun `a primary dictionary that merely lacks a word is still consulted next time`() = runTest {
    val api = FakeApi { throw httpError(404) }
    val repository = repository(api, FakeWiktionary { wiktionaryEntries(WIKTIONARY_CRANE) })

    repository.lookUp("crane")
    repository.lookUp("plinth")

    assertEquals(2, api.calls)
  }

  @Test
  fun `wiktionary entries in other languages are ignored`() = runTest {
    val body = """{"fr":[{"partOfSpeech":"Noun","definitions":[{"definition":"Un oiseau."}]}]}"""
    val repository = repository(FakeApi { throw httpError(522) }, FakeWiktionary { wiktionaryEntries(body) })

    assertEquals(DefinitionResult.NotFound, repository.lookUp("crane"))
  }

  private companion object {
    // Trimmed from a real en.wiktionary.org REST response: HTML in the text, and fields the app ignores.
    const val WIKTIONARY_CRANE = """
      {
        "en": [
          {
            "partOfSpeech": "Noun",
            "language": "English",
            "definitions": [
              {
                "definition": "Any  <a rel=\"mw:WikiLink\" href=\"/wiki/bird\" title=\"bird\">bird</a> of the family <span class=\"biota\"><i>Gruidae</i></span>,\nlarge birds with long legs &amp; a long neck.",
                "parsedExamples": [{"example": ""}],
                "examples": [""]
              },
              {
                "definition": "A <a href=\"/wiki/machine\">machine</a> for lifting heavy loads.",
                "parsedExamples": [{"example": "The <b>crane</b> lifted the beam."}],
                "examples": ["The <b>crane</b> lifted the beam."]
              },
              {"definition": "<span></span>"}
            ]
          },
          {
            "partOfSpeech": "Verb",
            "language": "English",
            "definitions": [{"definition": "To stretch one&#39;s neck to see."}]
          }
        ]
      }
    """

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
