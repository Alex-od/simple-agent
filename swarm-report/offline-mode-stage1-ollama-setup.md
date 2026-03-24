# Этап 1: Настройка Ollama на ПК
Статус: [ ] pending

## Задача
Установить Ollama на Windows, загрузить модель llama3.2:3b,
настроить так чтобы Android-эмулятор мог обращаться к ней.

## КРИТИЧЕСКИ ВАЖНО
По умолчанию Ollama слушает только 127.0.0.1.
Android-эмулятор — отдельная VM, он обращается к хосту через 10.0.2.2.
Чтобы эмулятор достучался до Ollama, нужно переменная среды OLLAMA_HOST=0.0.0.0.

## Шаги

### 1. Установить Ollama
Запустить PowerShell от администратора:
```powershell
irm https://ollama.com/install.ps1 | iex
```

### 2. Настроить OLLAMA_HOST
В PowerShell (или через Параметры → Переменные среды):
```powershell
[System.Environment]::SetEnvironmentVariable("OLLAMA_HOST", "0.0.0.0", "Machine")
```
После установки переменной — ПЕРЕЗАПУСТИТЬ Ollama (завершить из трея, запустить снова).

### 3. Загрузить модель
```powershell
ollama pull llama3.2:3b
```

### 4. Проверить доступность
```powershell
# Список моделей (должен показать llama3.2:3b)
curl http://localhost:11434/api/tags

# Тест генерации
curl -X POST http://localhost:11434/v1/chat/completions `
  -H "Content-Type: application/json" `
  -d '{"model":"llama3.2:3b","messages":[{"role":"user","content":"ping"}]}'
```

## Критерий успеха
- Команда curl возвращает JSON с полем `choices[0].message.content`
- Из эмулятора: `adb shell curl http://10.0.2.2:11434/api/tags` — возвращает список моделей

## Замечания
- Модель llama3.2:3b занимает ~2 GB на диске
- При первом запросе возможна задержка ~5-10 сек (загрузка модели в RAM)
- Поддерживает JSON mode (response_format: {"type": "json_object"})
