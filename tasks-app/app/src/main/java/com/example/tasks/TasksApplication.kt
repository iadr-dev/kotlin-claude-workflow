package com.example.tasks

import android.app.Application

class TasksApplication : Application() {
  val container: AppContainer by lazy { AppContainer() }
}
