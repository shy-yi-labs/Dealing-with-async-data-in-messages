package com.example.uistatewithflowtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uistatewithflowtest.repository.BatchEmitRepository
import com.example.uistatewithflowtest.repository.IndividualEmitRepository
import com.example.uistatewithflowtest.repository.RawItem
import com.example.uistatewithflowtest.repository.RawItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val items: List<Item> = emptyList()
)

data class Item(
    val id: Int,
    val staticValue: String,
    val batchEmitValue: Flow<Int?>,
    val individualEmitValue: Flow<Int?>
)

class ItemFactory(
    private val coroutineScope: CoroutineScope,
    private val batchEmitRepository: BatchEmitRepository,
    private val individualEmitRepository: IndividualEmitRepository
) {
    private val itemCacheStore = mutableMapOf<Int, Pair<RawItem, Item>>()

    fun Collection<RawItem>.toItems(): List<Item> {
        val rawItemsNotInCache = this.filter { rawItem ->
            val cache = itemCacheStore[rawItem.id]
            // Is in cache and rawItem is equal to old rawItem
            return@filter (cache != null && rawItem == cache.first).not()
        }

        val batchEmitFlow =
            flow { emit(batchEmitRepository.get(rawItemsNotInCache.map { it.id })) }

        rawItemsNotInCache.forEach {
            itemCacheStore[it.id] = it to it.toItem(batchEmitFlow)
        }

        return this.map { itemCacheStore[it.id]!!.second }
    }

    private fun RawItem.toItem(batchEmitFlow: Flow<Map<Int, Int?>>): Item {
        return Item(
            id = id,
            staticValue = value,
            batchEmitValue = batchEmitFlow
                .map { it[this.id] }
                .shareIn(coroutineScope, SharingStarted.Eagerly, 1),
            individualEmitValue = flow {
                emit(individualEmitRepository.get(id))
            }.shareIn(coroutineScope, SharingStarted.Eagerly, 1)
        )
    }
}

class MainViewModel : ViewModel() {

    private val rawItemRepository = RawItemRepository(30, 3000)
    private val itemFactory = ItemFactory(
        viewModelScope,
        BatchEmitRepository(2000),
        IndividualEmitRepository(1000)
    )

    private val rawItemsFlow = OrderedMapFlow<Int, RawItem>()

    val uiState: StateFlow<MainUiState> = rawItemsFlow
        .map { it.values }
        .map { rawItems ->
            with(itemFactory) {
                MainUiState(items = rawItems.toItems())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    init {
        viewModelScope.launch {
            rawItemsFlow.putAll(rawItemRepository.fetchLatest(5).map { Pair(it.id, it) })

            rawItemRepository.pushes.collect {
                rawItemsFlow.put(it.id, it)
            }
        }
    }
}