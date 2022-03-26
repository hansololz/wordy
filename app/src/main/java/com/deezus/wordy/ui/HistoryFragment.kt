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
import com.deezus.wordy.data.HistoryEntry
import com.deezus.wordy.data.getAllHistory
import com.deezus.wordy.data.setupBookmarkButton
import com.deezus.wordy.databinding.FragmentHistoryBinding
import com.deezus.wordy.databinding.ItemHistoryBinding
import com.deezus.wordy.databinding.ItemHistoryHeaderBinding
import com.deezus.wordy.helpers.scope
import kotlinx.coroutines.launch


class HistoryHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val backButton: ImageView

  init {
    val binding = ItemHistoryHeaderBinding.bind(view)
    backButton = binding.backButton
  }
}

class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
  private val words: List<HistoryEntry>)
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
        .inflate(R.layout.item_history_header, parent, false)

      HistoryHeaderViewHolder(view)
    } else {
      val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.item_history, parent, false)

      HistoryViewHolder(view)
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (position == 0) {
      formatHeader(holder as HistoryHeaderViewHolder)
    } else {
      formatItem(holder as HistoryViewHolder, words[position - 1])
    }
  }

  private fun formatHeader(holder: HistoryHeaderViewHolder) {
    holder.backButton.setOnClickListener {
      navController.popBackStack()
    }
  }

  private fun formatItem(holder: HistoryViewHolder, entry: HistoryEntry) {
    val title = if (entry.scoreEarned > 0) {
      "${entry.word} (+${entry.scoreEarned})"
    } else {
      entry.word
    }

    holder.title.text = title
    holder.searchButton.setOnClickListener {
      DefinitionFragment.currentWord = entry.word
      navController.navigate(R.id.navigation_definition)
    }
    setupBookmarkButton(holder.bookmarkButton, entry.word)
  }
}

class HistoryFragment : BaseFragment() {
  private var _binding: FragmentHistoryBinding? = null
  private val binding get() = _binding!!

  private var historyAdapter: HistoryAdapter? = null

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

    binding.feed.layoutManager = LinearLayoutManager(getMainActivity())

    if (historyAdapter != null) {
      binding.feed.adapter = historyAdapter
    } else {
      scope.launch {
        historyAdapter = HistoryAdapter(findNavController(), getAllHistory())
        binding.feed.adapter = historyAdapter
      }
    }
  }
}