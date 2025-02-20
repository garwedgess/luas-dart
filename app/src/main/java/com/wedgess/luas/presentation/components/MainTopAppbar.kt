package com.wedgess.luas.presentation.components

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wedgess.luas.presentation.main.model.TopAppBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppbar(topAppBarState: TopAppBarState) {
    CenterAlignedTopAppBar(
        title = {
            Text(topAppBarState.title)
        },
        actions = topAppBarState.actions ?: run { @Composable {} }
    )
}
