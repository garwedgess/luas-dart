package com.wedgess.luas.presentation.components

import android.annotation.SuppressLint
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.main.compose.TransportTypeDropdown
import com.wedgess.luas.presentation.main.model.TopAppBarState

@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppbar(
    topAppBarState: TopAppBarState,
    modifier: Modifier = Modifier,
    onTransportTypeChange: (TransportType) -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(topAppBarState.title)
        },
        actions = {
            // Add your custom actions first
            TransportTypeDropdown(
                selectedTransportType = topAppBarState.transportType,
                onTransportTypeChange = onTransportTypeChange,
            )

            // Then add the existing actions if they exist
            topAppBarState.actions?.invoke(this)
        },
    )
}
