package com.danichapps.simpleagent.data.remote

import com.danichapps.simpleagent.data.remote.dto.SupportRequestDto
import com.danichapps.simpleagent.data.remote.dto.SupportResponseDto
import com.danichapps.simpleagent.data.remote.dto.TicketSummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val SUPPORT_BASE_URL = "http://$SERVER_HOST:8100"

class SupportService(private val client: HttpClient) {

    suspend fun ask(question: String, ticketId: String?): SupportResponseDto =
        client.post("$SUPPORT_BASE_URL/api/v1/support") {
            contentType(ContentType.Application.Json)
            setBody(SupportRequestDto(question = question, ticketId = ticketId))
        }.body()

    suspend fun listTickets(): List<TicketSummaryDto> =
        client.get("$SUPPORT_BASE_URL/api/v1/support/tickets").body()
}
