package com.example.uistatewithflowtest

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class ItemRepository(
    private val itemCount: Int = 10,
    private val emitInterval: Long = 3000
) {

    val items = flow {
        repeat(itemCount) {
            emit((0 .. it).toList())
            delay(emitInterval)
        }
    }
}