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
import com.deezus.wordy.R
import com.deezus.wordy.data.BookmarkEntry
import com.deezus.wordy.data.getAllBookmark
import com.deezus.wordy.databinding.FragmentBookmarkBinding
import com.deezus.wordy.databinding.ItemBookmarkBinding
import com.deezus.wordy.scope
import kotlinx.coroutines.launch


class BookmarkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val title: TextView
  val bookmarkButton: ImageView
  val searchButton: ImageView

  init {
    val binding = ItemBookmarkBinding.bind(view)

    title = binding.title
    bookmarkButton = binding.bookmarkButton
    searchButton = binding.searchButton
  }
}

private class BookmarkAdapter(
  private val navController: NavController,
  private val words: List<BookmarkEntry>)
  : RecyclerView.Adapter<BookmarkViewHolder>() {

  override fun getItemCount(): Int {
    return words.size
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_history, parent, false)

    return BookmarkViewHolder(view)
  }

  override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
    words[position].let {
      holder.title.text = it.word
      holder.searchButton.setOnClickListener {
        navController.navigate(R.id.navigation_definition)
      }
    }
  }
}

class BookmarkFragment : BaseFragment() {
  private var _binding: FragmentBookmarkBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentBookmarkBinding.inflate(inflater, container, false)
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
      binding.feed.adapter = BookmarkAdapter(findNavController(), getAllBookmark())
    }
  }
}