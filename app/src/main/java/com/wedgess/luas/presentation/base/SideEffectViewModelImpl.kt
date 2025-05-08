package com.wedgess.luas.presentation.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SideEffectViewModelImpl<SideEffect> : SideEffectViewModel<SideEffect> {

    private val _sideEffect by lazy { Channel<SideEffect>(Channel.CONFLATED) }
    override val sideEffect: Flow<SideEffect> by lazy { _sideEffect.receiveAsFlow() }

    override fun CoroutineScope.emitSideEffect(effect: SideEffect) {
        this.launch { _sideEffect.send(effect) }
    }
}
