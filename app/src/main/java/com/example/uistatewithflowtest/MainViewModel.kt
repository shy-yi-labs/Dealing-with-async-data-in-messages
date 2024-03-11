package com.example.uistatewithflowtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uistatewithflowtest.repository.ManualReactionPushDataSource
import com.example.uistatewithflowtest.repository.RawMessage
import com.example.uistatewithflowtest.repository.RawMessageRepository
import com.example.uistatewithflowtest.repository.Scrap
import com.example.uistatewithflowtest.repository.ScrapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

data class MainUiState(
    val messages: List<Message> = emptyList()
)

data class Message(
    val id: Int,
    val content: Int,
    val staticValue: String,
    val reaction: Flow<Reaction?>,
    val scrap: Flow<Scrap?>
)

class MessageFactory @Inject constructor(
    private val reactionRepository: ReactionRepository,
    private val scrapRepository: ScrapRepository
) {
    private val messageCacheStore = mutableMapOf<Int, Pair<RawMessage, Message>>()

    suspend fun Collection<RawMessage>.toItems(): List<Message> {
        val rawMessagesNotInCache = this.filter { rawItem ->
            val cache = messageCacheStore[rawItem.id]
            // Is in cache and rawItem is equal to old rawItem
            return@filter (cache != null && rawItem == cache.first).not()
        }

        reactionRepository.fetch(rawMessagesNotInCache.map { it.id })

        rawMessagesNotInCache.forEach {
            messageCacheStore[it.id] = it to it.toItem()
        }

        return this.map { messageCacheStore[it.id]!!.second }
    }

    private suspend fun RawMessage.toItem(): Message {
        return Message(
            id = id,
            content = id,
            staticValue = value,
            reaction = reactionRepository.get(id),
            scrap = flow {
                emit(scrapRepository.get(id))
            }.shareIn(CoroutineScope(coroutineContext), SharingStarted.Eagerly, 1)
        )
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val rawMessageRepository: RawMessageRepository,
    private val reactionRepository: ReactionRepository,
    private val messageFactory: MessageFactory,
    private val manualReactionPushDataSource: ManualReactionPushDataSource,
) : ViewModel() {

    private val rawItemsFlow = OrderedMapFlow<Int, RawMessage>()

    val uiState: StateFlow<MainUiState> = rawItemsFlow
        .map { it.values }
        .map { rawItems ->
            with(messageFactory) {
                MainUiState(messages = rawItems.toItems())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    init {
        viewModelScope.launch {
            rawItemsFlow.putAll(rawMessageRepository.fetchLatest(5).map { Pair(it.id, it) })

            launch {
                rawMessageRepository.pushes.collect {
                    rawItemsFlow.put(it.id, it)
                }
            }

            launch {
                reactionRepository.collectPushes()
            }
        }
    }

    fun triggerNewReactionEvent(reactionEvent: ReactionEvent) {
        viewModelScope.launch {
            manualReactionPushDataSource.send(reactionEvent)
        }
    }
}