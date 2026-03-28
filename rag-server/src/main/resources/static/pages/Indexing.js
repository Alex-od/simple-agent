const { ref, onMounted, onUnmounted, inject } = Vue
import { adminApi } from '../api/admin.js'
import ProgressBar from '../components/ProgressBar.js'
import LiveLog from '../components/LiveLog.js'

export default {
  name: 'Indexing',
  components: { ProgressBar, LiveLog },
  template: `
    <div>
      <h1 class="page-title">Indexing</h1>

      <div class="section">
        <button class="btn btn-primary"
                @click="handleStart"
                :disabled="indexingState.status === 'IN_PROGRESS'">
          Запустить индексацию
        </button>
      </div>

      <div class="section" v-if="indexingState.status">
        <div class="card">
          <div style="margin-bottom: 8px;">
            <strong>Статус:</strong> {{ statusLabel }}
          </div>

          <ProgressBar
            v-if="indexingState.status === 'IN_PROGRESS'"
            :current="indexingState.processedFiles || 0"
            :total="indexingState.totalFiles || 0"
            :label="indexingState.currentFile || ''"
          />

          <div v-if="indexingState.status === 'COMPLETED'" style="margin-top: 12px; color: var(--success);">
            ✅ Завершено: создано {{ indexingState.totalChunks || 0 }} чанков
          </div>

          <div v-if="indexingState.status === 'FAILED'" style="margin-top: 12px; color: var(--danger);">
            ❌ Ошибка: {{ indexingState.error || 'Неизвестная ошибка' }}
          </div>
        </div>
      </div>

      <div class="section" v-if="logEntries.length > 0">
        <h2 class="section-title">Лог</h2>
        <LiveLog :entries="logEntries" />
      </div>
    </div>
  `,
  setup() {
    const addToast = inject('addToast')

    const indexingState = ref({})
    const logEntries = ref([])
    let pollingId = null
    let lastFile = ''

    const statusLabel = ref('')

    function updateStatusLabel() {
      const s = indexingState.value.status
      const labels = {
        'IDLE': 'Ожидание',
        'IN_PROGRESS': 'Выполняется...',
        'COMPLETED': 'Завершена',
        'FAILED': 'Ошибка',
      }
      statusLabel.value = labels[s] || s || 'Неизвестно'
    }

    async function loadStatus() {
      try {
        const data = await adminApi.getIndexingStatus()
        indexingState.value = data

        if (data.currentFile && data.currentFile !== lastFile) {
          const ts = new Date().toLocaleTimeString('ru-RU')
          logEntries.value.push(`[${ts}] Обработка: ${data.currentFile}`)
          lastFile = data.currentFile
        }

        if (data.status === 'COMPLETED' && pollingId) {
          const ts = new Date().toLocaleTimeString('ru-RU')
          logEntries.value.push(`[${ts}] Завершено — создано ${data.totalChunks || 0} чанков`)
          stopPolling()
        }

        if (data.status === 'FAILED' && pollingId) {
          const ts = new Date().toLocaleTimeString('ru-RU')
          logEntries.value.push(`[${ts}] ERROR: ${data.error || 'Неизвестная ошибка'}`)
          stopPolling()
        }

        updateStatusLabel()
      } catch (e) {
        addToast('Ошибка получения статуса: ' + e.message, 'error')
      }
    }

    function startPolling() {
      if (pollingId) return
      pollingId = setInterval(loadStatus, 1500)
    }

    function stopPolling() {
      if (pollingId) {
        clearInterval(pollingId)
        pollingId = null
      }
    }

    async function handleStart() {
      try {
        await adminApi.startIndexing()
        logEntries.value = []
        lastFile = ''
        const ts = new Date().toLocaleTimeString('ru-RU')
        logEntries.value.push(`[${ts}] Индексация запущена`)
        await loadStatus()
        startPolling()
      } catch (e) {
        addToast('Ошибка запуска: ' + e.message, 'error')
      }
    }

    onMounted(async () => {
      await loadStatus()
      if (indexingState.value.status === 'IN_PROGRESS') {
        startPolling()
      }
    })

    onUnmounted(() => {
      stopPolling()
    })

    return { indexingState, logEntries, statusLabel, handleStart }
  },
}
