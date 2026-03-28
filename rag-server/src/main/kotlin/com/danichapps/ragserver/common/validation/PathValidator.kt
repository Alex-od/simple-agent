package com.danichapps.ragserver.common.validation

import com.danichapps.ragserver.common.exception.ApiException
import java.io.File

object PathValidator {

    fun validate(userPath: String, allowedBasePaths: List<String>) {
        if (userPath.contains("\u0000")) {
            throw ApiException.PathTraversalException("Path contains null bytes")
        }
        if (userPath.contains("..")) {
            throw ApiException.PathTraversalException("Path contains '..' traversal")
        }

        val canonicalPath = File(userPath).canonicalPath.replace('\\', '/')
        val allowed = allowedBasePaths.any { base ->
            val canonicalBase = File(base).canonicalPath.replace('\\', '/')
            canonicalPath.startsWith(canonicalBase)
        }
        if (!allowed) {
            throw ApiException.PathTraversalException(
                "Path '$userPath' is outside of allowed base paths"
            )
        }
    }
}
