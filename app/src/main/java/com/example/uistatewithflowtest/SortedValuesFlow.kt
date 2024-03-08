package com.example.uistatewithflowtest

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TreeMap

class SortedValuesFlow<K, V>: Flow<Collection<V>> {

    private val mutex = Mutex()
    private val treeMap = TreeMap<K, V>()
    private val stateFlow = MutableStateFlow<Collection<V>>(emptyList())

    suspend fun put(key: K, value: V) {
        mutex.withLock {
            treeMap[key] = value
            stateFlow.emit(treeMap.values)
        }
    }

    suspend fun putAll(rawItems: List<Pair<K, V>>) {
        mutex.withLock {
            treeMap.putAll(rawItems)
            stateFlow.emit(treeMap.values)
        }
    }

    override suspend fun collect(collector: FlowCollector<Collection<V>>) {
        stateFlow.collect(collector)
    }
}