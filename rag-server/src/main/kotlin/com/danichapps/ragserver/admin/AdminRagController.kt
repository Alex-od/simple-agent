package com.danichapps.ragserver.admin

import com.danichapps.ragserver.admin.dto.DocumentsPathResponse
import com.danichapps.ragserver.admin.dto.IndexingRequest
import com.danichapps.ragserver.admin.dto.SetDocumentsPathRequest
import com.danichapps.ragserver.common.exception.ApiException
import com.danichapps.ragserver.common.validation.PathValidator
import com.danichapps.ragserver.config.ConfigService
import com.danichapps.ragserver.config.RagFilesProperties
import com.danichapps.ragserver.rag.indexing.IndexingService
import com.danichapps.ragserver.rag.indexing.IndexingState
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
@RequestMapping("/api/v1/admin/rag")
class AdminRagController(
    private val configService: ConfigService,
    private val indexingService: IndexingService,
    private val ragFilesProperties: RagFilesProperties
) {

    @GetMapping("/documents-path")
    fun getDocumentsPath(): DocumentsPathResponse {
        val path = configService.getDocumentsPath()
        val dir = path?.let { File(it) }
        val files = dir?.listFiles() ?: emptyArray()
        return DocumentsPathResponse(
            path = path,
            fileCount = files.size,
            totalSizeBytes = files.sumOf { it.length() }
        )
    }

    @PutMapping("/documents-path")
    fun setDocumentsPath(@RequestBody @Valid request: SetDocumentsPathRequest): ResponseEntity<Void> {
        PathValidator.validate(request.path, ragFilesProperties.allowedBasePaths)
        val dir = File(request.path)
        if (!dir.exists() || !dir.isDirectory) {
            throw ApiException.ValidationException("Path '${request.path}' does not exist or is not a directory")
        }
        configService.setDocumentsPath(request.path)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/indexing")
    fun startIndexing(@RequestBody(required = false) request: IndexingRequest?): ResponseEntity<IndexingState> {
        val documentsPath = configService.getDocumentsPath()
            ?: throw ApiException.ValidationException("Documents path is not configured. Set it via PUT /api/v1/admin/rag/documents-path first.")

        val started = indexingService.startIndexing(documentsPath)
        return if (started) {
            ResponseEntity.status(HttpStatus.ACCEPTED).body(indexingService.getState())
        } else {
            throw ApiException.ConflictException("Indexing is already in progress")
        }
    }

    @GetMapping("/indexing/status")
    fun getIndexingStatus(): IndexingState {
        return indexingService.getState()
    }
}
