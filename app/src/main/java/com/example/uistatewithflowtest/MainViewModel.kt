package com.example.uistatewithflowtest

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uistatewithflowtest.repository.FetchType
import com.example.uistatewithflowtest.repository.ManualReactionPushDataSource
import com.example.uistatewithflowtest.repository.message.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val manualReactionPushDataSource: ManualReactionPushDataSource,
) : ViewModel() {

    private val around = savedStateHandle.get<Long>(ARG_AROUND)

    private val key = MessageRepository.Key(
        channelId = savedStateHandle.get<Long>(ARG_CHANNEL_ID) ?: 0L,
        extraKey = this.hashCode().toLong()
    )

    private val isFetchInProgress = AtomicBoolean(false)

    private val mutex = Mutex()
    private var scrollToId: Long? = null

    val messages = messageRepository
        .getMessages(key)
        .onEach { messages ->
            mutex.withLock {
                if (scrollToId != null) {
                    val index = messages.indexOfFirst { scrollToId == it.id.messageId }

                    if (-1 < index) {
                        lazyListState.scrollToItem(index)
                        Log.d("MainViewModel", "SCROLL: channelId=${key.channelId}, scroll to $scrollToId")

                    }
                    scrollToId = null
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val messagesState by lazy { messageRepository.getMessagesState(key) }

    val lazyListState = LazyListState()

    init {
        if (around == null) {
            fetchLatest()
        } else {
            fetch(around, FetchType.Around)
        }
    }

    fun triggerNewReactionEvent(reactionEvent: ReactionEvent) {
        viewModelScope.launch {
            manualReactionPushDataSource.send(reactionEvent)
        }
    }

    fun fetchLatest() {
        viewModelScope.launch {
            messageRepository.fetchLatest(key, MESSAGE_FETCH_COUNT_UNIT)
        }
    }

    fun fetch(pivot: Long, type: FetchType) {
        viewModelScope.launch {
            if (isFetchInProgress.compareAndSet(false, true)) {
                messageRepository.fetch(
                    key = key,
                    pivot = pivot,
                    count = MESSAGE_FETCH_COUNT_UNIT,
                    type = type
                )
                if (type == FetchType.Around) {
                    mutex.withLock {
                        scrollToId = pivot
                    }
                }
                isFetchInProgress.set(false)
            }
        }
    }

    fun clearMessages() {
        viewModelScope.launch(NonCancellable) {
            messageRepository.clear(key)
        }
    }

    fun dropMessages() {
        messageRepository.drop(key)
    }

    override fun onCleared() {
        super.onCleared()

        dropMessages()
    }

    companion object {
        const val ARG_CHANNEL_ID = "argChannelId"
        const val ARG_AROUND = "argAround"

        private val MESSAGE_FETCH_COUNT_UNIT = 50
    }
}