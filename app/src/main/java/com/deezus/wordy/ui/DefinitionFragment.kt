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


class DefinitionViewModel : ViewModel() {
  val errorMessage = MutableLiveData<String?>(null)
  val loadingMessage = MutableLiveData<String?>("Loading word definition...")
  val definition = MutableLiveData<SpannableString?>(null)
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
    viewModel = ViewModelProvider(getMainActivity())[DefinitionViewModel::class.java]
    _binding = FragmentDefinitionBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    _binding?.backButton?.setOnClickListener {
      findNavController().popBackStack()
    }
    _binding?.title?.text = currentWord
    _binding?.content?.visibility = View.GONE
    _binding?.let {
      setupBookmarkButton(it.bookmarkButton, currentWord)
    }

    viewModel.definition.observe(getMainActivity()) {
      _binding?.loadingMessage?.visibility = View.GONE
      _binding?.content?.visibility = View.VISIBLE
    }

    viewModel.loadingMessage.observe(getMainActivity()) {
      _binding?.loadingMessage?.text = it
      _binding?.loadingMessage?.visibility = View.VISIBLE
      _binding?.content?.visibility = View.GONE
    }

    viewModel.errorMessage.observe(getMainActivity()) {
      _binding?.loadingMessage?.text = it
      _binding?.loadingMessage?.visibility = View.VISIBLE
      _binding?.content?.visibility = View.GONE
    }

    scope.launch {
      populateDefinition()
    }
  }

  private fun populateDefinition() {
    scope.launch {
      try {
        val response = fetchDefinition()

        val meanings = JSONArray(response.body?.string())
          .getJSONObject(0)
          .getJSONArray("meanings")

        val builder = SpannableStringBuilder()

        viewModel.loadingMessage.value = meanings.toString()
      } catch (exception: UnknownHostException) {
        viewModel.loadingMessage.value = "Failed to establish connection with word definition service."
      }catch (exception: JSONException) {
        viewModel.loadingMessage.value = "Failed to parse response from server."
      } catch (exception: Exception) {
        viewModel.loadingMessage.value = "Failed to fetch definition.\n$exception"
      }
    }
  }

  private suspend fun fetchDefinition(): Response = withContext(Dispatchers.IO) {
    val request: Request = Request.Builder()
      .get()
      .url("https://api.dictionaryapi.dev/api/v2/entries/en/$currentWord")
      .build()

    httpClient
      .newCall(request)
      .execute()
  }
}