package com.danichapps.ragserver.review

import com.danichapps.ragserver.review.dto.PrReviewRequest
import com.danichapps.ragserver.review.dto.PrReviewResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/review")
class ReviewController(
    private val prReviewService: PrReviewService
) {

    private val log = LoggerFactory.getLogger(ReviewController::class.java)

    @PostMapping("/pr")
    fun reviewPr(@RequestBody request: PrReviewRequest): PrReviewResponse {
        log.info(
            "qqwe_tag reviewPr POST: baseBranch={}, useRag={}, diffProvided={}",
            request.baseBranch,
            request.useRag,
            request.diff != null
        )
        return prReviewService.reviewPr(request)
    }
}
