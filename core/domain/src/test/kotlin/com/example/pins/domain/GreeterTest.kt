package com.example.pins.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GreeterTest {
  @Test fun blankNameFallsBackToWorld() = assertEquals("Hello, world!", greeting("  "))
  @Test fun namedGreeting() = assertEquals("Hello, Ray!", greeting(" Ray "))
}
