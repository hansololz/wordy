package com.deezus.wordy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deezus.wordy.GuessedWordEntry
import com.deezus.wordy.R
import com.deezus.wordy.data.addBookmark
import com.deezus.wordy.data.deleteBookmark
import com.deezus.wordy.data.getBookmark
import com.deezus.wordy.databinding.FragmentHistoryBinding
import com.deezus.wordy.databinding.ItemHistoryBinding
import com.deezus.wordy.getAllGuessedWords
import com.deezus.wordy.scope
import kotlinx.coroutines.launch


class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val title: TextView
  val bookmarkButton: ImageView
  val searchButton: ImageView

  init {
    val binding = ItemHistoryBinding.bind(view)

    title = binding.title
    bookmarkButton = binding.bookmarkButton
    searchButton = binding.searchButton
  }
}

private class HistoryAdapter(
  private val navController: NavController,
  private val words: List<GuessedWordEntry>)
  : RecyclerView.Adapter<ViewHolder>() {

  override fun getItemCount(): Int {
    return words.size
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_history, parent, false)

    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    words[position].let { entry ->
      val title = if (entry.scoreEarned > 0) {
        "${entry.word} (+${entry.scoreEarned})"
      } else {
        entry.word
      }

      holder.title.text = title
      holder.searchButton.setOnClickListener {
        navController.navigate(R.id.navigation_definition)
      }
      holder.bookmarkButton.setOnClickListener {
        scope.launch {
          if (getBookmark(entry.word) != null) {
            deleteBookmark(entry.word)
            holder.bookmarkButton.setImageResource(R.drawable.ic_round_bookmark_border_24)
          } else {
            addBookmark(entry.word)
            holder.bookmarkButton.setImageResource(R.drawable.ic_round_bookmark_24)
          }
        }
      }

      scope.launch {
        if (getBookmark(entry.word) != null) {
          holder.bookmarkButton.setImageResource(R.drawable.ic_round_bookmark_24)
        } else {
          holder.bookmarkButton.setImageResource(R.drawable.ic_round_bookmark_border_24)
        }
      }
    }
  }

  private fun updateBookmarkButton(button: ImageView, word: String) {
    scope.launch {
      if (getBookmark(word) != null) {
        button.setImageResource(R.drawable.ic_round_bookmark_24)
      } else {
        button.setImageResource(R.drawable.ic_round_bookmark_border_24)
      }
    }
  }
}

class HistoryFragment : BaseFragment() {
  private var _binding: FragmentHistoryBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentHistoryBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    scope.launch {
      binding.feed.layoutManager = LinearLayoutManager(getMainActivity())
      binding.feed.adapter = HistoryAdapter(findNavController(), getAllGuessedWords())
    }
  }

}