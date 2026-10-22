package com.example.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LabelsTest {
    @Test
    fun shouts() {
        assertEquals("HI", Labels().shout("hi"))
    }
}
