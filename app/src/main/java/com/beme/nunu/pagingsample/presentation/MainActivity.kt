package com.beme.nunu.pagingsample.presentation

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import com.beme.nunu.pagingsample.Injection
import com.beme.nunu.pagingsample.R
import com.beme.nunu.pagingsample.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val adapter = ReposAdapter()
    private lateinit var binding: ActivityMainBinding
    private var searchJob: Job? = null
    private val viewModel: SearchRepositoryViewModel by lazy {
        ViewModelProvider(this, Injection.provideViewModelFactory())
            .get(SearchRepositoryViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)

        binding.list.addItemDecoration(decoration)
        binding.list.adapter = adapter.withLoadStateFooter(
            // footer에 retry 버튼 생성, Loading 시 프로그레스바 보여줌
            footer = RepoLoadStateAdapter { adapter.retry() }
        )
        adapter.addLoadStateListener {
            val isListEmpty = it.refresh is LoadState.NotLoading && adapter.itemCount == 0
            showEmptyList(isListEmpty)

            with(binding) {
                list.isVisible = it.source.refresh is LoadState.NotLoading
                progressBar.isVisible = it.source.refresh is LoadState.Loading
                retryButton.isVisible = it.source.refresh is LoadState.Error
            }

            val errorState = it.source.append as? LoadState.Error
                ?: it.source.prepend as? LoadState.Error
                ?: it.append as? LoadState.Error
                ?: it.prepend as? LoadState.Error
            errorState?.let {
                Toast.makeText(
                    this,
                    "\uD83D\uDE28 Wooops ${it.error}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        setUi()
    }

    private fun setUi() {
        with(binding.searchRepo) {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    updateRepoList()
                    true
                } else false
            }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    updateRepoList()
                    true
                } else false
            }
        }

        binding.retryButton.setOnClickListener { adapter.retry() }

        lifecycleScope.launch {
            adapter.loadStateFlow
                .distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.list.scrollToPosition(0) }
        }
    }

    private fun updateRepoList() {
        binding.searchRepo.text
            .trim().let {
                if (it.isNotEmpty()) search(it.toString())
            }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            viewModel.searchRepo(query)
                .collectLatest { adapter.submitData(it) }
        }
    }

    private fun showEmptyList(show: Boolean) {
        if (show) {
            binding.emptyList.isVisible = true
            binding.list.isVisible = false
        } else {
            binding.emptyList.isVisible = false
            binding.list.isVisible = true
        }
    }
}