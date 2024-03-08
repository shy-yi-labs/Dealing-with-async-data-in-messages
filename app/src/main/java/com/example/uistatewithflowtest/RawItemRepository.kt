package com.example.uistatewithflowtest

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RawItem(
    val id: Int,
    val value: String
)

private fun Int.toRawItem(): RawItem {
    return RawItem(
        this,
        "$this!"
    )
}

class RawItemRepository(
    private val pushCount: Int = 10,
    private val pushInterval: Long = 3000
) {
    private val mutex = Mutex()
    private val items = MutableList(100) { it.toRawItem() }

    suspend fun fetchLatest(
        count: Int
    ): List<RawItem> {
        return mutex.withLock {
            items.takeLast(count)
        }
    }

    suspend fun fetchRange(
        start: Int,
        end: Int
    ): List<RawItem> {
        return mutex.withLock {
            items.slice(start .. end)
        }
    }

    val pushes = flow {
        for(i in 100 .. (100 + pushCount)) {
            delay(pushInterval)
            mutex.withLock {
                val rawItem = i.toRawItem()
                items.add(rawItem)
                emit(rawItem)
            }
        }
    }
}