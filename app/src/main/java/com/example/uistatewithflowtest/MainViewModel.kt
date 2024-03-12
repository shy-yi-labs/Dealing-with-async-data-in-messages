package com.example.uistatewithflowtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uistatewithflowtest.repository.ManualReactionPushDataSource
import com.example.uistatewithflowtest.repository.message.Message
import com.example.uistatewithflowtest.repository.message.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val messages: List<Message> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    messageRepository: MessageRepository,
    private val manualReactionPushDataSource: ManualReactionPushDataSource,
) : ViewModel() {

    @OptIn(FlowPreview::class)
    val uiState = flow {
        val messages = messageRepository
            .getMessages(
                channelId = 0,
                allowPush = true,
                awaitInitialization = false
            )
            .map { MainUiState(it) }
        emit(messages)
    }
        .flattenConcat()
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    fun triggerNewReactionEvent(reactionEvent: ReactionEvent) {
        viewModelScope.launch {
            manualReactionPushDataSource.send(reactionEvent)
        }
    }
}