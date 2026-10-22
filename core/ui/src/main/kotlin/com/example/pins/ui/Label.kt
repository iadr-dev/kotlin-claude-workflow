package com.example.pins.ui

import com.example.pins.domain.greeting

public fun label(name: String): String = greeting(name).uppercase()
