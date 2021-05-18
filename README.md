# PagingSample : Android Paging3 라이브러리를 사용해보자

### Paging3 is based on Android-MVVM Architecture + Kotlin Coroutines-Flow

## Data Layer : Data Source를 정의한다 -> PagingSource 구현

> PagingSource의 구현은 **데이터(PagingData)를 불러오는 곳**과 **데이터를 Source에서 불러오는 법**을 정의하는 것

### 기존 Paging 코드의 문제점

```kotlin
// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
private const val GITHUB_STARTING_PAGE_INDEX = 1

/**
 * Repository class that works with local and remote data sources.
 */
class GithubRepository(private val service: GithubService) {

    // keep the list of all results received
    private val inMemoryCache = mutableListOf<Repo>()

    // shared flow of results, which allows us to broadcast updates so
    // the subscriber will have the latest data
    private val searchResults = MutableSharedFlow<RepoSearchResult>(replay = 1)

    // keep the last requested page. When the request is successful, increment the page number.
    private var lastRequestedPage = GITHUB_STARTING_PAGE_INDEX

    // avoid triggering multiple requests in the same time
    private var isRequestInProgress = false

    /**
     * Search repositories whose names match the query, exposed as a stream of data that will emit
     * every time we get more data from the network.
     */
    suspend fun getSearchResultStream(query: String): Flow<RepoSearchResult> {
        Log.d("GithubRepository", "New query: $query")
        lastRequestedPage = 1
        inMemoryCache.clear()
        requestAndSaveData(query)

        return searchResults
    }

    suspend fun requestMore(query: String) {
        if (isRequestInProgress) return
        val successful = requestAndSaveData(query)
        if (successful) {
            lastRequestedPage++
        }
    }

    suspend fun retry(query: String) {
        if (isRequestInProgress) return
        requestAndSaveData(query)
    }

    private suspend fun requestAndSaveData(query: String): Boolean {
        isRequestInProgress = true
        var successful = false

        val apiQuery = query + IN_QUALIFIER
        try {
            val response = service.searchRepos(apiQuery, lastRequestedPage, NETWORK_PAGE_SIZE)
            Log.d("GithubRepository", "response $response")
            val repos = response.items ?: emptyList()
            inMemoryCache.addAll(repos)
            val reposByName = reposByName(query)
            searchResults.emit(RepoSearchResult.Success(reposByName))
            successful = true
        } catch (exception: IOException) {
            searchResults.emit(RepoSearchResult.Error(exception))
        } catch (exception: HttpException) {
            searchResults.emit(RepoSearchResult.Error(exception))
        }
        isRequestInProgress = false
        return successful
    }

    private fun reposByName(query: String): List<Repo> {
        // from the in memory cache select only the repos whose name or description matches
        // the query. Then order the results.
        return inMemoryCache.filter {
            it.name.contains(query, true) ||
                (it.description != null && it.description.contains(query, true))
        }.sortedWith(compareByDescending<Repo> { it.stars }.thenBy { it.name })
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}
```

위와 같이 생으로 구현할 때, GithubRepository 클래스는

- GithubService에서 데이터를 중복되지 않게 불러오는 것
- In-Memory에 캐시하는 것
- 계속 추가되는 페이지를 요청하는 것

과 같은 역할들을 부여받는다.

### 주요 DataType, Parameter

이런 복잡성을 덜어내기 위해, Paging3 API의 PagingSource는 다음과 같은 역할을 부여받는다.

- Paging을 하는 Key 타입을 정의한다.
- 어떤 데이터들이 Load 되는 지 정의한다.
- 어디서 데이터들이 불러와지는 지 정의한다.

다음과 같이

```kotlin
class GithubPagingSource(
        // 어디서 데이터들이 불러와지는 지 정의한다
        private val service: GithubService,
        private val query: String
        // <키의 Data Type, 로드되는 Data Type>
) : PagingSource<Int, Repo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        TODO("Not yet implemented")
    }
   override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        TODO("Not yet implemented")
    }
}
```

### 두 개의 override function

PagingSource에서 두 개의 함수를 구현해야 한다.

- load: 유저가 스크롤 할 때 더 많은 데이터를 **비동기적으로** 가져오기 위한 함수
    - LoadParams<Key> : 페이징을 하기 위한 데이터를 담는 Wrapper Class
        - 로딩을 하기 위한 페이지의 Key가 담겨져 있다: 그러하다면 당연히 첫 로딩때는 null이 담겨있겠지
            - 그래서 null인 경우 Default Page Key를 넣어줌
        - 로딩을 얼만큼 할 건지: 그래서 얼마만큼 데이터를 받아올건데?
    - LoadResult<Key, Data> : API에서 받아온 Response Data를 대체해서 넘겨준다
        - 성공할 시 LoadResult.Page를
            - 만약 해당 방향으로 더 이상 load를 할 수 없다면 nextKey, prevKey에 null을 담아준다
            - 예를 들어서 아래방향으로 스크롤해서 데이터를 받아왔는데 빈 리스트면 nextKey는 null
        - 실패할 시 LoadResult.Error를 넘겨줌

- getRefreshKey: PagingSource.load를 하고도 이후에 해당 페이지에 대한 추가적인 load가 필요할 때
    - 예를 들어서, SwipeRefresh, DB Update해서 데이터 갱신이 필요한 경우
    - refresh 작업은 가장 최근에 보여지는 anchorPosition을 활용하여 데이터 로딩을 다시 요청한다

### 구현

```kotlin
// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
private const val GITHUB_STARTING_PAGE_INDEX = 1

class GithubPagingSource(
        private val service: GithubService,
        private val query: String
) : PagingSource<Int, Repo>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val position = params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = query + IN_QUALIFIER
        return try {
            val response = service.searchRepos(apiQuery, position, params.loadSize)
            val repos = response.items
            val nextKey = if (repos.isEmpty()) {
                null
            } else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                    data = repos,
                    prevKey = if (position == GITHUB_STARTING_PAGE_INDEX) null else position - 1,
                    nextKey = nextKey
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
    // The refresh key is used for subsequent refresh calls to PagingSource.load after the initial load
    override fun getRefreshKey(state: PagingState<Int, Repo>): Int? {
        // We need to get the previous key (or next key if previous is null) of the page
        // that was closest to the most recently accessed index.
        // Anchor position is the most recently accessed index
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

}
```

## Paging 설정 : PagingData

- 어떤 API를 사용할건지?
    - Flow (Pager.flow)
    - LiveData (Pager.liveData)
    - Rx.Flowable (Pager.flowable)
    - Rx.Observable (Pager.observable)
- Parameters
    - PagingConfig
        - 아이템을 로드하는 방법(초기 로드 사이즈 같은 거)을 설정
        - 반드시 넣어야 하는 패러미터는 페이지 크기
        - 페이징 라이브러리는 너가 로딩한 모든 아이템들을 메모리에 적재시켜서
            - maxSize 패러미터로 적재량 조절해야됨
            - 기본적으로 maxSize는 무한대(unbounded);;
            - Minimum value of (pageSize + prefetchDistance * 2)
        - 아직 로딩하지 않은 아이템들에 대해서 PlaceHolder를 보여주는게 default이다 이거 싫으면
            - enablePlaceholders = false 지정
    -
