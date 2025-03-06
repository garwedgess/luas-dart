package com.wedgess.luas.presentation.model

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    data class StringResource(@StringRes val id: Int) : UiText()

    class StringResourceWithArgs(
        @StringRes val id: Int,
        vararg val formatArgs: Any
    ) : UiText()

    class Plural(
        @PluralsRes val id: Int,
        val count: Int,
        vararg val formatArgs: Any
    ) : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(id)
            is StringResourceWithArgs -> context.getString(id, *formatArgs)
            is Plural -> context.resources.getQuantityString(id, count, *formatArgs)
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(id)
            is StringResourceWithArgs -> stringResource(id, *formatArgs)
            is Plural -> pluralStringResource(id, count, *formatArgs)
        }
    }
}
