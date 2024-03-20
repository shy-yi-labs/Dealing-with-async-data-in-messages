package com.example.uistatewithflowtest.repository.message

import com.example.uistatewithflowtest.ReactionRepository
import com.example.uistatewithflowtest.repository.RawMessage
import com.example.uistatewithflowtest.repository.ScrapRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class MessageFactory @Inject constructor(
    private val reactionRepository: ReactionRepository,
    private val scrapRepository: ScrapRepository
) {
    private val messageCacheStore = mutableMapOf<Message.Id, Pair<RawMessage, Message>>()

    suspend fun Collection<RawMessage>.toMessages(): List<Message> {
        val rawMessagesNotInCache = this.filter { rawMessage ->
            val cache = messageCacheStore[rawMessage.id]
            // Is in cache and rawItem is equal to old rawItem
            return@filter cache == null || cache.first != rawMessage
        }

        reactionRepository.fetch(rawMessagesNotInCache.map { it.id })

        rawMessagesNotInCache.forEach {
            val newItem = it.toItem()
            if (newItem == null) {
                messageCacheStore.remove(it.id)
            } else {
                messageCacheStore[it.id] = it to newItem
            }
        }

        return this.mapNotNull { messageCacheStore[it.id]?.second }
    }

    private suspend fun RawMessage.toItem(): Message? {
        return when(this) {
            is RawMessage.Deleted -> null
            is RawMessage.Normal -> Message(
                id = id,
                text = text,
                reaction = reactionRepository.get(id),
                scrap = flow {
                    emit(scrapRepository.get(id))
                }.shareIn(CoroutineScope(coroutineContext), SharingStarted.Eagerly, 1)
            )
        }
    }
}