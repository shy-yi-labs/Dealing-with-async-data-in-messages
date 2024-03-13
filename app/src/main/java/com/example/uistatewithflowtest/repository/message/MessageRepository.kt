package com.example.uistatewithflowtest.repository.message

import com.example.uistatewithflowtest.OrderedMapFlow
import com.example.uistatewithflowtest.Reaction
import com.example.uistatewithflowtest.repository.RawMessage
import com.example.uistatewithflowtest.repository.RawMessageRepository
import com.example.uistatewithflowtest.repository.Scrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

data class Message(
    val id: Long,
    val channelId: Long,
    val content: Long,
    val staticValue: String,
    val reaction: Flow<Reaction?>,
    val scrap: Flow<Scrap?>
)

data class MessagesState(
    var allowPush: Boolean,
    var awaitInitialization: Boolean
)

@Singleton
class MessageRepository @Inject constructor(
    private val messageFactory: MessageFactory,
    private val rawMessageRepository: RawMessageRepository,
) {

    private data class MessagesKey(
        val channelId: Long,
        val extraKey: Long?,
    )

    private val messagesMap = mutableMapOf<MessagesKey, Flow<List<Message>>>()
    private val messagesStates = mutableMapOf<MessagesKey, MessagesState>()
    private val rawMessagesMap = mutableMapOf<MessagesKey, OrderedMapFlow<Long, RawMessage>>()

    suspend fun getMessages(
        channelId: Long,
        allowPush: Boolean,
        awaitInitialization: Boolean,
        extraKey: Long? = null,
    ): Flow<List<Message>> {
        val key = MessagesKey(channelId, extraKey)
        val messages = messagesMap[key]

        return messages ?: run {
            val messagesState = MessagesState(allowPush, awaitInitialization)
            val mapFlow = OrderedMapFlow<Long, RawMessage>()
            messagesStates[key] = messagesState
            rawMessagesMap[key] = mapFlow

            mapFlow
                .map { it.values }
                .map { rawMessages ->
                    with(messageFactory) {
                        rawMessages.toMessages()
                    }
                }
                .onEach { if (messagesState.awaitInitialization) it.await() }
        }.also {
            messagesMap[key] = it
        }
    }

    private suspend fun List<Message>.await() {
        forEach {
            it.reaction.first()
            it.scrap.first()
        }
    }

    private suspend fun OrderedMapFlow<Long, RawMessage>.init(messagesKey: MessagesKey) {
        CoroutineScope(coroutineContext).launch {
            putAll(rawMessageRepository.fetchLatest(messagesKey.channelId, 5).map { Pair(it.id, it) })

            val messagesState = messagesStates[messagesKey]
            rawMessageRepository.pushes
                .filter { messagesState?.allowPush ?: true }
                .filter { it.channelId == messagesKey.channelId }
                .collect {
                put(it.id, it)
            }
        }
    }

    suspend fun init(channelId: Long, extraKey: Long? = null, allowPush: Boolean = true) {
        val key = MessagesKey(channelId, extraKey)
        rawMessagesMap[key]?.init(key)
        messagesStates[key]?.allowPush = allowPush
    }

    suspend fun clear(channelId: Long, extraKey: Long? = null, allowPush: Boolean = false) {
        val key = MessagesKey(channelId, extraKey)
        rawMessagesMap[key]?.clear()
        messagesStates[key]?.allowPush = allowPush
    }

    fun getMessagesState(channelId: Long, extraKey: Long? = null): MessagesState? {
        return messagesStates[MessagesKey(channelId, extraKey)]
    }
}