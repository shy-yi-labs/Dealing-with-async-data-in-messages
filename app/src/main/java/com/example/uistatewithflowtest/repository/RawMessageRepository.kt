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

class RawMessageRepository(
    private val pushCount: Int = 10,
    private val pushInterval: Long = 3000
) {
    private val mutex = Mutex()
    private val rawMessages = MutableList(100) { it.toLong().toRawMessage(getRandomChannel()) }

    suspend fun fetchLatest(
        channelId: Long,
        count: Int
    ): List<RawMessage> {
        return mutex.withLock {
            rawMessages.filter { it.channelId == channelId }.takeLast(count)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    val pushes = flow {
        for(i in 100L .. (100 + pushCount)) {
            delay(pushInterval)
            mutex.withLock {
                val rawItem = i.toRawMessage(getRandomChannel())
                rawMessages.add(rawItem)
                emit(rawItem)
            }
        }
    }.shareIn(GlobalScope, SharingStarted.Eagerly)

    private fun getRandomChannel(): Long = (0L..3L).random()
}