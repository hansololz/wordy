package com.deezus.wordy.ui

import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.deezus.wordy.data.setupBookmarkButton
import com.deezus.wordy.databinding.FragmentDefinitionBinding
import com.deezus.wordy.helpers.scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import java.net.UnknownHostException


data class Content(
  var errorMessage: String? = null,
  var loadingMessage: String? = null,
  var definition: SpannableStringBuilder? = null
)

class DefinitionViewModel : ViewModel() {
  val content = MutableLiveData(Content(loadingMessage = "Loading word definition..."))
}

class DefinitionFragment : BaseFragment() {
  private var _binding: FragmentDefinitionBinding? = null
  private val binding get() = _binding!!
  private lateinit var viewModel: DefinitionViewModel

  companion object {
    var currentWord = ""
    var httpClient: OkHttpClient = OkHttpClient()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentDefinitionBinding.inflate(inflater, container, false)
    viewModel = ViewModelProvider(getMainActivity())[DefinitionViewModel::class.java]
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.backButton.setOnClickListener {
      findNavController().popBackStack()
    }
    binding.title.text = currentWord
    setupBookmarkButton(binding.bookmarkButton, currentWord)

    viewModel.content.observe(getMainActivity()) { maybeContent ->
      if (_binding != null) {
        maybeContent?.let { content ->
          if (content.definition != null ) {
            binding.definition.text = content.definition
            binding.definition.visibility = View.VISIBLE
          } else {
            binding.definition.visibility = View.GONE
          }

          if (content.loadingMessage != null ) {
            binding.loadingMessage.text = content.loadingMessage
            binding.loadingMessage.visibility = View.VISIBLE
          } else {
            binding.loadingMessage.visibility = View.GONE
          }

          if (content.errorMessage != null ) {
            binding.errorMessage.text = content.errorMessage
            binding.errorMessage.visibility = View.VISIBLE
          } else {
            binding.errorMessage.visibility = View.GONE
          }
        }
      }
    }

    viewModel.content.value = Content(loadingMessage = "Loading word definition...")

    populateDefinition()
  }

  private fun populateDefinition() {
    scope.launch {
      try {
        val response = fetchDefinition()
        val responseString = response.body?.string()

        if (responseString?.contains("No Definitions Found") == true) {
          throw Resources.NotFoundException()
        }

        val meanings = JSONArray(responseString)
          .getJSONObject(0)
          .getJSONArray("meanings")

        val builder = SpannableStringBuilder()

        val headerSize = 24
        val textSize = 16

        for (i in 0 until meanings.length()) {
          val meaning = meanings.getJSONObject(i)

          if (meaning.has("partOfSpeech")) {
            val partOfSpeech = SpannableString(meaning.getString("partOfSpeech"))

            partOfSpeech.setSpan(RelativeSizeSpan(1.5f), 0, partOfSpeech.length, 0)
            partOfSpeech.setSpan(StyleSpan(Typeface.BOLD), 0, partOfSpeech.length, 0)

            builder.append(partOfSpeech)
            builder.append("\n\n")

            val definitions = meaning.getJSONArray("definitions")
            for (j in 0 until definitions.length()) {
              val definition = definitions.getJSONObject(j)

              val definitionString = SpannableString(definition.getString("definition"))

              val definitionHeading = SpannableString("Definition: ")
              definitionHeading.setSpan(StyleSpan(Typeface.BOLD), 0, definitionHeading.length, 0)

              builder.append(definitionHeading)
              builder.append(definitionString)
              builder.append("\n")

              if (definition.has("example")) {
                val exampleString = SpannableString(definition.getString("example"))
                val exampleHeading = SpannableString("Example: ")
                exampleHeading.setSpan(StyleSpan(Typeface.BOLD), 0, exampleHeading.length, 0)

                builder.append(exampleHeading)
                builder.append(exampleString)
                builder.append("\n")
              }

              builder.append("\n")
            }
          }

          if (meaning.has("synonyms")) {
            val synonyms = meaning.getJSONArray("synonyms")

            if (synonyms.length() > 0) {
              var synonymsString = synonyms.getString(0)

              for (j in 1 until synonyms.length()) {
                synonymsString += ", ${synonyms.getString(j)}"
              }

              val synonymsSpannableString = SpannableString(synonymsString)

              val synonymHeader = if (synonyms.length() > 1) {
                "Synonyms"
              } else {
                "Synonym"
              }

              val synonymHeading = SpannableString("$synonymHeader: ")
              synonymHeading.setSpan(StyleSpan(Typeface.BOLD), 0, synonymHeading.length, 0)
              builder.append(synonymHeading)
              builder.append(synonymsSpannableString)
              builder.append("\n\n")
            }
          }

          builder.append("\n")
        }

        Log.d("WORDYYY", meanings.toString())

        viewModel.content.value = Content(definition = builder)
      } catch (exception: Resources.NotFoundException) {
        viewModel.content.value = Content(errorMessage = "Could not find word definition.")
      } catch (exception: UnknownHostException) {
        viewModel.content.value = Content(errorMessage = "Could not establish connection with word definition service.")
      } catch (exception: JSONException) {
        viewModel.content.value = Content(errorMessage = "Could not parse response from server.")
      } catch (exception: Exception) {
        viewModel.content.value = Content(errorMessage = "Could not fetch definition.")
      }
    }
  }

  private suspend fun fetchDefinition(): Response = withContext(Dispatchers.Default) {
    val request: Request = Request.Builder()
      .get()
      .url("https://api.dictionaryapi.dev/api/v2/entries/en/$currentWord")
      .build()

    httpClient
      .newCall(request)
      .execute()
  }
}