package com.example.uistatewithflowtest.repository.message

import com.example.uistatewithflowtest.OrderedMapFlow
import com.example.uistatewithflowtest.Reaction
import com.example.uistatewithflowtest.repository.FetchType
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

    data class Key(
        val channelId: Long,
        val extraKey: Long?,
    )

    private inner class MessagesStateImpl: MessagesState {
        var pushJob: Job? = null
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

    private val messagesStateMap = mutableMapOf<Key, MessagesStateImpl>()

    suspend fun getMessages(key: Key): Flow<List<Message>> {
        val messageState = messagesStateMap[key] ?: run {
            MessagesStateImpl().also {
                it.init(key)
                messagesStateMap[key] = it
            }
        }

        return messageState.messages
    }

    private suspend fun MessagesStateImpl.init(key: Key) {
        pushJob = CoroutineScope(coroutineContext).launch {
            rawMessageRepository.pushes
                .filter { allowPush }
                .filter { it.channelId == key.channelId }
                .collect {
                    rawMessageMaps.put(it.id, it)
                }
        }
    }

    private suspend fun List<Message>.await() {
        forEach {
            it.reaction.first()
            it.scrap.first()
        }
    }

    suspend fun init(key: Key, count: Int) {
        val messagesState = messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
        messagesState.rawMessageMaps.putAll(
            rawMessageRepository.fetchLatest(key.channelId, count).map { Pair(it.id, it) }
        )
    }

    suspend fun fetch(
        key: Key,
        pivot: Long,
        count: Int,
        type: FetchType
    ) {
        val messagesState = messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
        messagesState.rawMessageMaps.putAll(
            rawMessageRepository.fetch(key.channelId, pivot, count, type).map { Pair(it.id, it) }
        )
    }

    suspend fun clear(key: Key) {
        val messagesState = messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
        messagesState.rawMessageMaps.clear()
    }

    fun drop(key: Key) {
        val messagesState = messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
        messagesState.pushJob?.cancel()
        messagesStateMap.remove(key)
    }

    fun getMessagesState(key: Key): MessagesState {
        return messagesStateMap[key]
            ?: throw getGetMessagesNotCalledException(key)
    }

    private fun getGetMessagesNotCalledException(key: Key): IllegalStateException {
        return IllegalStateException("getMessages() must be called before calling this method for the given key: $key")
    }
}