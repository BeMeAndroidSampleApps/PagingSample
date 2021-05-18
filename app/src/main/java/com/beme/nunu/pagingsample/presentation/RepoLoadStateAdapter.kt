package com.beme.nunu.pagingsample.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beme.nunu.pagingsample.R
import com.beme.nunu.pagingsample.databinding.ReposLoadStateFooterViewItemBinding

class RepoLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<RepoLoadStateAdapter.RepoLoadStateViewHolder>() {

    class RepoLoadStateViewHolder(
        private val binding: ReposLoadStateFooterViewItemBinding,
        retry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.retryButton.setOnClickListener { retry.invoke() }
        }

        fun bind(loadState: LoadState) {
            if (loadState is LoadState.Error) {
                binding.errorMsg.text = loadState.error.localizedMessage
            }
            binding.progressBar.isVisible = loadState is LoadState.Loading
            binding.retryButton.isVisible = loadState is LoadState.Error
            binding.errorMsg.isVisible = loadState is LoadState.Error
        }

        companion object {
            fun create(parent: ViewGroup, retry: () -> Unit): RepoLoadStateViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.repos_load_state_footer_view_item, parent, false)
                val binding = ReposLoadStateFooterViewItemBinding.bind(view)
                return RepoLoadStateViewHolder(binding, retry)
            }
        }
    }

    override fun onBindViewHolder(holder: RepoLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): RepoLoadStateViewHolder = RepoLoadStateViewHolder.create(parent, retry)
}