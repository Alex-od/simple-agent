package com.danichapps.ragserver.admin

import com.danichapps.ragserver.admin.dto.EmbeddingModelsResponse
import com.danichapps.ragserver.admin.dto.ModelsPathResponse
import com.danichapps.ragserver.admin.dto.SetActiveEmbeddingRequest
import com.danichapps.ragserver.admin.dto.SetModelsPathRequest
import com.danichapps.ragserver.rag.embedding.EmbeddingService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/embedding")
class AdminEmbeddingController(
    private val embeddingService: EmbeddingService
) {

    @GetMapping("/models")
    fun listModels(): EmbeddingModelsResponse {
        return EmbeddingModelsResponse(
            models = embeddingService.getAvailableModels(),
            active = embeddingService.getModelName()
        )
    }

    @PutMapping("/active")
    fun setActive(@RequestBody @Valid request: SetActiveEmbeddingRequest): ResponseEntity<Void> {
        embeddingService.switchModel(request.modelId, request.confirmReindex)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/models-path")
    fun getModelsPath(): ModelsPathResponse {
        return ModelsPathResponse(path = embeddingService.getModelsPath())
    }

    @PutMapping("/models-path")
    fun setModelsPath(@RequestBody @Valid request: SetModelsPathRequest): ResponseEntity<Void> {
        embeddingService.setModelsPath(request.path.replace('\\', '/'))
        return ResponseEntity.ok().build()
    }
}
