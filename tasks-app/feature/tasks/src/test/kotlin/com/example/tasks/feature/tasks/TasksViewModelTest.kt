package com.example.tasks.feature.tasks

import com.example.tasks.core.domain.AddTaskUseCase
import com.example.tasks.core.domain.GetVisibleTasksUseCase
import com.example.tasks.core.domain.Task
import com.example.tasks.core.domain.TaskId
import com.example.tasks.core.testing.FakeSettingsRepository
import com.example.tasks.core.testing.FakeTaskRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
  private val tasks = FakeTaskRepository(listOf(Task(TaskId(1), "existing", isCompleted = false)))
  private val settings = FakeSettingsRepository()
  private lateinit var viewModel: TasksViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    viewModel = TasksViewModel(GetVisibleTasksUseCase(tasks, settings), AddTaskUseCase(tasks), tasks, settings)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun content() = viewModel.uiState.value as TasksUiState.Content

  @Test
  fun addingValidDraftAddsTaskAndClearsDraft() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

    viewModel.onDraftChange("Buy milk")
    viewModel.onAddTask()

    assertEquals(listOf("existing", "Buy milk"), content().tasks.map { it.title })
    assertEquals("", content().draft)
    assertNull(content().draftError)
  }

  @Test
  fun blankDraftShowsErrorUntilEdited() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

    viewModel.onDraftChange("  ")
    viewModel.onAddTask()
    assertEquals(DraftError.Blank, content().draftError)

    viewModel.onDraftChange("  x")
    assertNull(content().draftError)
  }

  @Test
  fun doubleSubmitWhileAddInFlightAddsOnce() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    val gate = CompletableDeferred<Unit>().also { tasks.addGate = it }

    viewModel.onDraftChange("Buy milk")
    viewModel.onAddTask()
    viewModel.onAddTask()
    gate.complete(Unit)

    assertEquals(listOf("existing", "Buy milk"), content().tasks.map { it.title })
  }

  @Test
  fun textTypedWhileAddInFlightIsKept() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    val gate = CompletableDeferred<Unit>().also { tasks.addGate = it }

    viewModel.onDraftChange("Buy milk")
    viewModel.onAddTask()
    viewModel.onDraftChange("Walk dog")
    gate.complete(Unit)

    assertEquals("Walk dog", content().draft)
    assertEquals(listOf("existing", "Buy milk"), content().tasks.map { it.title })
  }

  @Test
  fun tooLongDraftMapsToTooLongError() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

    viewModel.onDraftChange("a".repeat(AddTaskUseCase.MAX_TITLE_LENGTH + 1))
    viewModel.onAddTask()

    assertEquals(DraftError.TooLong(AddTaskUseCase.MAX_TITLE_LENGTH), content().draftError)
  }

  @Test
  fun clearCompletedDeletesCompletedTasksAndRemovesHiddenLabel() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    tasks.addTask("open")
    viewModel.onCompletedChange(id = 1, isCompleted = true)
    viewModel.onHideCompletedChange(true)
    assertEquals(1, content().hiddenCount)

    viewModel.onClearCompleted()

    assertEquals(listOf("open"), content().tasks.map { it.title })
    assertEquals(0, content().hiddenCount)
    assertEquals(0, content().completedCount)
  }

  @Test
  fun openCountFollowsAddedAndCompletedTasks() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    assertEquals(1, content().openCount)

    tasks.addTask("second")
    assertEquals(2, content().openCount)

    viewModel.onCompletedChange(id = 1, isCompleted = true)
    assertEquals(1, content().openCount)

    viewModel.onClearCompleted()
    assertEquals(1, content().openCount)

    viewModel.onCompletedChange(id = 2, isCompleted = true)
    assertEquals(0, content().openCount)
  }

  @Test
  fun completedCountCountsCompletedTasksWhetherOrNotHidden() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    viewModel.onCompletedChange(id = 1, isCompleted = true)
    assertEquals(1, content().completedCount)

    viewModel.onHideCompletedChange(true)

    assertEquals(1, content().completedCount)
    assertEquals(1, content().hiddenCount)
  }

  @Test
  fun completingWhileHidingRemovesTaskAndCountsIt() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

    viewModel.onHideCompletedChange(true)
    viewModel.onCompletedChange(id = 1, isCompleted = true)

    assertEquals(emptyList<TaskItemUi>(), content().tasks)
    assertEquals(1, content().hiddenCount)
  }
}
