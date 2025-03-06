package com.wedgess.luas.data.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import java.io.IOException

internal suspend inline fun <reified T> HttpClient.requestResult(
    block: HttpRequestBuilder.() -> Unit
): Result<T> {
    return resultOf {
        val response = request { block() }
        response.handleResponse<T>()
    }.recoverCatching { e ->
        throw handleRecovery(e)
    }
}

private fun handleRecovery(e: Throwable): Throwable {
    return when (e) {
        is ClientRequestException -> e
        is ServerResponseException -> e
        is IOException -> Exception("Network error occurred: ${e.message}", e)
        is SerializationException -> Exception("Serialization error: ${e.message}", e)
        else -> Exception("Unknown error occurred: ${e.message}", e)
    }
}

private suspend inline fun <reified T> HttpResponse.handleResponse(): T {
    return when (this.status.value) {
        in HttpStatusCode.BadRequest.value..HttpStatusCode.TooManyRequests.value -> {
            throw ClientRequestException(this, this.bodyAsText())
        }

        in HttpStatusCode.InternalServerError.value..HttpStatusCode.InsufficientStorage.value -> {
            throw ServerResponseException(this, this.bodyAsText())
        }

        HttpStatusCode.NoContent.value -> Unit as T
        HttpStatusCode.OK.value, HttpStatusCode.Created.value -> this.body()
        else -> throw ResponseException(this, this.bodyAsText())
    }
}
