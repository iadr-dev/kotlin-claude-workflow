package com.example.pins.domain

public fun greeting(name: String): String = "Hello, ${name.trim().ifEmpty { "world" }}!"
