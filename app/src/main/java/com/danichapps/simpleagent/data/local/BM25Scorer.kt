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
        TOKEN_REGEX.findAll(text.lowercase())
            .flatMap { match ->
                val raw = match.value
                normalizeToken(raw).asSequence()
            }
            .filter { it.isNotBlank() }
            .toList()

    private fun normalizeToken(token: String): List<String> {
        val cleaned = token
            .replace('ё', 'е')
            .trim('-', '_')

        if (cleaned.isBlank()) return emptyList()

        val parts = cleaned
            .split('-', '_')
            .filter { it.isNotBlank() }

        return buildList {
            parts.forEach { part ->
                add(part)
                stemToken(part)?.let(::add)
            }
        }.distinct()
    }

    private fun stemToken(token: String): String? {
        if (token.length < 5) return null

        val russianSuffixes = listOf(
            "иями", "ями", "ами", "его", "ого", "ему", "ому",
            "ыми", "ими", "ией", "ий", "ый", "ой", "ое", "ее",
            "ая", "яя", "ам", "ям", "ах", "ях", "ов", "ев",
            "ие", "ые", "ое", "ей", "ой", "ий", "ый", "ым",
            "им", "ом", "ем", "ую", "юю", "а", "я", "ы", "и",
            "е", "о", "у", "ю", "ть", "ти", "но", "ен", "ил", "ло"
        )

        val englishSuffixes = listOf("ing", "ed", "es", "s")

        val suffix = when {
            token.any { it in 'а'..'я' } -> russianSuffixes.firstOrNull { token.endsWith(it) && token.length - it.length >= 4 }
            token.any { it in 'a'..'z' } -> englishSuffixes.firstOrNull { token.endsWith(it) && token.length - it.length >= 3 }
            else -> null
        } ?: return null

        return token.removeSuffix(suffix).takeIf { it.length >= 3 && it != token }
    }

    private companion object {
        val TOKEN_REGEX = Regex("[\\p{L}\\p{Nd}_-]+")
    }
}
