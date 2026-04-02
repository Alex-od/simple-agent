package com.danichapps.ragserver.files

import com.danichapps.ragserver.files.dto.FilesRequest
import com.danichapps.ragserver.files.dto.FilesResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/files")
class FilesController(
    private val filesService: FilesService
) {

    private val log = LoggerFactory.getLogger(FilesController::class.java)

    @PostMapping("/analyze")
    fun analyze(@RequestBody request: FilesRequest): FilesResponse {
        log.info("qqwe_tag FilesController, analyze: task='{}'", request.task)
        return filesService.analyze(request)
    }
}
