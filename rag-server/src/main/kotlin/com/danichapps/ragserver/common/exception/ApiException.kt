package com.danichapps.ragserver.common.exception

sealed class ApiException(message: String) : RuntimeException(message) {
    class ConflictException(message: String) : ApiException(message)
    class NotFoundException(message: String) : ApiException(message)
    class ValidationException(message: String) : ApiException(message)
    class PathTraversalException(message: String) : ApiException(message)
}
