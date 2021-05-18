package com.beme.nunu.pagingsample.presentation

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beme.nunu.pagingsample.R
import com.beme.nunu.pagingsample.databinding.RepoViewItemBinding
import com.beme.nunu.pagingsample.model.Repo

class ReposAdapter : PagingDataAdapter<Repo, ReposAdapter.RepoViewHolder>(REPO_COMPARATOR) {
    class RepoViewHolder(private val binding: RepoViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var repo: Repo? = null

        init {
            binding.root.setOnClickListener {
                repo?.url?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    it.context.startActivity(intent)
                }
            }
        }

        fun bind(repo: Repo?) {
            if (repo == null) {
                val resources = itemView.resources
                with(binding) {
                    repoName.text = resources.getString(R.string.loading)
                    repoDescription.isVisible = false
                    repoLanguage.isVisible = false
                    repoStars.text = resources.getString(R.string.unknown)
                    repoForks.text = resources.getString(R.string.unknown)
                }
            } else {
                showRepoData(repo)
            }
        }

        private fun showRepoData(repo: Repo) {
            this.repo = repo
            binding.repoName.text = repo.fullName

            // if the description is missing, hide the TextView
            var descriptionVisibility = View.GONE
            if (repo.description != null) {
                binding.repoDescription.text = repo.description
                descriptionVisibility = View.VISIBLE
            }
            binding.repoDescription.visibility = descriptionVisibility

            with(binding) {
                repoStars.text = repo.stars.toString()
                repoForks.text = repo.forks.toString()
            }

            // if the language is missing, hide the label and the value
            var languageVisibility = View.GONE
            if (!repo.language.isNullOrEmpty()) {
                val resources = this.itemView.context.resources
                binding.repoLanguage.text = resources.getString(R.string.language, repo.language)
                languageVisibility = View.VISIBLE
            }
            binding.repoLanguage.visibility = languageVisibility
        }

        companion object {
            fun create(parent: ViewGroup): RepoViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding = RepoViewItemBinding.inflate(inflater, parent, false)
                return RepoViewHolder(binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepoViewHolder {
        return RepoViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: RepoViewHolder, position: Int) {
        val repoItem = getItem(position)
        if (repoItem != null) {
            holder.bind(repoItem)
        }
    }

    companion object {
        private val REPO_COMPARATOR = object : DiffUtil.ItemCallback<Repo>() {
            override fun areItemsTheSame(oldItem: Repo, newItem: Repo): Boolean =
                oldItem.fullName == newItem.fullName

            override fun areContentsTheSame(oldItem: Repo, newItem: Repo): Boolean =
                oldItem == newItem
        }
    }
}