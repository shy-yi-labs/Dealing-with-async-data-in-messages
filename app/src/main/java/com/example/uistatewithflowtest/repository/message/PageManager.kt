package com.example.uistatewithflowtest.repository.message

import android.util.Log
import com.example.uistatewithflowtest.OrderedMapFlow
import com.example.uistatewithflowtest.repository.Page
import com.example.uistatewithflowtest.repository.RawMessage
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PageManager {

    private val mutex = Mutex()
    var lastMessageId: Long? = null
        private set

    private val rawMessageMaps: OrderedMapFlow<Message.Id, RawMessage> = OrderedMapFlow { old, new ->
        Log.d("MessageRepository", new.toString())
        if (old.updateAt < new.updateAt) {
            true
        } else {
            Log.d("MessageRepository", "Reject! $old rejected $new")
            false
        }
    }

    val rawMessages = rawMessageMaps.map { it.values }

    suspend fun put(page: Page) {
        mutex.withLock {
            lastMessageId = page.lastMessageId
            rawMessageMaps.putAll(page.messages.map { Pair(it.id, it) })
        }
    }

    suspend fun clear() {
        mutex.withLock {
            lastMessageId = null
            rawMessageMaps.clear()
        }
    }

    suspend fun push(rawMessage: RawMessage): Boolean {
        return mutex.withLock {
            when (rawMessage) {
                is RawMessage.Deleted -> {
                    rawMessageMaps.put(rawMessage.id, rawMessage)
                    true
                }
                is RawMessage.Normal -> {
                    if (rawMessageMaps.map.keys.last().messageId == lastMessageId) {
                        lastMessageId = rawMessage.id.messageId
                        rawMessageMaps.put(rawMessage.id, rawMessage)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
}