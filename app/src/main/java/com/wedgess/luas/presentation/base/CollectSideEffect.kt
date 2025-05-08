package com.wedgess.luas.presentation.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

@Composable
fun <SideEffect> CollectSideEffect(
    sideEffect: Flow<SideEffect>,
    onSideEffect: suspend CoroutineScope.(effect: SideEffect) -> Unit
) {
    LaunchedEffect(Unit) {
        sideEffect.collect { onSideEffect(it) }
    }
}
