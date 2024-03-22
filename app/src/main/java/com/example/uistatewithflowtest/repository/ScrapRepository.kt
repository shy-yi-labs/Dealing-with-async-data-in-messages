package com.example.uistatewithflowtest.repository

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
    private val delayBy: Long = 1000
) {

    suspend fun get(id: Message.Id): Scrap? {
        delay(delayBy)
        return if ((0 until 4).random() == 0) Scrap(id)  else null
    }
}