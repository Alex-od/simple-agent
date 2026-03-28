package com.danichapps.ragserver.rag.indexing

import com.danichapps.ragserver.service.RagService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@Service
class IndexingService(
    private val ragService: RagService
) {

    private val log = LoggerFactory.getLogger(IndexingService::class.java)
    private val executor = Executors.newSingleThreadExecutor()
    private val state = AtomicReference(IndexingState())

    fun startIndexing(documentsPath: String): Boolean {
        val current = state.get()
        if (current.status == IndexingStatus.IN_PROGRESS) {
            return false
        }

        state.set(
            IndexingState(
                status = IndexingStatus.IN_PROGRESS,
                startedAt = Instant.now().toString()
            )
        )

        executor.submit {
            try {
                val dir = File(documentsPath)
                ragService.clearStore()

                val callback = ragService.indexDirectory(dir)

                state.set(
                    IndexingState(
                        status = IndexingStatus.COMPLETED,
                        totalFiles = callback.totalFiles,
                        processedFiles = callback.processedFiles,
                        currentFile = null,
                        totalChunks = callback.totalChunks,
                        startedAt = state.get().startedAt,
                        completedAt = Instant.now().toString()
                    )
                )
                log.info("Индексация завершена успешно")
            } catch (e: Exception) {
                log.error("Индексация завершилась с ошибкой", e)
                state.set(
                    state.get().copy(
                        status = IndexingStatus.FAILED,
                        error = e.message,
                        completedAt = Instant.now().toString()
                    )
                )
            }
        }

        return true
    }

    fun getState(): IndexingState = state.get()
}
