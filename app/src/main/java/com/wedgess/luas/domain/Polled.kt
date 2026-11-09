package com.wedgess.luas.domain

/**
 * One poll outcome. [data] is the most recent successful result (if any) and [error] is set when the latest
 * attempt failed, which means [data] is no longer live.
 */
data class Polled<out T>(val data: T?, val error: Throwable? = null)
