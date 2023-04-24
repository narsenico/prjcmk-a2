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
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
                val comicsCount by viewModel.comicsCount.observeAsState(0)
                val lastUpdate by viewModel.lastUpdate.observeAsState(null)

                Mdc3Theme {
                    StatsScreen(
                        comicsCount = comicsCount,
                        lastUpdate = lastUpdate
                    )
                }
            }
        }
    }
}

@Composable
fun StatsScreen(
    comicsCount: Int,
    lastUpdate: ZonedDateTime?
) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
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


@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    Mdc3Theme {
        StatsScreen(comicsCount = 100, lastUpdate = ZonedDateTime.now())
    }
}

@Preview(showBackground = true)
@Composable
fun StatsCounterRowPreview() {
    Mdc3Theme {
        StatsCounterRow(label = "Counter", value = "100")
    }
}