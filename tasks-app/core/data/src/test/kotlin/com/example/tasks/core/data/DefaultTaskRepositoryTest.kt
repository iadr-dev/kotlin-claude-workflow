package com.example.tasks.core.data

import com.example.tasks.core.data.local.TaskLocalDataSource
import com.example.tasks.core.data.local.TaskRecord
import com.example.tasks.core.domain.Task
import com.example.tasks.core.domain.TaskId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultTaskRepositoryTest {
  @Test
  fun mapsRecordsToDomainTasks() = runTest {
    val local = TaskLocalDataSource(listOf(TaskRecord(id = 7, title = "seeded", completedAtMillis = 99)))
    val repository = DefaultTaskRepository(local, StandardTestDispatcher(testScheduler), clock = { 0 })

    assertEquals(listOf(Task(TaskId(7), "seeded", isCompleted = true)), repository.observeTasks().first())
  }

  @Test
  fun addAssignsIncreasingIdsAndCompletionUsesClock() = runTest {
    val local = TaskLocalDataSource(listOf(TaskRecord(id = 3, title = "seeded", completedAtMillis = null)))
    val repository = DefaultTaskRepository(local, StandardTestDispatcher(testScheduler), clock = { 1234 })

    val added = repository.addTask("new")
    repository.setCompleted(added.id, isCompleted = true)

    assertEquals(TaskId(4), added.id)
    assertEquals(1234L, local.observe().first().single { it.id == 4L }.completedAtMillis)

    repository.setCompleted(added.id, isCompleted = false)
    assertEquals(null, local.observe().first().single { it.id == 4L }.completedAtMillis)
  }
}
