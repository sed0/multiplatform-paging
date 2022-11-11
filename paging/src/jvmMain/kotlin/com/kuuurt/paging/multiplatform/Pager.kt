package com.kuuurt.paging.multiplatform

import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import androidx.paging.Pager as AndroidXPager

/**
 * Copyright 2020, Kurt Renzo Acosta, All rights reserved.
 *
 * @author Kurt Renzo Acosta
 * @since 06/11/2020
 */

@FlowPreview
@ExperimentalCoroutinesApi
actual class Pager<K : Any, V : Any> actual constructor(
    clientScope: CoroutineScope,
    config: PagingConfig,
    initialKey: K,
    getItems: suspend (K, Int) -> PagingResult<K, V>,
    getItemKey: (V) -> K,
) {
    private var pagingSource: PagingSource<K, V>? = null

    actual val pagingData: Flow<PagingData<V>> = AndroidXPager(config = config) {
        PagingSource(
            initialKey,
            getItems
        ) { state: PagingState<K, V> ->
            val anchorPosition = state.anchorPosition ?: return@PagingSource null
            val closestPageToPosition =
                state.closestPageToPosition(anchorPosition) ?: return@PagingSource null

            getItemKey(closestPageToPosition.data.first())
        }.also { pagingSource = it }
    }.flow

    actual fun refresh() {
        pagingSource?.invalidate()
    }

    class PagingSource<K : Any, V : Any>(
        private val initialKey: K,
        private val getItems: suspend (K, Int) -> PagingResult<K, V>,
        private val getRefreshK: (state: PagingState<K, V>) -> K?,
    ) : androidx.paging.PagingSource<K, V>() {

        override val jumpingSupported: Boolean
            get() = true

        override val keyReuseSupported: Boolean
            get() = true

        override fun getRefreshKey(state: PagingState<K, V>): K? = getRefreshK(state)

        override suspend fun load(params: LoadParams<K>): LoadResult<K, V> {
            val currentKey = params.key ?: initialKey
            return try {
                val pagingResult = getItems(currentKey, params.loadSize)
                LoadResult.Page(
                    data = pagingResult.items,
                    prevKey = if (currentKey == initialKey) null else pagingResult.prevKey(),
                    nextKey = if (pagingResult.items.isEmpty()) null else pagingResult.nextKey()
                )
            } catch (exception: Exception) {
                return LoadResult.Error(exception)
            }
        }
    }
}