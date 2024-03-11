package com.example.uistatewithflowtest

import com.example.uistatewithflowtest.repository.Reaction
import com.example.uistatewithflowtest.repository.ReactionEvent
import com.example.uistatewithflowtest.repository.ReactionRepository
import kotlinx.coroutines.flow.Flow

class ReactionManager(
    private val reactionRepository: ReactionRepository
) {
    private val mapFlow = OrderedMapFlow<Int, Reaction>()
    val reactions: Flow<Map<Int, Reaction>> = mapFlow

    suspend fun collectPushes() {
        reactionRepository.pushEvents.collect { event ->
            when(event) {
                is ReactionEvent.Delete -> {
                    mapFlow.delete(event.targetId)
                }
                is ReactionEvent.Insert -> {
                    mapFlow.put(event.targetId, event.reaction)
                }
                is ReactionEvent.Update -> {
                    mapFlow.put(event.targetId, event.reaction)
                }
            }
        }
    }

    suspend fun fetch(ids: List<Int>) {
        val pairs = reactionRepository
            .get(ids)
            .mapNotNull { (id, reaction) ->
                reaction?.let { Pair(id, reaction) }
            }
        mapFlow.putAll(pairs)
    }
}