const { ref, onMounted, inject, nextTick } = Vue
import { chatApi } from '../api/chat.js'

export default {
  name: 'Playground',
  template: `
    <div>
      <h1 class="page-title">Playground</h1>

      <div class="chat-controls">
        <label>
          <input type="checkbox" v-model="useRag" /> RAG
        </label>
        <label>
          Top K:
          <input type="number" class="input" v-model.number="topK" min="1" max="20" style="width: 60px; padding: 4px 8px;" />
        </label>
        <span v-if="chatConfig" style="margin-left: auto;">
          LLM: {{ chatConfig.activeLlm || '—' }} | Чанков: {{ chatConfig.rag?.indexedChunks ?? 0 }}
        </span>
      </div>

      <div class="chat-container">
        <div class="chat-main">
          <div class="chat-messages" ref="messagesEl">
            <div v-if="messages.length === 0" style="color: var(--text-secondary); text-align: center; margin-top: 40px;">
              Отправьте сообщение для начала диалога
            </div>
            <div v-for="(msg, i) in messages" :key="i"
                 :class="['message', msg.role, { selected: selectedIdx === i }]"
                 @click="selectMessage(i)">
              {{ msg.content }}
            </div>
          </div>
          <div class="chat-input-row">
            <input class="input"
                   v-model="inputText"
                   placeholder="Введите сообщение..."
                   @keydown.enter="sendMessage"
                   :disabled="sending" />
            <button class="btn btn-primary" @click="sendMessage" :disabled="sending || !inputText.trim()">
              Отправить
            </button>
          </div>
        </div>

        <div class="debug-panel">
          <div class="debug-panel-title">RAG контекст</div>
          <div v-if="!selectedRagContext">
            <span style="font-size: 12px; color: var(--text-secondary);">
              Выберите сообщение ассистента для просмотра чанков
            </span>
          </div>
          <div v-else-if="selectedRagContext.length === 0">
            <span style="font-size: 12px; color: var(--text-secondary);">
              Нет RAG-контекста
            </span>
          </div>
          <div v-else>
            <div v-for="(chunk, ci) in selectedRagContext" :key="ci" class="chunk-item">
              <div>{{ chunk.text }}</div>
              <div class="chunk-source">{{ chunk.source }}</div>
              <div :class="['chunk-score', scoreClass(chunk.score)]">
                Score: {{ chunk.score?.toFixed(3) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  setup() {
    const addToast = inject('addToast')

    const messages = ref([])
    const inputText = ref('')
    const useRag = ref(true)
    const topK = ref(5)
    const sending = ref(false)
    const selectedIdx = ref(null)
    const chatConfig = ref(null)
    const messagesEl = ref(null)

    const selectedRagContext = ref(null)

    function selectMessage(i) {
      const msg = messages.value[i]
      if (msg.role === 'assistant' && msg.ragContext) {
        selectedIdx.value = i
        selectedRagContext.value = msg.ragContext
      }
    }

    function scoreClass(score) {
      if (score == null) return 'score-low'
      if (score >= 0.7) return 'score-high'
      if (score >= 0.4) return 'score-mid'
      return 'score-low'
    }

    async function sendMessage() {
      const text = inputText.value.trim()
      if (!text || sending.value) return

      messages.value.push({ role: 'user', content: text })
      inputText.value = ''
      sending.value = true
      scrollToBottom()

      try {
        const data = await chatApi.sendMessage(text, useRag.value, topK.value)
        messages.value.push({
          role: 'assistant',
          content: data.answer,
          ragContext: data.ragContext || [],
        })
        scrollToBottom()
      } catch (e) {
        addToast('Ошибка отправки: ' + e.message, 'error')
        messages.value.push({
          role: 'assistant',
          content: 'Ошибка: ' + e.message,
          ragContext: [],
        })
      } finally {
        sending.value = false
      }
    }

    function scrollToBottom() {
      nextTick(() => {
        if (messagesEl.value) {
          messagesEl.value.scrollTop = messagesEl.value.scrollHeight
        }
      })
    }

    onMounted(async () => {
      try {
        chatConfig.value = await chatApi.getConfig()
      } catch (e) {
        addToast('Ошибка загрузки конфигурации чата: ' + e.message, 'error')
      }
    })

    return {
      messages, inputText, useRag, topK, sending,
      selectedIdx, selectedRagContext, chatConfig, messagesEl,
      sendMessage, selectMessage, scoreClass,
    }
  },
}
