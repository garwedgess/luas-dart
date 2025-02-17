package com.wedgess.luas.presentation.components

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppbar() {
    CenterAlignedTopAppBar(
        title = {
            Text("Luas Tracker")
        }
    )
}