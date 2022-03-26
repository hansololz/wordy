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
import com.deezus.wordy.data.*
import com.deezus.wordy.databinding.FragmentBookmarkBinding
import com.deezus.wordy.databinding.ItemBookmarkBinding
import com.deezus.wordy.databinding.ItemBookmarkHeaderBinding
import com.deezus.wordy.scope
import kotlinx.coroutines.launch


class BookmarkHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val backButton: ImageView

  init {
    val binding = ItemBookmarkHeaderBinding.bind(view)
    backButton = binding.backButton
  }
}

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
  : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  override fun getItemCount(): Int {
    return words.size + 1
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 0) {
      0
    } else {
      1
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return if (viewType == 0) {
      val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.item_bookmark_header, parent, false)

      BookmarkHeaderViewHolder(view)
    } else {
      val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.item_bookmark, parent, false)

      BookmarkViewHolder(view)
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (position == 0) {
      formatHeader(holder as BookmarkHeaderViewHolder)
    } else {
      formatItem(holder as BookmarkViewHolder, words[position - 1])
    }
  }

  private fun formatHeader(holder: BookmarkHeaderViewHolder) {
    holder.backButton.setOnClickListener {
      navController.popBackStack()
    }
  }

  private fun formatItem(holder: BookmarkViewHolder, entry: BookmarkEntry) {
    holder.title.text = entry.word
    holder.searchButton.setOnClickListener {
      DefinitionFragment.currentWord = entry.word
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