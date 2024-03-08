package com.example.uistatewithflowtest.repository

import kotlinx.coroutines.delay

data class Scrap(val value: Int) {

    override fun toString(): String {
        return "Scrap($value)"
    }
}

class ScrapRepository(
    private val delayBy: Long = 1000
) {

    suspend fun get(id: Int): Scrap? {
        delay(delayBy)
        return if ((0 until 4).random() == 0) Scrap(id)  else null
    }
}