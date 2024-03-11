package com.example.uistatewithflowtest.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Singleton

data class RawMessage(
    val id: Int,
    val value: String
)

private fun Int.toRawMessage(): RawMessage {
    return RawMessage(
        this,
        "$this!"
    )
}

@Singleton
class RawMessageRepository(
    private val pushCount: Int = 10,
    private val pushInterval: Long = 3000
) {
    private val mutex = Mutex()
    private val rawMessages = MutableList(100) { it.toRawMessage() }

    suspend fun fetchLatest(
        count: Int
    ): List<RawMessage> {
        return mutex.withLock {
            rawMessages.takeLast(count)
        }
    }

    suspend fun fetchRange(
        start: Int,
        end: Int
    ): List<RawMessage> {
        return mutex.withLock {
            rawMessages.slice(start .. end)
        }
    }

    val pushes = flow {
        for(i in 100 .. (100 + pushCount)) {
            delay(pushInterval)
            mutex.withLock {
                val rawItem = i.toRawMessage()
                rawMessages.add(rawItem)
                emit(rawItem)
            }
        }
    }
}