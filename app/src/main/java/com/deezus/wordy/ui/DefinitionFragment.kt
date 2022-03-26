package com.deezus.wordy.ui

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
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
import com.deezus.wordy.scope
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
  var definition: String? = null
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
    binding.let {
      setupBookmarkButton(it.bookmarkButton, currentWord)
    }

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

    populateDefinition()
  }

  private fun populateDefinition() {
    Log.d("WORDYYY", "WORD: $currentWord")

    scope.launch {
      try {
        val response = fetchDefinition()

        val meanings = JSONArray(response.body?.string())
          .getJSONObject(0)
          .getJSONArray("meanings")

        val builder = SpannableStringBuilder()

        Log.d("WORDYYY", meanings.toString())

        viewModel.content.value = Content(definition = meanings.toString())
      } catch (exception: UnknownHostException) {
        viewModel.content.value = Content(errorMessage = "Failed to establish connection with word definition service.")
      }catch (exception: JSONException) {
        viewModel.content.value = Content(errorMessage = "Failed to parse response from server.")
      } catch (exception: Exception) {
        viewModel.content.value = Content(errorMessage = "Failed to fetch definition.\n$exception")
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