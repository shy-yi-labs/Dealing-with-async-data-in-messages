package com.example.uistatewithflowtest.repository.message

import android.util.Log
import com.example.uistatewithflowtest.OrderedMapFlow
import com.example.uistatewithflowtest.repository.RawMessage
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class PageManager {

    private val mutex = Mutex()

    private var localLastMessageId = LastMessageIdTracker()
    private var remoteLastMessageId = LastMessageIdTracker()

    val hasLatestMessage get() = localLastMessageId.isIdNull.not() && localLastMessageId == remoteLastMessageId

    private val rawMessageMaps: OrderedMapFlow<Message.Id, RawMessage> = OrderedMapFlow { old, new ->
        Log.d("PageManager", "OVERRIDE: $new")
        if (old.updateAt < new.updateAt) {
            true
        } else {
            Log.d("PageManager", "OVERRIDE REJECTED: $old rejected $new")
            false
        }
    }

    val rawMessages = rawMessageMaps.map { it.values }

    suspend fun put(page: RawMessage.Page) {
        mutex.withLock {
            localLastMessageId.update(page.messages.maxOfOrNull { it.id.messageId })
            remoteLastMessageId.update(page.lastMessageId)
            rawMessageMaps.putAll(page.messages.map { Pair(it.id, it) })
        }
    }

    suspend fun clear() {
        mutex.withLock {
            localLastMessageId.clear()
            remoteLastMessageId.clear()
            rawMessageMaps.clear()
        }
    }

    suspend fun push(rawMessage: RawMessage): Boolean {
        return mutex.withLock {
            val result = when (rawMessage) {
                is RawMessage.Deleted -> {
                    rawMessageMaps.put(rawMessage.id, rawMessage)
                    true
                }
                is RawMessage.Normal -> {
                    if (hasLatestMessage) {
                        rawMessageMaps.put(rawMessage.id, rawMessage)
                        localLastMessageId.update(rawMessage.id.messageId)
                        remoteLastMessageId.update(rawMessage.id.messageId)
                        true
                    } else {
                        false
                    }
                }
            }

            Log.d("PageManager", "PUSH: $rawMessage")
            result
        }
    }
}

private data class LastMessageIdTracker(private var id: Long? = null) {

    private val mutex = Mutex()

    val isIdNull get() = id == null

    suspend fun update(newId: Long?) {
        if (newId == null) return
        mutex.withLock {
            id = max(id ?: 0L, newId)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            id = null
        }
    }
}