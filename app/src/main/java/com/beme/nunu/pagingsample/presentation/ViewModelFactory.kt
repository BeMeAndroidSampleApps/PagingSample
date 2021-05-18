package com.beme.nunu.pagingsample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beme.nunu.pagingsample.data.GithubRepository

class ViewModelFactory(private val repository: GithubRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchRepositoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchRepositoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}