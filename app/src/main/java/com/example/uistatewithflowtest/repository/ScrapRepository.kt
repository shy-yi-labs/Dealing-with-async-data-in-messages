package com.example.uistatewithflowtest.repository

import com.example.uistatewithflowtest.outOf
import com.example.uistatewithflowtest.repository.message.Message
import kotlinx.coroutines.delay
import javax.inject.Singleton

data class Scrap(val value: Message.Id) {

    override fun toString(): String {
        return "📄 (${value.messageId})"
    }
}

@Singleton
class ScrapRepository(
    private val delayBy: Long
) {

    suspend fun get(id: Message.Id): Scrap? {
        delay(delayBy)
        if (1 outOf 2) delay(delayBy) // twice the delay, simulate lag
        return if (1 outOf 3) Scrap(id)  else null
    }
}