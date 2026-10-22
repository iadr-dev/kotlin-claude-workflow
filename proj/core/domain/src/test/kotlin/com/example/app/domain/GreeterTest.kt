package com.example.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GreeterTest {
    @Test
    fun greets() {
        assertEquals("Hello, Ray", Greeter().greet("Ray"))
    }
}
