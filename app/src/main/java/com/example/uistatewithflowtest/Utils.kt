package com.example.uistatewithflowtest

infix fun Int.outOf(value: Int): Boolean {
    return (0 until value).random() in (0 until this)
}