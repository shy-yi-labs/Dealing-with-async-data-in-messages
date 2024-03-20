package com.example.uistatewithflowtest

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TreeMap

class OrderedMapFlow<K, V>(
    private val treeMap: TreeMap<K, V> = TreeMap(),
    val onKeyExists: (V, V) -> Boolean = { _, _ -> true }
) : Flow<Map<K, V>>,
    Map<K, V> by treeMap {

    private val mutex = Mutex()
    private val sharedFlow = MutableSharedFlow<TreeMap<K, V>>(replay = 1)

    suspend fun put(key: K, value: V) {
        mutex.withLock {
            val old = treeMap[key]

            if (old != null && !onKeyExists(old, value)) return

            treeMap[key] = value
            sharedFlow.emit(treeMap)
        }
    }

    suspend fun putAll(pairs: List<Pair<K, V>>) {
        mutex.withLock {
            pairs.forEach { (key, value) ->
                val old = treeMap[key]

                if (old != null && !onKeyExists(old, value)) return@forEach

                treeMap[key] = value
            }
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
            sharedFlow.emit(treeMap)
        }
    }

    override suspend fun collect(collector: FlowCollector<Map<K, V>>) {
        sharedFlow.collect(collector)
    }
}