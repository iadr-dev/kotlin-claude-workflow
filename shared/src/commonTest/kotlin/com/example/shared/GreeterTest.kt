package com.example.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class GreeterTest {
    @Test
    fun greets() = assertEquals("Hello, KMP", Greeter().greet("KMP"))
}
