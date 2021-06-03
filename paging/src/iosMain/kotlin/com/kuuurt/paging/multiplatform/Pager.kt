package com.kuuurt.paging.multiplatform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Copyright 2020, Kurt Renzo Acosta, All rights reserved.
 *
 * @author Kurt Renzo Acosta
 * @since 06/11/2020
 */

@OptIn(ExperimentalCoroutinesApi::class)
actual class Pager<K : Any, V : Any> actual constructor(
    private val clientScope: CoroutineScope,
    private val config: PagingConfig,
    private val initialKey: K,
    private val getItems: suspend (K, Int) -> PagingResult<K, V>
) {
    private val items = PagingData<V>()

    private val _pagingData = MutableStateFlow<PagingData<V>?>(null)
    actual val pagingData: Flow<PagingData<V>> get() = _pagingData.filterNotNull()

    private var hasNextPage = true

    private var currentPagingResult: PagingResult<K, V>? = null


    init {
        loadNext()
    }

    fun loadPrevious() {
        loadItems(LoadDirection.PREVIOUS)
    }

    fun loadNext() {
        loadItems(LoadDirection.NEXT)
    }

    private fun loadItems(loadDirection: LoadDirection) {
        val pagingResult = currentPagingResult
        val key = if (pagingResult == null) {
            initialKey
        } else {
            when (loadDirection) {
                LoadDirection.NEXT -> pagingResult.nextKey()
                LoadDirection.PREVIOUS -> pagingResult.prevKey()
            }
        }

        if (key != null && hasNextPage) {
            clientScope.launch {
                val newPagingResult = getItems(key, config.pageSize)
                items.addAll(newPagingResult.items)

                hasNextPage = newPagingResult.items.size >= config.pageSize
                _pagingData.value = PagingData<V>().apply { addAll(items) }

                currentPagingResult = newPagingResult
            }
        }
    }

    enum class LoadDirection {
        PREVIOUS,
        NEXT
    }
}