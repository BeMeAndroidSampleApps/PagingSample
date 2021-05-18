package com.beme.nunu.pagingsample.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.beme.nunu.pagingsample.api.GithubService
import com.beme.nunu.pagingsample.model.Repo
import kotlinx.coroutines.flow.Flow

class GithubRepository(private val service: GithubService) {
    fun getSearchResultStream(query: String): Flow<PagingData<Repo>> =
        Pager(
            config = PagingConfig(pageSize = NETWORK_PAGE_SIZE),
            pagingSourceFactory = { GithubPagingSource(service, query) }
        ).flow
}