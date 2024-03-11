package com.example.uistatewithflowtest

import com.example.uistatewithflowtest.repository.Reaction
import com.example.uistatewithflowtest.repository.ReactionEvent
import com.example.uistatewithflowtest.repository.ReactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class ReactionManager(
    private val reactionRepository: ReactionRepository
) {
    private val mapFlow = OrderedMapFlow<Int, Reaction>()
    private val reactions: Flow<Map<Int, Reaction>> = mapFlow

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

    fun get(id: Int): Flow<Reaction?> {
        return reactions.map { it[id] }
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