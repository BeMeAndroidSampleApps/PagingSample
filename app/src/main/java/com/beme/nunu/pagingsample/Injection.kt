package com.beme.nunu.pagingsample

import androidx.lifecycle.ViewModelProvider
import com.beme.nunu.pagingsample.api.GithubService
import com.beme.nunu.pagingsample.data.GithubRepository
import com.beme.nunu.pagingsample.presentation.ViewModelFactory

object Injection {
    /**
     * Creates an instance of [GithubRepository] based on the [GithubService] and a
     * [GithubLocalCache]
     */
    private fun provideGithubRepository(): GithubRepository {
        return GithubRepository(GithubService.create())
    }

    /**
     * Provides the [ViewModelProvider.Factory] that is then used to get a reference to
     * [ViewModel] objects.
     */
    fun provideViewModelFactory(): ViewModelProvider.Factory {
        return ViewModelFactory(provideGithubRepository())
    }
}