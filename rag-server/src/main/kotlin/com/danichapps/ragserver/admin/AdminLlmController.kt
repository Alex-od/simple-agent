package com.danichapps.ragserver.admin

import com.danichapps.ragserver.admin.dto.LlmModelsResponse
import com.danichapps.ragserver.admin.dto.ModelsPathResponse
import com.danichapps.ragserver.admin.dto.SetActiveLlmRequest
import com.danichapps.ragserver.admin.dto.SetModelsPathRequest
import com.danichapps.ragserver.llm.LlmService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/llm")
class AdminLlmController(
    private val llmService: LlmService
) {

    @GetMapping("/models")
    fun listModels(): LlmModelsResponse {
        return LlmModelsResponse(
            models = llmService.listAllModels(),
            active = llmService.getActiveName()
        )
    }

    @PutMapping("/active")
    fun setActive(@RequestBody @Valid request: SetActiveLlmRequest): ResponseEntity<Void> {
        llmService.setActive(request.modelId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/models-path")
    fun getModelsPath(): ModelsPathResponse {
        return ModelsPathResponse(path = llmService.getModelsPath())
    }

    @PutMapping("/models-path")
    fun setModelsPath(@RequestBody @Valid request: SetModelsPathRequest): ResponseEntity<Void> {
        llmService.setModelsPath(request.path.replace('\\', '/'))
        return ResponseEntity.ok().build()
    }
}
