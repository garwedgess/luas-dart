package com.wedgess.luas.data.utils.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Like [runCatching], but with proper coroutines cancellation handling.
 * Also only catches [Exception] instead of [Throwable].
 *
 * Cancellation exceptions need to be rethrown.
 * @see [Issue 1814](https://github.com/Kotlin/kotlinx.coroutines/issues/1814)
 */
inline fun <T, R> T.resultOf(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Similar to the above [resultOf] but do be used on Kotlin Flows.
 */
fun <T> Flow<T>.resultOf() = flow<Result<T>> {
    catch {
        emit(Result.failure(it))
    }.collect { emit(Result.success(it)) }
}

fun <T, R> Flow<Result<T>>.mapResult(transform: (T) -> R): Flow<Result<R>> {
    return this.map { result ->
        result.map { data ->
            transform(data)
        }
    }
}

suspend fun <T> runWithErrorHandling(operation: suspend () -> T): Result<Unit> {
    return try {
        operation()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
