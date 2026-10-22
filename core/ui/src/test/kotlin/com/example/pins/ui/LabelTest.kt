package com.example.pins.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LabelTest {
  @Test fun upperCases() = assertEquals("HELLO, RAY!", label("Ray"))
}
