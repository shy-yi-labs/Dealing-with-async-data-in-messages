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
    val id: Int,
    val content: Int,
    val staticValue: String,
    val reaction: Flow<Reaction?>,
    val scrap: Flow<Scrap?>
)

class MessageRepository @Inject constructor(
    private val messageFactory: MessageFactory,
    private val rawMessageRepository: RawMessageRepository,
    private val reactionRepository: ReactionRepository,
) {

    private val mapFlow = OrderedMapFlow<Int, RawMessage>()

    private var messages: Flow<List<Message>>? = null

    var isFlowOpen = true
    var awaitInitialization = false

    suspend fun getMessages(): Flow<List<Message>> {
            return messages ?: run {
                init()

                mapFlow
                    .filter { isFlowOpen }
                    .map { it.values }
                    .map { rawMessages ->
                        with(messageFactory) {
                            rawMessages.toMessagess()
                        }
                    }
                    .onEach { if (awaitInitialization) it.await() }
            }.also {
                messages = it
            }
        }

    private suspend fun List<Message>.await() {
        forEach {
            it.reaction.first()
            it.scrap.first()
        }
    }

    private suspend fun init() {
        CoroutineScope(coroutineContext).launch {
            mapFlow.putAll(rawMessageRepository.fetchLatest(5).map { Pair(it.id, it) })

            launch {
                rawMessageRepository.pushes.collect {
                    mapFlow.put(it.id, it)
                }
            }

            launch {
                reactionRepository.collectPushes()
            }
        }
    }

    suspend fun clear() {
        mapFlow.clear()
    }
}