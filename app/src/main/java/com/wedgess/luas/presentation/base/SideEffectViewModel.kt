package com.wedgess.luas.presentation.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SideEffectViewModel<SideEffect> {
    val sideEffect: Flow<SideEffect>

    fun CoroutineScope.emitSideEffect(effect: SideEffect)
}
