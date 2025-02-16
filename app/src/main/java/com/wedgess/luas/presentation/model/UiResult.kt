package com.wedgess.luas.presentation.model

import androidx.compose.runtime.Composable

sealed class UiResult<out T> {
    data object Loading : UiResult<Nothing>()
    data class Empty(val message: String) : UiResult<Nothing>()
    data class Error(val message: String) : UiResult<Nothing>()
    data class Success<T>(val data: T) : UiResult<T>()
}

@Composable
inline fun <T> UiResult<T>.Compose(
    onLoading: @Composable () -> Unit,
    onEmpty: @Composable (String) -> Unit,
    onError: @Composable (String) -> Unit,
    onSuccess: @Composable (T) -> Unit
) {
    when (this) {
        is UiResult.Empty -> onEmpty(this.message)
        is UiResult.Error -> onError(this.message)
        UiResult.Loading -> onLoading()
        is UiResult.Success -> onSuccess(this.data)
    }
}