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
    private val messageCacheStore = mutableMapOf<Long, Pair<RawMessage, Message>>()

    suspend fun Collection<RawMessage>.toMessagess(): List<Message> {
        val rawMessagesNotInCache = this.filter { rawMessage ->
            val cache = messageCacheStore[rawMessage.id]
            // Is in cache and rawItem is equal to old rawItem
            return@filter (cache != null && rawMessage == cache.first).not()
        }

        reactionRepository.fetch(rawMessagesNotInCache.map { it.id })

        rawMessagesNotInCache.forEach {
            messageCacheStore[it.id] = it to it.toItem()
        }

        return this.map { messageCacheStore[it.id]!!.second }
    }

    private suspend fun RawMessage.toItem(): Message {
        return Message(
            id = id,
            content = id,
            staticValue = value,
            reaction = reactionRepository.get(id),
            scrap = flow {
                emit(scrapRepository.get(id))
            }.shareIn(CoroutineScope(coroutineContext), SharingStarted.Eagerly, 1)
        )
    }
}