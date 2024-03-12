package com.example.uistatewithflowtest.repository.message

import com.example.uistatewithflowtest.OrderedMapFlow
import com.example.uistatewithflowtest.Reaction
import com.example.uistatewithflowtest.ReactionRepository
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
import kotlin.coroutines.coroutineContext

data class Message(
    val id: Long,
    val content: Long,
    val staticValue: String,
    val reaction: Flow<Reaction?>,
    val scrap: Flow<Scrap?>
)

data class MessagesState(
    var allowPush: Boolean,
    var awaitInitialization: Boolean
)

class MessageRepository @Inject constructor(
    private val messageFactory: MessageFactory,
    private val rawMessageRepository: RawMessageRepository,
    private val reactionRepository: ReactionRepository,
) {

    private val messagesMap = mutableMapOf<Long, Flow<List<Message>>>()
    private val messagesStates = mutableMapOf<Long, MessagesState>()
    private val rawMessagesMap = mutableMapOf<Long, OrderedMapFlow<Long, RawMessage>>()

    suspend fun getMessages(
        channelId: Long,
        allowPush: Boolean,
        awaitInitialization: Boolean
    ): Flow<List<Message>> {
        val messages = messagesMap[channelId]

        return messages ?: run {
            val mapFlow = OrderedMapFlow<Long, RawMessage>()
            val messagesState = MessagesState(allowPush, awaitInitialization)
            messagesStates[channelId] = messagesState
            rawMessagesMap[channelId] = mapFlow

            mapFlow.init()

            mapFlow
                .filter { messagesState.allowPush }
                .map { it.values }
                .map { rawMessages ->
                    with(messageFactory) {
                        rawMessages.toMessages()
                    }
                }
                .onEach { if (messagesState.awaitInitialization) it.await() }
        }.also {
            messagesMap[channelId] = it
        }
    }

    private suspend fun List<Message>.await() {
        forEach {
            it.reaction.first()
            it.scrap.first()
        }
    }

    private suspend fun OrderedMapFlow<Long, RawMessage>.init() {
        CoroutineScope(coroutineContext).launch {
            putAll(rawMessageRepository.fetchLatest(5).map { Pair(it.id, it) })

            launch {
                rawMessageRepository.pushes.collect {
                    put(it.id, it)
                }
            }

            launch {
                reactionRepository.collectPushes()
            }
        }
    }

    suspend fun clear(channelId: Long) {
        rawMessagesMap[channelId]?.clear()
    }

    fun getMessagesState(channelId: Long): MessagesState? {
        return messagesStates[channelId]
    }
}