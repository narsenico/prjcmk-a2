@file:OptIn(ExperimentalMaterial3Api::class)

package it.amonshore.comikkua.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import it.amonshore.comikkua.BuildConfig
import it.amonshore.comikkua.R
import it.amonshore.comikkua.ui.toHumanReadable
import java.time.LocalDate
import java.time.ZonedDateTime

class StatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel: StatsViewModel by viewModels()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val counterState by viewModel.counterState
                    .collectAsStateWithLifecycle()

                val importState by viewModel.importFromPreviousVersionState
                    .collectAsStateWithLifecycle()

                val exportBackupState by viewModel.exportBackupState
                    .collectAsStateWithLifecycle()

                Mdc3Theme {
                    Column(Modifier.fillMaxSize()) {
                        CounterCard(
                            version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            state = counterState,
                            onDeleteAllCLick = viewModel::deleteAll,
                        )
                        WorkerCard(
                            text = stringResource(id = R.string.import_old_database),
                            state = importState,
                            onRequestExecution = viewModel::importFromPreviousVersion
                        )
                        WorkerCard(
                            text = stringResource(id = R.string.export_backup),
                            state = exportBackupState,
                            onRequestExecution = viewModel::exportBackup
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CounterCard(
    version: String,
    state: StatsCounterState,
    onDeleteAllCLick: () -> Unit,
) {
    var showConfirmDialog by rememberSaveable {
        mutableStateOf(false)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            StatsCounterRow(
                label = stringResource(id = R.string.version),
                value = version
            )
            Divider()
            StatsCounterRow(
                label = stringResource(id = R.string.stats_comics_count),
                value = state.comicsCount.toString()
            )
            Divider()
            StatsCounterRow(
                label = stringResource(id = R.string.stats_last_update),
                value = toHumanReadable(state.lastUpdate)
            )
            Divider()
            TextButton(
                enabled = !state.isLoading,
                onClick = { showConfirmDialog = true }
            ) {
                Text(stringResource(id = R.string.delete_all))
            }
            if (!state.error.isNullOrEmpty()) {
                Text(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    text = state.error
                )
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmDialog(
            title = stringResource(id = R.string.confirm_title),
            message = stringResource(
                id = R.string.confirm_delete_comics_with_confirm_phrase,
                "DELETE"
            ),
            confirmPhrase = "DELETE",
            onConfirm = onDeleteAllCLick,
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
fun WorkerCard(
    text: String,
    state: StatsWorkerState,
    onRequestExecution: () -> Unit,
) {
    val enabled = state !is StatsWorkerState.Running
    val message = when (state) {
        is StatsWorkerState.Running -> stringResource(id = R.string.running)
        is StatsWorkerState.Completed -> state.message
        is StatsWorkerState.Failed -> state.message
        else -> ""
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    enabled = enabled,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    onClick = onRequestExecution
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "work is running",
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = message
                )
            }
        }
    }
}

@Composable
fun StatsCounterRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            modifier = Modifier.weight(weight = 1F, fill = true),
            text = label
        )
        Text(
            modifier = Modifier.weight(weight = 1F, fill = true),
            textAlign = TextAlign.End,
            text = value
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmPhrase: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var phrase by rememberSaveable {
        mutableStateOf("")
    }
    var buttonEnabled by rememberSaveable {
        mutableStateOf(false)
    }

    // https://m3.material.io/components/dialogs/specs
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 16.dp
                        ),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextField(
                    value = phrase,
                    onValueChange = {
                        phrase = it
                        buttonEnabled = phrase == confirmPhrase
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )
                TextButton(
                    enabled = buttonEnabled,
                    onClick = {
                        onDismiss()
                        onConfirm()
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(4.dp)
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CounterCardPreview() {
    val counterState = StatsCounterState(
        comicsCount = 10,
        lastUpdate = ZonedDateTime.now(),
        isLoading = false,
        error = stringResource(id = R.string.comics_delete_error)
    )

    Mdc3Theme {
        CounterCard(
            version = "1.0-hello 123",
            state = counterState,
            onDeleteAllCLick = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkerCardRunningPreview() {
    Mdc3Theme {
        WorkerCard(
            text = stringResource(id = R.string.import_old_database),
            state = StatsWorkerState.Running,
            onRequestExecution = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkerCardCompletedPreview() {
    val title = stringResource(id = R.string.import_old_database)
    val message = stringResource(
        id = R.string.import_old_database_success_with_date,
        10, 5, LocalDate.now()
    )

    Mdc3Theme {
        WorkerCard(
            text = title,
            state = StatsWorkerState.Completed(message),
            onRequestExecution = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WorkerCardFailedPreview() {
    val title = stringResource(id = R.string.import_old_database)
    val message = stringResource(id = R.string.import_old_database_error)

    Mdc3Theme {
        WorkerCard(
            text = title,
            state = StatsWorkerState.Failed(message),
            onRequestExecution = { }
        )
    }
}

@Preview
@Composable
fun ConfirmDialogPreview() {
    Mdc3Theme {
        ConfirmDialog(
            title = stringResource(id = R.string.confirm_title),
            message = stringResource(
                id = R.string.confirm_delete_comics_with_confirm_phrase,
                "DELETE"
            ),
            confirmPhrase = "DELETE",
            onConfirm = { },
            onDismiss = { }
        )
    }
}