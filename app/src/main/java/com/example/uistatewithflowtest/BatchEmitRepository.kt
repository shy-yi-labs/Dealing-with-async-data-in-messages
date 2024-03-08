package com.example.uistatewithflowtest

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BatchEmitRepository(
    private val delayBy: Long = 1000
) {

    suspend fun get(ids: List<Int>): Map<Int, Int?> {
        delay(delayBy)
        return ids.associateWith { if (it % 5 == 0) null else it * it }
    }
}