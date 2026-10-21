package com.example.tasks.core.domain

import com.example.tasks.core.testing.FakeTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTaskUseCaseTest {
  private val repository = FakeTaskRepository()
  private val addTask = AddTaskUseCase(repository)

  @Test
  fun trimsAndAddsValidTitle() = runTest {
    val result = addTask("  Buy milk  ")

    assertEquals("Buy milk", (result as AddTaskResult.Added).task.title)
    assertEquals(listOf("Buy milk"), repository.observeTasks().first().map { it.title })
  }

  @Test
  fun rejectsBlankTitleWithoutAdding() = runTest {
    assertEquals(AddTaskResult.Invalid(TitleError.Blank), addTask("   "))
    assertTrue(repository.observeTasks().first().isEmpty())
  }

  @Test
  fun acceptsTitleAtMaxLengthAndRejectsOneLonger() = runTest {
    val max = AddTaskUseCase.MAX_TITLE_LENGTH

    assertTrue(addTask("a".repeat(max)) is AddTaskResult.Added)
    assertEquals(AddTaskResult.Invalid(TitleError.TooLong(max)), addTask("a".repeat(max + 1)))
  }
}
