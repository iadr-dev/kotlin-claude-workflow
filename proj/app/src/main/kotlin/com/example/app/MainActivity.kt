package com.example.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.example.app.domain.Greeter

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = Greeter().greet("Android") })
    }
}
