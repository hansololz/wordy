package com.deezus.wordy.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.deezus.wordy.GuessedWordEntry
import com.deezus.wordy.R
import com.deezus.wordy.databinding.FragmentHistoryBinding
import com.deezus.wordy.databinding.ItemHistoryBinding
import android.text.format.DateFormat
import androidx.recyclerview.widget.LinearLayoutManager
import com.deezus.wordy.getAllGuessedWords
import com.deezus.wordy.scope
import kotlinx.coroutines.launch


class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val title: TextView
//  val context: TextView

  init {
    val binding = ItemHistoryBinding.bind(view)

    title = binding.title
//    context = binding.context
  }
}

private class HistoryAdapter(private val words: List<GuessedWordEntry>) : RecyclerView.Adapter<ViewHolder>() {

  override fun getItemCount(): Int {
    return words.size
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_history, parent, false)

    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    words[position].let {
      val title = if (it.scoreEarned > 0) {
        "${it.word} (+${it.scoreEarned})"
      } else {
        it.word
      }

      holder.title.text = title
//      holder.context.text = convertDate(it.time)
    }
  }

  fun convertDate(dateInMilliseconds: Long): String {
    return DateFormat.format("yyyy/MM/dd", dateInMilliseconds).toString()
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
      binding.feed.adapter = HistoryAdapter(getAllGuessedWords())
    }
  }

}