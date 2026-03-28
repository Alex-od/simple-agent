package com.danichapps.ragserver.common.exception

import com.danichapps.ragserver.common.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.ConflictException::class)
    fun handleConflict(ex: ApiException.ConflictException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(error = "CONFLICT", message = ex.message ?: "Conflict")
        )

    @ExceptionHandler(ApiException.NotFoundException::class)
    fun handleNotFound(ex: ApiException.NotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(error = "NOT_FOUND", message = ex.message ?: "Not found")
        )

    @ExceptionHandler(ApiException.ValidationException::class)
    fun handleValidation(ex: ApiException.ValidationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(error = "VALIDATION_ERROR", message = ex.message ?: "Validation failed")
        )

    @ExceptionHandler(ApiException.PathTraversalException::class)
    fun handlePathTraversal(ex: ApiException.PathTraversalException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(error = "PATH_TRAVERSAL", message = ex.message ?: "Path traversal detected")
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(error = "VALIDATION_ERROR", message = message)
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(error = "INVALID_REQUEST", message = "Неверный формат запроса. Используйте прямые слэши / в путях (не обратные \\).")
        )
}
