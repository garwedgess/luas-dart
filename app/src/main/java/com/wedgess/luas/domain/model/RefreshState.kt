package com.wedgess.luas.domain.model

sealed class RefreshState<out T> {
    data class Success<T>(val data: T, val progress: Float) : RefreshState<T>()
    data class Error(val exception: Throwable) : RefreshState<Nothing>()
}
