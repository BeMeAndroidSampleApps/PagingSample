package com.beme.nunu.pagingsample.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.beme.nunu.pagingsample.api.GithubService
import com.beme.nunu.pagingsample.api.IN_QUALIFIER
import com.beme.nunu.pagingsample.model.Repo

private const val GITHUB_STARTING_PAGE_INDEX = 1
const val NETWORK_PAGE_SIZE = 50

class GithubPagingSource(
    private val service: GithubService,
    private val query: String
) : PagingSource<Int, Repo>() {
    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        // We need to get the previous key (or next key if previous is null) of the page
        // that was closest to the most recently accessed index.
        // Anchor position is the most recently accessed index
        // 즉 (이전 키의 +1) 혹은 (다음 키의 -1)을 활용하여 현재 anchorPosition을 가져온다.
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val currentPosition = params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = query + IN_QUALIFIER
        return runCatching {
            val response = service.searchRepos(apiQuery, currentPosition, params.loadSize)
            val repos = response.items
            val nextKey =
                if (repos.isEmpty()) null else currentPosition + (params.loadSize / NETWORK_PAGE_SIZE)
            LoadResult.Page(
                data = repos,
                prevKey = if (currentPosition == GITHUB_STARTING_PAGE_INDEX) null else currentPosition - 1,
                nextKey = nextKey
            )
        }.getOrElse { LoadResult.Error(it) }
    }
}