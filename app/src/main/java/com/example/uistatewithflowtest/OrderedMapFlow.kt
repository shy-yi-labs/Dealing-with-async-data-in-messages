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
    private val sharedFlow = MutableSharedFlow<TreeMap<K, V>>(replay = 1)

    suspend fun put(key: K, value: V) {
        mutex.withLock {
            treeMap[key] = value
            sharedFlow.emit(treeMap)
        }
    }

    suspend fun putAll(pairs: List<Pair<K, V>>) {
        mutex.withLock {
            treeMap.putAll(pairs)
            sharedFlow.emit(treeMap)
        }
    }

    suspend fun delete(key: K) {
        mutex.withLock {
            treeMap.remove(key)
            sharedFlow.emit(treeMap)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            treeMap.clear()
        }
    }

    override suspend fun collect(collector: FlowCollector<Map<K, V>>) {
        sharedFlow.collect(collector)
    }
}