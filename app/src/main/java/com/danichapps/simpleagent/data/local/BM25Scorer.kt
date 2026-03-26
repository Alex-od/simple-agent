package com.danichapps.simpleagent.data.local

import kotlin.math.ln

private const val K1 = 1.5f
private const val B = 0.75f

class BM25Scorer(corpus: List<String>) {

    private val tokenizedCorpus: List<List<String>> = corpus.map(::tokenize)
    private val avgDocLen: Float = tokenizedCorpus.map { it.size }.average().toFloat().coerceAtLeast(1f)
    private val docCount: Int = corpus.size
    private val df: Map<String, Int> = buildMap {
        tokenizedCorpus.forEach { tokens ->
            tokens.toSet().forEach { term -> merge(term, 1, Int::plus) }
        }
    }

    fun score(query: String, docIndex: Int): Float {
        val queryTerms = tokenize(query)
        val docTerms = tokenizedCorpus[docIndex]
        val docLen = docTerms.size.toFloat()
        val tf = docTerms.groupingBy { it }.eachCount()

        return queryTerms.sumOf { term ->
            val termTf = tf[term] ?: 0
            val termDf = df[term] ?: 0
            if (termTf == 0 || termDf == 0) return@sumOf 0.0
            val idf = ln((docCount - termDf + 0.5) / (termDf + 0.5) + 1.0)
            val tfNorm = (termTf * (K1 + 1)) / (termTf + K1 * (1 - B + B * docLen / avgDocLen))
            idf * tfNorm
        }.toFloat()
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
}
