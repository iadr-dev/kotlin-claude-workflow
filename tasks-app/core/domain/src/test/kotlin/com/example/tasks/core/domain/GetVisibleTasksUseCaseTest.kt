package com.example.tasks.core.domain

import com.example.tasks.core.testing.FakeSettingsRepository
import com.example.tasks.core.testing.FakeTaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetVisibleTasksUseCaseTest {
  private val open = Task(TaskId(2), "open", isCompleted = false)
  private val done = Task(TaskId(1), "done", isCompleted = true)

  @Test
  fun showsOpenTasksBeforeCompletedOnes() = runTest {
    val useCase = GetVisibleTasksUseCase(FakeTaskRepository(listOf(done, open)), FakeSettingsRepository())

    val visible = useCase().first()

    assertEquals(listOf(open, done), visible.tasks)
    assertEquals(0, visible.hiddenCount)
  }

  @Test
  fun hidesCompletedTasksAndCountsThem() = runTest {
    val useCase =
      GetVisibleTasksUseCase(FakeTaskRepository(listOf(done, open)), FakeSettingsRepository(hideCompleted = true))

    val visible = useCase().first()

    assertEquals(listOf(open), visible.tasks)
    assertEquals(1, visible.hiddenCount)
  }

  @Test
  fun reactsToSettingChanges() = runTest {
    val settings = FakeSettingsRepository(hideCompleted = true)
    val useCase = GetVisibleTasksUseCase(FakeTaskRepository(listOf(done, open)), settings)
    val emissions = mutableListOf<VisibleTasks>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(emissions) }

    settings.setHideCompleted(false)

    assertEquals(listOf(listOf(open), listOf(open, done)), emissions.map { it.tasks })
  }
}
