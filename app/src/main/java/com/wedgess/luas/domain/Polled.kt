package com.wedgess.luas.domain

/**
 * One poll outcome. [data] is the most recent result (if any). It is no longer live when the latest attempt failed
 * ([error]) or when it was restored from storage before any fetch completed ([isCached]).
 */
data class Polled<out T>(val data: T?, val error: Throwable? = null, val isCached: Boolean = false) {
    /** True only for data from a successful network response in this run. */
    val isLive: Boolean get() = data != null && error == null && !isCached
}
