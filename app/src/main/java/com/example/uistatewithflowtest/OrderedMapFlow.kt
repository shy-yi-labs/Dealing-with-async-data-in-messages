package com.example.uistatewithflowtest

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TreeMap

class OrderedMapFlow<K, V>: Flow<Map<K, V>> {

    private val mutex = Mutex()
    private val treeMap = TreeMap<K, V>()
    private val stateFlow = MutableSharedFlow<TreeMap<K, V>>(replay = 1)

    suspend fun put(key: K, value: V) {
        mutex.withLock {
            treeMap[key] = value
            stateFlow.emit(treeMap)
        }
    }

    suspend fun putAll(rawItems: List<Pair<K, V>>) {
        mutex.withLock {
            treeMap.putAll(rawItems)
            stateFlow.emit(treeMap)
        }
    }

    override suspend fun collect(collector: FlowCollector<Map<K, V>>) {
        stateFlow.collect(collector)
    }
}