package com.example.uistatewithflowtest

import com.example.uistatewithflowtest.repository.ReactionRepository
import kotlinx.coroutines.flow.Flow

class ReactionManager(
    private val reactionRepository: ReactionRepository
) {

    private val mapFlow = OrderedMapFlow<Int, Int>()
    val reactions: Flow<Map<Int, Int>> = mapFlow


}