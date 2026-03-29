package com.danichapps.ragserver.config

import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore
import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.grpc.Collections.CreateCollection
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.VectorParams
import io.qdrant.client.grpc.Collections.VectorsConfig
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QdrantConfig(
    @Value("\${qdrant.host}") private val host: String,
    @Value("\${qdrant.port}") private val port: Int,
    @Value("\${qdrant.collection-name}") private val collectionName: String,
    @Value("\${qdrant.vector-size}") private val vectorSize: Long
) {

    private val log = LoggerFactory.getLogger(QdrantConfig::class.java)

    private var _client: QdrantClient? = null

    @Bean
    fun qdrantClient(): QdrantClient {
        val grpcClient = QdrantGrpcClient.newBuilder(host, port, false).build()
        val client = QdrantClient(grpcClient)
        _client = client
        ensureCollectionExists(client)
        return client
    }

    @Bean
    fun qdrantEmbeddingStore(qdrantClient: QdrantClient): QdrantEmbeddingStore {
        return QdrantEmbeddingStore.builder()
            .host(host)
            .port(port)
            .collectionName(collectionName)
            .build()
    }

    @PreDestroy
    fun closeClient() {
        _client?.close()
    }

    private fun ensureCollectionExists(client: QdrantClient) {
        try {
            val collections = client.listCollectionsAsync().get()
            val exists = collections.any { it == collectionName }
            if (!exists) {
                log.info("Создаю коллекцию Qdrant: {}", collectionName)
                client.createCollectionAsync(
                    CreateCollection.newBuilder()
                        .setCollectionName(collectionName)
                        .setVectorsConfig(
                            VectorsConfig.newBuilder()
                                .setParams(
                                    VectorParams.newBuilder()
                                        .setSize(vectorSize)
                                        .setDistance(Distance.Cosine)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                ).get()
                log.info("Коллекция {} создана", collectionName)
            } else {
                log.info("Коллекция {} уже существует", collectionName)
            }
        } catch (e: Exception) {
            log.error("Ошибка при инициализации коллекции Qdrant: {}", e.message)
        }
    }
}
