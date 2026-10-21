package com.example.tasks.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TasksScreen(viewModel: TasksViewModel, modifier: Modifier = Modifier) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (val current = state) {
    TasksUiState.Loading -> Unit
    is TasksUiState.Content ->
      TasksContent(
        state = current,
        onDraftChange = viewModel::onDraftChange,
        onAddTask = viewModel::onAddTask,
        onCompletedChange = viewModel::onCompletedChange,
        onHideCompletedChange = viewModel::onHideCompletedChange,
        modifier = modifier,
      )
  }
}

@Composable
internal fun TasksContent(
  state: TasksUiState.Content,
  onDraftChange: (String) -> Unit,
  onAddTask: () -> Unit,
  onCompletedChange: (id: Long, isCompleted: Boolean) -> Unit,
  onHideCompletedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(stringResource(R.string.tasks_title), style = MaterialTheme.typography.headlineMedium)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = state.draft,
        onValueChange = onDraftChange,
        label = { Text(stringResource(R.string.tasks_new_task)) },
        isError = state.draftError != null,
        supportingText = state.draftError?.let { error -> { Text(error.message()) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAddTask() }),
        modifier = Modifier.weight(1f).testTag("draft"),
      )
      Button(onClick = onAddTask, modifier = Modifier.testTag("add")) { Text(stringResource(R.string.tasks_add)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(stringResource(R.string.tasks_hide_completed), modifier = Modifier.weight(1f))
      Switch(
        checked = state.hideCompleted,
        onCheckedChange = onHideCompletedChange,
        modifier = Modifier.testTag("hideCompleted"),
      )
    }
    if (state.hiddenCount > 0) {
      Text(
        pluralStringResource(R.plurals.tasks_hidden_count, state.hiddenCount, state.hiddenCount),
        style = MaterialTheme.typography.bodySmall,
      )
    }
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
      items(state.tasks, key = { it.id }) { task -> TaskRow(task, onCompletedChange) }
    }
  }
}

@Composable
private fun TaskRow(task: TaskItemUi, onCompletedChange: (id: Long, isCompleted: Boolean) -> Unit) {
  val description =
    stringResource(if (task.isCompleted) R.string.tasks_mark_not_done else R.string.tasks_mark_done, task.title)
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Checkbox(
      checked = task.isCompleted,
      onCheckedChange = { onCompletedChange(task.id, it) },
      modifier = Modifier.semantics { contentDescription = description },
    )
    Text(
      text = task.title,
      textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
      color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun DraftError.message(): String =
  when (this) {
    DraftError.Blank -> stringResource(R.string.tasks_error_blank)
    is DraftError.TooLong -> stringResource(R.string.tasks_error_too_long, maxLength)
  }

@Preview(showBackground = true)
@Composable
private fun TasksContentPreview() {
  MaterialTheme {
    TasksContent(
      state =
        TasksUiState.Content(
          tasks = listOf(TaskItemUi(1, "Buy milk", isCompleted = false), TaskItemUi(2, "Walk dog", isCompleted = true)),
          hideCompleted = false,
          hiddenCount = 0,
          draft = "",
          draftError = DraftError.Blank,
        ),
      onDraftChange = {},
      onAddTask = {},
      onCompletedChange = { _, _ -> },
      onHideCompletedChange = {},
      modifier = Modifier.padding(16.dp),
    )
  }
}
