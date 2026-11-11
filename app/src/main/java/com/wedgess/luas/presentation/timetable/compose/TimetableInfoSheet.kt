package com.wedgess.luas.presentation.timetable.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableZone
import com.wedgess.luas.presentation.timetable.TimetableUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimetableInfoSheet(
    state: TimetableUiState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(stringResource(R.string.timetable_info), style = MaterialTheme.typography.titleLarge) }
            item { TimetableNotice(state.transport) }
            state.feed?.let { feed ->
                item {
                    Text(
                        stringResource(
                            R.string.timetable_coverage,
                            feed.validFrom.formatDate(),
                            feed.validUntil.formatDate()
                        )
                    )
                }
                item {
                    Text(
                        stringResource(
                            R.string.timetable_downloaded,
                            feed.downloadedAt.atZone(TimetableZone).toLocalDate().formatDate()
                        )
                    )
                }
            }
            item { TimetableWarnings(state) }
            item { TimetableUpdateButton(state, onUpdate) }
            item { TimetableAttribution() }
        }
    }
}

@Composable
internal fun TimetableDownload(state: TimetableUiState, onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TimetableNotice(state.transport)
        Text(stringResource(R.string.timetable_download_hint))
        TimetableWarnings(state)
        TimetableUpdateButton(state, onUpdate)
        TimetableAttribution()
    }
}

@Composable
private fun TimetableNotice(transport: TransportType) {
    Text(
        stringResource(
            if (transport == TransportType.DART) R.string.timetable_dart_notice else R.string.timetable_scheduled_notice
        )
    )
}

@Composable
private fun TimetableUpdateButton(state: TimetableUiState, onUpdate: () -> Unit) {
    Button(onClick = onUpdate, enabled = !state.updating && !state.loading) {
        Text(stringResource(if (state.feed == null) R.string.timetable_download else R.string.timetable_update))
    }
}

@Composable
private fun TimetableAttribution() {
    val uriHandler = LocalUriHandler.current
    TextButton(onClick = { uriHandler.openUri("https://www.transportforireland.ie/transitData/PT_Data.html") }) {
        Text(stringResource(R.string.timetable_attribution), style = MaterialTheme.typography.labelSmall)
    }
}
