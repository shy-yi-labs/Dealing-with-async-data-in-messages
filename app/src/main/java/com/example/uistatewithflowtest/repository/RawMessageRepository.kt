package com.example.uistatewithflowtest.repository

import com.example.uistatewithflowtest.repository.message.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TreeMap
import kotlin.coroutines.coroutineContext

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

class RawMessageRepository(
    private val pushCount: Int = 10,
    private val pushInterval: Long = 3000
) {
    private val mutex = Mutex()
    private val rawMessages = TreeMap<Message.Id, RawMessage>()

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
    ): List<RawMessage> {
        return mutex.withLock {
            rawMessages.values.filter { it.id.channelId == channelId }.takeLast(count)
        }
    }

    suspend fun fetch(
        channelId: Long,
        pivot: Long,
        count: Int,
        type: FetchType
    ): List<RawMessage> {
        return mutex.withLock {

            val filteredMessages = rawMessages.values.filter { it.id.channelId == channelId }
            val pivotIndex = filteredMessages.indexOfFirst { it.id.messageId == pivot }

            if (pivotIndex < 0) return emptyList()

            val (from, to) = when (type) {
                FetchType.Older -> Pair(pivotIndex - count, pivotIndex)
                FetchType.Around -> Pair(pivotIndex - count, pivotIndex + count)
                FetchType.Newer -> Pair(pivotIndex, pivotIndex + count)
            }

            filteredMessages.subList(
                fromIndex = from.coerceAtLeast(0),
                toIndex = to.coerceAtMost(filteredMessages.lastIndex)
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    val pushes = flow {
        for(i in INITIAL_RAW_MESSAGES_COUNT.toLong() .. (INITIAL_RAW_MESSAGES_COUNT + pushCount)) {
            delay(pushInterval)
            mutex.withLock {
                val channelId = getRandomChannel()

                val new = if ((0 until 2).random() == 0) {
                    val idsOfLast10Messages = rawMessages.values
                        .filter { it.id.channelId == channelId }
                        .filterIsInstance<RawMessage.Normal>()
                        .takeLast(2).map { it.id }
                    val id = idsOfLast10Messages.random()
                    RawMessage.Deleted(id)
                } else {
                    val newId = Message.Id(channelId = channelId, messageId = i)
                    RawMessage.Normal(newId)
                }

                rawMessages[new.id] = new
                emit(new)
            }
        }
    }.lag(3000).shareIn(GlobalScope, SharingStarted.Eagerly)

    private fun <T> Flow<T>.lag(delay: Long): Flow<T> {
        return object : Flow<T> {

            override suspend fun collect(collector: FlowCollector<T>) {
                val scope = CoroutineScope(coroutineContext)
                this@lag.collect() {
                    scope.launch {
                        if ((0..1).random() == 0) delay(delay)
                        collector.emit(it)
                    }
                }
            }

        }
    }

    private fun getRandomChannel(): Long = (0L..3L).random()

    companion object {

        private const val INITIAL_RAW_MESSAGES_COUNT = 1000
    }
}