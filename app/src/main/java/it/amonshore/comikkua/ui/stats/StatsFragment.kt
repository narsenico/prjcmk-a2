@file:OptIn(ExperimentalMaterial3Api::class)

package it.amonshore.comikkua.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
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
import it.amonshore.comikkua.R
import it.amonshore.comikkua.toHumanReadable
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
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                Mdc3Theme {
                    StatsScreen(
                        comicsCount = uiState.comicsCount,
                        lastUpdate = uiState.lastUpdate,
                        isLoading = uiState.isLoading,
                        onDeleteAllCLick = viewModel::deleteAll
                    )
                }
            }
        }
    }
}

@Composable
fun StatsScreen(
    comicsCount: Int,
    lastUpdate: ZonedDateTime?,
    isLoading: Boolean = false,
    onDeleteAllCLick: () -> Unit,
) {
    val context = LocalContext.current
    var showConfirmDialog by rememberSaveable {
        mutableStateOf(false)
    }

    Column(Modifier.fillMaxSize()) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        StatsCounterRow(
            label = stringResource(id = R.string.stats_comics_count),
            value = comicsCount.toString()
        )
        Divider()
        StatsCounterRow(
            label = stringResource(id = R.string.stats_last_update),
            value = lastUpdate?.toHumanReadable(context) ?: ""
        )
        Divider()
        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(id = R.string.delete_all))
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
fun StatsCounterRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
fun StatsScreenPreview() {
    Mdc3Theme {
        StatsScreen(
            comicsCount = 100,
            lastUpdate = ZonedDateTime.now(),
            onDeleteAllCLick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatsCounterRowPreview() {
    Mdc3Theme {
        StatsCounterRow(label = "Counter", value = "100")
    }
}

@Preview
@Composable
fun ConfirmDialogPreview() {
    Mdc3Theme {
        ConfirmDialog(
            title = "Confirm",
            message = "Enter CONFIRM to confirm",
            confirmPhrase = "CONFIRM",
            onConfirm = {},
            onDismiss = {}
        )
    }
}