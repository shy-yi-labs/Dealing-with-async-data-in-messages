package com.example.uistatewithflowtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MainUiState(
    val items: List<Item> = emptyList()
)

data class Item(
    val id: Int,
    val staticValue: Int,
    val batchEmitValue: Flow<Int?>,
    val individualEmitValue: Flow<Int?>
)

class MainViewModel : ViewModel() {

    private val itemRepository = ItemRepository(30)
    private val batchEmitRepository = BatchEmitRepository(2000)
    private val individualEmitRepository = IndividualEmitRepository(1000)


    private val _uiState = itemRepository
        .items
        .map { items ->
            val batchFlow =  flow { emit(batchEmitRepository.get(items)) }
            MainUiState(
                items = items.map { item ->
                    Item(
                        id = item,
                        staticValue = item,
                        batchEmitValue = batchFlow.map { it[item] },
                        individualEmitValue =  flow { emit(individualEmitRepository.get(item)) }
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState
}