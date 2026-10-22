package com.example.tasks.feature.tasks

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TasksContentTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private val state =
    TasksUiState.Content(
      tasks = listOf(TaskItemUi(1, "Buy milk", isCompleted = false)),
      hideCompleted = false,
      hiddenCount = 0,
      completedCount = 0,
      openCount = 1,
      draft = "",
      draftError = DraftError.Blank,
    )

  @Test
  fun rendersTasksErrorAndForwardsEvents() {
    var completed: Pair<Long, Boolean>? = null
    var addClicks = 0
    compose.setContent {
      MaterialTheme {
        TasksContent(
          state = state,
          onDraftChange = {},
          onAddTask = { addClicks++ },
          onCompletedChange = { id, done -> completed = id to done },
          onHideCompletedChange = {},
          onClearCompleted = {},
        )
      }
    }

    compose.onNodeWithText("Buy milk").assertExists()
    compose.onNodeWithText("Enter a title").assertExists()
    compose.onNodeWithTag("hideCompleted").assertIsOff()

    compose.onNodeWithContentDescription("Mark \"Buy milk\" done").performClick()
    compose.onNodeWithTag("add").performClick()

    assertEquals(1L to true, completed)
    assertEquals(1, addClicks)
  }

  @Test
  fun showsOpenCountInTitle() {
    setContent(state.copy(openCount = 2))

    compose.onNodeWithText(compose.activity.getString(R.string.tasks_title_with_open, 2)).assertExists()
  }

  @Test
  fun showsZeroOpenWhenEverythingIsDone() {
    setContent(state.copy(openCount = 0, completedCount = 1))

    compose.onNodeWithText(compose.activity.getString(R.string.tasks_title_with_open, 0)).assertExists()
  }

  @Test
  fun clearCompletedIsDisabledWithoutCompletedTasks() {
    setContent(state.copy(completedCount = 0))

    compose.onNodeWithTag("clearCompleted").assertIsNotEnabled()
  }

  @Test
  fun clearCompletedForwardsClickWhenEnabled() {
    var clicks = 0
    setContent(state.copy(completedCount = 1), onClearCompleted = { clicks++ })

    compose.onNodeWithTag("clearCompleted").assertIsEnabled().performClick()

    assertEquals(1, clicks)
  }

  private fun setContent(content: TasksUiState.Content, onClearCompleted: () -> Unit = {}) {
    compose.setContent {
      MaterialTheme {
        TasksContent(
          state = content,
          onDraftChange = {},
          onAddTask = {},
          onCompletedChange = { _, _ -> },
          onHideCompletedChange = {},
          onClearCompleted = onClearCompleted,
        )
      }
    }
  }
}
