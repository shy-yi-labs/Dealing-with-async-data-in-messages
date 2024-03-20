package com.example.uistatewithflowtest.repository

import com.example.uistatewithflowtest.Reaction
import com.example.uistatewithflowtest.ReactionEvent
import com.example.uistatewithflowtest.repository.message.Message
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReactionPullDataSource(
    private val getDelay: Long = 1000,
) {

    suspend fun get(ids: List<Message.Id>): Map<Message.Id, Reaction?> {
        delay(getDelay)
        return ids.associateWith { if ((0 until 2).random() == 0) Reaction.random() else null }
    }
}

interface ReactionPushDataSource {

    val pushEvents: Flow<ReactionEvent>
}

@Singleton
class ManualReactionPushDataSource @Inject constructor(): ReactionPushDataSource {

    private val pushEventChannel = Channel<ReactionEvent>()
    override val pushEvents: Flow<ReactionEvent> = pushEventChannel.consumeAsFlow()

    suspend fun send(push: ReactionEvent) {
        pushEventChannel.send(push)
    }
}