package com.example.uistatewithflowtest

import kotlinx.coroutines.delay

class IndividualEmitRepository(
    private val delayBy: Long = 1000
) {

    suspend fun get(id: Int): Int? {
        delay(delayBy)
        return if (id % 3 == 0) id.repeat(2)  else null
    }

    private fun Int.repeat(n: Int): Int {
        return toString().repeat(n).toInt()
    }
}