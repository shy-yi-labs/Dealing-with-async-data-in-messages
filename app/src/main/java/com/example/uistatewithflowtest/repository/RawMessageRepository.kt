package com.example.uistatewithflowtest.repository

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RawMessage(
    val id: Long,
    val channelId: Long,
    val value: String
)

private fun Long.toRawMessage(channelId: Long): RawMessage {
    return RawMessage(
        this,
        channelId,
        "$this!"
    )
}

enum class FetchType {
    Older, Around, Newer
}

class RawMessageRepository(
    private val pushCount: Int = 10,
    private val pushInterval: Long = 3000
) {
    private val mutex = Mutex()
    private val rawMessages = MutableList(INITIAL_RAW_MESSAGES_COUNT) { it.toLong().toRawMessage(getRandomChannel()) }

    suspend fun fetchLatest(
        channelId: Long,
        count: Int
    ): List<RawMessage> {
        return mutex.withLock {
            rawMessages.filter { it.channelId == channelId }.takeLast(count)
        }
    }

    suspend fun fetch(
        channelId: Long,
        pivot: Long,
        count: Int,
        type: FetchType
    ): List<RawMessage> {
        return mutex.withLock {

            val filteredMessages = rawMessages.filter { it.channelId == channelId }
            val pivotIndex = filteredMessages.indexOfFirst { it.id == pivot }

            if (pivotIndex < 0) return emptyList()

            val (from, to) = when (type) {
                FetchType.Older -> Pair(pivotIndex - count, pivotIndex)
                FetchType.Around -> Pair(pivotIndex - count, pivotIndex + count)
                FetchType.Newer -> Pair(pivotIndex, pivotIndex + count)
            }

            filteredMessages.subList(
                fromIndex = from.coerceAtLeast(0),
                toIndex = to.coerceAtMost(rawMessages.lastIndex)
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    val pushes = flow {
        for(i in INITIAL_RAW_MESSAGES_COUNT.toLong() .. (INITIAL_RAW_MESSAGES_COUNT + pushCount)) {
            delay(pushInterval)
            mutex.withLock {
                val rawItem = i.toRawMessage(getRandomChannel())
                rawMessages.add(rawItem)
                emit(rawItem)
            }
        }
    }.shareIn(GlobalScope, SharingStarted.Eagerly)

    private fun getRandomChannel(): Long = (0L..3L).random()

    companion object {

        private const val INITIAL_RAW_MESSAGES_COUNT = 1000
    }
}