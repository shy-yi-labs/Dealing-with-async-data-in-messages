package com.example.uistatewithflowtest.repository

import android.util.Log
import com.example.uistatewithflowtest.repository.message.Message
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TreeMap

sealed interface RawMessage {
    val id: Message.Id
    val updateAt: Long
    data class Normal(
        override val id: Message.Id,
        override val updateAt: Long = System.currentTimeMillis(),
        val text: String = "${id.messageId}!"
    ): RawMessage

    data class Deleted(
        override val id: Message.Id,
        override val updateAt: Long = System.currentTimeMillis(),
    ): RawMessage
}

enum class FetchType {
    Older, Around, Newer
}

data class Page(
    val messages: List<RawMessage.Normal>,
    val lastMessageId: Long?
)

class RawMessageRepository(
    private val pushCount: Int,
    private val pushInterval: Long,
) {
    private val mutex = Mutex()
    private val rawMessages = TreeMap<Message.Id, RawMessage.Normal>()

    init {
        List(INITIAL_RAW_MESSAGES_COUNT) {
            RawMessage.Normal(
                Message.Id(
                    channelId = getRandomChannel(),
                    messageId = it.toLong()
                )
            )
        }.forEach {
            rawMessages[it.id] = it
        }
    }

    suspend fun fetchLatest(
        channelId: Long,
        count: Int
    ): Page {
        return mutex.withLock {
            val filteredMessages = rawMessages.values.filter { it.id.channelId == channelId }
            Page(
                filteredMessages.takeLast(count),
                filteredMessages.last().id.messageId
            )
        }
    }

    suspend fun fetch(
        channelId: Long,
        pivot: Long,
        count: Int,
        type: FetchType
    ): Page {
        return mutex.withLock {

            val filteredMessages = rawMessages.values.filter { it.id.channelId == channelId }
            val pivotIndex = filteredMessages.indexOfFirst { it.id.messageId == pivot }

            val messages = if (pivotIndex < 0) {
                emptyList()
            } else {
                val (from, to) = when (type) {
                    FetchType.Older -> Pair(pivotIndex - count, pivotIndex)
                    FetchType.Around -> Pair(pivotIndex - (count / 2), pivotIndex + (count / 2))
                    FetchType.Newer -> Pair(pivotIndex + 1, pivotIndex + 1 + count)
                }

                filteredMessages.subList(
                    fromIndex = from.coerceIn(0, filteredMessages.lastIndex),
                    toIndex = to.coerceIn(0, filteredMessages.lastIndex)
                )
            }

            Page(
                messages,
                filteredMessages.last().id.messageId
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    val pushes = flow {
        for(i in INITIAL_RAW_MESSAGES_COUNT.toLong() .. (INITIAL_RAW_MESSAGES_COUNT + pushCount)) {
            delay(pushInterval)
            mutex.withLock {
                val channelId = getRandomChannel()

                when ((0 until 3).random()) {
                    0 -> { // Insert
                        val newId = Message.Id(channelId = channelId, messageId = i)

                        rawMessages[newId] = RawMessage.Normal(newId).also { emit(it) }
                        Log.d("RawRepository", "PUSH: ${rawMessages[newId]}")
                    }
                    1 -> { // Delete
                        val idsOfLast10Messages = rawMessages.values
                            .filter { it.id.channelId == channelId }
                            .takeLast(10).map { it.id }

                        val targetId = idsOfLast10Messages.random()

                        rawMessages.remove(targetId)
                        RawMessage.Deleted(targetId).also { emit(it) }
                        Log.d("RawRepository", "PUSH: ${rawMessages[targetId]}")
                    }
                    else -> { // Delete followed by Insert, simulating lag
                        val newId = Message.Id(channelId = channelId, messageId = i)

                        val normal = RawMessage.Normal(newId)
                        val deleted = RawMessage.Deleted(newId)

                        emit(deleted)
                        emit(normal)
                        Log.d("RawRepository", "PUSH: $deleted")
                        Log.d("RawRepository", "PUSH: $normal")
                    }
                }
            }
        }
    }.shareIn(GlobalScope, SharingStarted.Eagerly)

    private fun getRandomChannel(): Long = (0L..3L).random()

    companion object {

        private const val INITIAL_RAW_MESSAGES_COUNT = 1000
    }
}