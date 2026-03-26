package com.danichapps.simpleagent.data.local

object RerankParser {

    /**
     * Парсит ответ LLM и возвращает валидные 1-based индексы.
     *
     * @param rawResponse  сырой текст от Gemma
     * @param maxCount     сколько индексов нам нужно (POST_RANK_K)
     * @param candidatesSize количество кандидатов, переданных в промпт
     * @return список 1-based индексов; пустой список если ничего не распарсилось
     */
    fun parse(rawResponse: String, maxCount: Int, candidatesSize: Int): List<Int> =
        Regex("\\d+")
            .findAll(rawResponse)
            .map { it.value.toInt() }
            .filter { it in 1..candidatesSize }
            .distinct()
            .take(maxCount)
            .toList()
}
