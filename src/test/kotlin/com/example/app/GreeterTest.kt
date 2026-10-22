package com.example.app

import kotlin.test.Test
import kotlin.test.assertEquals

class GreeterTest {
    @Test
    fun greets() {
        assertEquals("Hello, Kotlin", Greeter().greet("Kotlin"))
    }
}
