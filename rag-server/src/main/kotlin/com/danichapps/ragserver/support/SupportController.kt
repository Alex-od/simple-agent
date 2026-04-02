package com.danichapps.ragserver.support

import com.danichapps.ragserver.support.dto.SupportRequest
import com.danichapps.ragserver.support.dto.SupportResponse
import com.danichapps.ragserver.support.dto.TicketDto
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/support")
class SupportController(
    private val supportService: SupportService
) {

    private val log = LoggerFactory.getLogger(SupportController::class.java)

    @PostMapping
    fun handleRequest(@RequestBody request: SupportRequest): SupportResponse {
        log.info(
            "qqwe_tag support POST: question='{}', ticketId={}",
            request.question, request.ticketId
        )
        return supportService.handleRequest(request)
    }

    @GetMapping("/tickets")
    fun listTickets(): List<TicketDto> {
        log.info("qqwe_tag support GET /tickets")
        return supportService.listTickets()
    }
}
