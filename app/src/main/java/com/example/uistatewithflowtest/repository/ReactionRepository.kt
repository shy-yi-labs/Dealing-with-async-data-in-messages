package com.example.uistatewithflowtest.repository

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

data class Reaction(val value: Int) {

    companion object {
        fun random(): Reaction {
            return Reaction((0..9).random())
        }
    }

    override fun toString(): String {
        return "Reaction($value)"
    }
}

sealed class ReactionEvent {

    data class Insert(val targetId: Int, val reaction: Reaction): ReactionEvent()

    data class Update(val targetId: Int, val reaction: Reaction): ReactionEvent()

    data class Delete(val targetId: Int): ReactionEvent()
}

@OptIn(DelicateCoroutinesApi::class)
class ReactionRepository(
    private val getDelay: Long = 1000,
    private val pushInterval: Long = 2000,
    private val pushTargetIdsRange: IntRange = 0 .. 100,
) {

    private val pushEventChannel = Channel<ReactionEvent>()
    val pushEvents: Flow<ReactionEvent> = pushEventChannel.consumeAsFlow()

    suspend fun get(ids: List<Int>): Map<Int, Reaction?> {
        delay(getDelay)
        return ids.associateWith { if ((0 until 2).random() == 0) Reaction.random() else null }
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                delay(pushInterval)
                val targetId = pushTargetIdsRange.random()
                val event = when((0 .. 2).random()) {
                    0 -> ReactionEvent.Insert(targetId, Reaction.random())
                    1 -> ReactionEvent.Update(targetId, Reaction.random())
                    else -> ReactionEvent.Delete(targetId)
                }

                launch { pushEventChannel.send(event) }
            }
        }
    }
}