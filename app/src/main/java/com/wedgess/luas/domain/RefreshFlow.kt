package com.wedgess.luas.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart

class RefreshFlow {
    private val trigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <R> flatMapLatest(transform: suspend (value: Unit) -> Flow<R>): Flow<R> =
        trigger.onStart { emit(Unit) }.flatMapLatest(transform)

    fun refresh() = trigger.tryEmit(Unit)
}
