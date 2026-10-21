package com.example.tasks.core.data

import com.example.tasks.core.data.local.TaskLocalDataSource
import com.example.tasks.core.data.local.TaskRecord
import com.example.tasks.core.domain.Task
import com.example.tasks.core.domain.TaskId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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

  @Test
  fun clearCompletedRemovesOnlyCompletedTasks() = runTest {
    val local =
      TaskLocalDataSource(
        listOf(
          TaskRecord(id = 1, title = "open one", completedAtMillis = null),
          TaskRecord(id = 2, title = "done one", completedAtMillis = 10),
          TaskRecord(id = 3, title = "open two", completedAtMillis = null),
          TaskRecord(id = 4, title = "done two", completedAtMillis = 20),
        ),
      )
    val repository = DefaultTaskRepository(local, StandardTestDispatcher(testScheduler), clock = { 0 })

    repository.clearCompleted()

    assertEquals(listOf(1L, 3L), repository.observeTasks().first().map { it.id.value })
    assertEquals(listOf("open one", "open two"), repository.observeTasks().first().map { it.title })
  }

  @Test
  fun clearCompletedWithNothingCompletedIsNoOp() = runTest {
    val seed =
      listOf(
        TaskRecord(id = 1, title = "open one", completedAtMillis = null),
        TaskRecord(id = 2, title = "open two", completedAtMillis = null),
      )
    val repository = DefaultTaskRepository(TaskLocalDataSource(seed), StandardTestDispatcher(testScheduler), clock = { 0 })
    val before = repository.observeTasks().first()

    repository.clearCompleted()

    assertEquals(before, repository.observeTasks().first())
  }

  // Needs a real multi-threaded dispatcher: a single-queue test dispatcher cannot show a missing lock.
  // Sensitivity check: removing `writes.withLock` from clearCompleted makes this test fail.
  @Test
  fun clearCompletedWaitsForInFlightWrite() = runBlocking {
    // A deadlocked write path must fail the test, not hang the suite.
    withTimeout(5_000) {
      val entered = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      val local = TaskLocalDataSource(listOf(TaskRecord(id = 1, title = "task", completedAtMillis = null)))
      val repository =
        DefaultTaskRepository(
          local,
          Dispatchers.Default,
          clock = {
            entered.complete(Unit)
            runBlocking { release.await() }
            1L
          },
        )

      val completing = launch(Dispatchers.Default) { repository.setCompleted(TaskId(1), isCompleted = true) }
      entered.await()
      val clearing = launch(Dispatchers.Default) { repository.clearCompleted() }
      delay(50)
      release.complete(Unit)
      completing.join()
      clearing.join()

      assertEquals(emptyList<Task>(), repository.observeTasks().first())
    }
  }
}
