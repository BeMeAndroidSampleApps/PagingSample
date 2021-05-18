package com.beme.nunu.pagingsample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.beme.nunu.pagingsample.data.GithubRepository
import com.beme.nunu.pagingsample.model.Repo
import kotlinx.coroutines.flow.Flow

class SearchRepositoryViewModel(
    private val repository: GithubRepository
) : ViewModel() {

    private var currentQueryValue: String? = null
    private var currentSearchResult: Flow<PagingData<Repo>>? = null

    fun searchRepo(queryString: String): Flow<PagingData<Repo>> {
        val lastResult = currentSearchResult
        if (queryString == currentQueryValue && lastResult != null)
            return lastResult

        currentQueryValue = queryString
        val newResult = repository.getSearchResultStream(queryString)
            .cachedIn(viewModelScope)
        currentSearchResult = newResult
        return newResult
    }
}