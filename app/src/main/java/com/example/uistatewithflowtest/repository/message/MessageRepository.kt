package com.example.uistatewithflowtest.repository.message

import com.example.uistatewithflowtest.OrderedMapFlow
import com.example.uistatewithflowtest.Reaction
import com.example.uistatewithflowtest.repository.RawMessage
import com.example.uistatewithflowtest.repository.RawMessageRepository
import com.example.uistatewithflowtest.repository.Scrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

interface MessagesState {
    var allowPush: Boolean
    var awaitInitialization: Boolean
}

@Singleton
class MessageRepository @Inject constructor(
    private val messageFactory: MessageFactory,
    private val rawMessageRepository: RawMessageRepository,
) {

    private data class MessagesKey(
        val channelId: Long,
        val extraKey: Long?,
    )

    private inner class MessagesStateImpl: MessagesState {
        var initJob: Job? = null
        override var allowPush: Boolean = true
        override var awaitInitialization: Boolean = false
        val rawMessageMaps: OrderedMapFlow<Long, RawMessage> = OrderedMapFlow()

        val messages = rawMessageMaps
            .map { it.values }
            .map { rawMessages ->
                with(messageFactory) {
                    rawMessages.toMessages()
                }
            }
            .onEach { if (awaitInitialization) it.await() }
    }

    private val messagesStateMap = mutableMapOf<MessagesKey, MessagesStateImpl>()

    fun getMessages(
        channelId: Long,
        extraKey: Long? = null,
    ): Flow<List<Message>> {
        val key = MessagesKey(channelId, extraKey)
        val messageState = messagesStateMap[key] ?: run {
            MessagesStateImpl().also {
                messagesStateMap[key] = it
            }
        }

        return messageState.messages
    }

    private suspend fun List<Message>.await() {
        forEach {
            it.reaction.first()
            it.scrap.first()
        }
    }

    private suspend fun MessagesStateImpl.init(messagesKey: MessagesKey) {
        initJob = CoroutineScope(coroutineContext).launch {
            rawMessageMaps.putAll(
                rawMessageRepository.fetchLatest(messagesKey.channelId, 5).map { Pair(it.id, it) })

            rawMessageRepository.pushes
                .filter { allowPush }
                .filter { it.channelId == messagesKey.channelId }
                .collect {
                    rawMessageMaps.put(it.id, it)
                }
        }
    }

    suspend fun init(
        channelId: Long,
        extraKey: Long? = null,
        allowPush: Boolean = true,
        awaitInitialization: Boolean
    ) {
        val key = MessagesKey(channelId, extraKey)
        val messagesState = messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
        if (messagesState.initJob != null) throw IllegalStateException("Duplicate init call.")
        messagesState.allowPush = allowPush
        messagesState.awaitInitialization = awaitInitialization
        messagesState.init(key)
    }

    suspend fun clear(channelId: Long, extraKey: Long? = null, allowPush: Boolean = false) {
        val key = MessagesKey(channelId, extraKey)
        val messagesState = messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
        messagesState.allowPush = allowPush
        messagesState.rawMessageMaps.clear()
    }

    fun drop(channelId: Long, extraKey: Long? = null) {
        val key = MessagesKey(channelId, extraKey)
        val messagesState = messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
        messagesState.initJob?.cancel()
        messagesStateMap.remove(key)
    }

    fun getMessagesState(channelId: Long, extraKey: Long? = null): MessagesState {
        val key = MessagesKey(channelId, extraKey)
        return messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
    }

    private fun getGetMessagesNotCalledException(key: MessagesKey): IllegalStateException {
        return IllegalStateException("getMessages() must be called before calling this method for the given key: $key")
    }
}