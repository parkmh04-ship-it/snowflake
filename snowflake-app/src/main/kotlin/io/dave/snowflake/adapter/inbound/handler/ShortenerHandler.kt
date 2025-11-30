package io.dave.snowflake.adapter.inbound.handler

import io.dave.snowflake.adapter.inbound.dto.ShortenRequest
import io.dave.snowflake.adapter.inbound.dto.ShortenResponse
import io.dave.snowflake.application.usecase.RetrieveUrlUseCase
import io.dave.snowflake.application.usecase.ShortenUrlUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import java.net.URI

@Component
class ShortenerHandler(
    private val shortenUrlUseCase: ShortenUrlUseCase,
    private val retrieveUrlUseCase: RetrieveUrlUseCase,
    @param:Value($$"${app.base-url}") private val baseUrl: String,
    @param:Value($$"${app.shorten-path-prefix}") private val shortenPathPrefix: String
) {

    suspend fun shorten(request: ServerRequest): ServerResponse {
        return try {
            // 1. Parse request body
            val shortenRequest = request.awaitBody<ShortenRequest>()

            // 2. Execute service
            val mapping = shortenUrlUseCase.shorten(shortenRequest.url)

            // 3. Convert to HTTP response
            val response =
                ShortenResponse(
                    shortUrl = "$baseUrl$shortenPathPrefix/${mapping.shortUrl.value}",
                    originalUrl = mapping.longUrl.value,
                    createdAt = mapping.createdAt
                )

            ServerResponse.status(HttpStatus.CREATED).bodyValueAndAwait(response)
        } catch (e: IllegalArgumentException) {
            ServerResponse.badRequest().bodyValueAndAwait("Invalid request: ${e.message}")
        } catch (e: Exception) {
            ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValueAndAwait("An error occurred")
        }
    }

    suspend fun redirect(request: ServerRequest): ServerResponse {
        return try {
            // 1. Extract path variable
            val shortUrl = request.pathVariable("shortUrl")

            // 2. Execute service
            val mapping = retrieveUrlUseCase.retrieve(shortUrl)

            // 3. Respond
            if (mapping != null) {
                ServerResponse.status(HttpStatus.FOUND)
                    .location(URI.create(mapping.longUrl.value))
                    .buildAndAwait()
            } else {
                ServerResponse.notFound().buildAndAwait()
            }
        } catch (e: IllegalArgumentException) {
            ServerResponse.badRequest().bodyValueAndAwait("Invalid request: ${e.message}")
        } catch (e: Exception) {
            ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValueAndAwait("An error occurred")
        }
    }

    suspend fun health(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("OK")
    }

    suspend fun ping(request: ServerRequest): ServerResponse {
        return ServerResponse.ok().bodyValueAndAwait("pong")
    }
}