# E2E Scenario: RAG + Task Memory
Дата: 2026-03-21

## Критерии проверки
- Источники (N) присутствуют под каждым ответом ассистента
- Ассистент понимает контекстные вопросы без потери темы (task state работает)

---

## Диалог 1 — Структура SRS-документа

- [x] D1-1. Запустить приложение SimpleAgent → заголовок "AI Agent" без бейджа RAG ✅ (проверено)
- [x] D1-2. Открыть меню (hamburger), включить RAG → бейдж "RAG" появился в заголовке ✅ (проверено)
- [x] D1-3. Отправить: "What is an SRS document according to IEEE 830?" → ответ + кнопка "Источники (N)" ✅ (проверено)
- [x] D1-4. Отправить: "What are the main sections of SRS?" → ответ перечисляет разделы + источники ✅ (проверено)
- [x] D1-5. Отправить: "Tell me about the Introduction section" → ответ про Introduction + источники ✅ (проверено)
- [x] D1-6. Отправить: "What should Overall Description contain?" → ответ + источники ✅ (проверено)
- [ ] D1-7. Отправить: "Tell me about product perspective subsection" → ответ понимает контекст (SRS) + источники
- [ ] D1-8. Отправить: "What are functional requirements in IEEE 830?" → ответ + источники
- [ ] D1-9. Отправить: "What are non-functional requirements?" → ответ + источники
- [ ] D1-10. Отправить: "What is the difference between them?" → ответ понимает "them" = functional vs non-functional + источники

---

## Диалог 2 — Качество SRS-документа

- [ ] D2-1. Перезапустить приложение (кнопка Back или force stop) → чистый чат
- [ ] D2-2. Включить RAG → бейдж "RAG" появился
- [ ] D2-3. Отправить: "What are the quality characteristics of a good SRS?" → ответ + источники
- [ ] D2-4. Отправить: "Tell me about correctness" → ответ понимает контекст (SRS correctness) + источники
- [ ] D2-5. Отправить: "What does unambiguity mean?" → ответ в контексте SRS + источники
- [ ] D2-6. Отправить: "How to ensure completeness?" → ответ + источники
- [ ] D2-7. Отправить: "What about consistency?" → ответ + источники
- [ ] D2-8. Отправить: "Tell me about verifiability" → ответ + источники
- [ ] D2-9. Отправить: "How does modifiability work in SRS?" → ответ + источники
- [ ] D2-10. Отправить: "Which of these qualities is most critical and why?" → ответ синтезирует весь диалог + источники
