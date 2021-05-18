package com.beme.nunu.pagingsample.presentation

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import com.beme.nunu.pagingsample.Injection
import com.beme.nunu.pagingsample.R
import com.beme.nunu.pagingsample.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
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
        binding.list.adapter = adapter
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

}