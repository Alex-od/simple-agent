const { ref, onMounted, onUnmounted, inject } = Vue
const { useRouter } = VueRouter
import { adminApi } from '../api/admin.js'
import StatusCard from '../components/StatusCard.js'

export default {
  name: 'Dashboard',
  components: { StatusCard },
  template: `
    <div>
      <h1 class="page-title">Dashboard</h1>

      <div class="cards-grid">
        <StatusCard
          title="LLM"
          :value="status.activeLlm || 'Не выбрана'"
          :status="status.activeLlm ? 'ok' : 'warning'"
        />
        <StatusCard
          title="Embedding"
          :value="status.activeEmbedding || 'Не выбрана'"
          :status="status.activeEmbedding ? 'ok' : 'warning'"
        />
        <StatusCard
          title="RAG Index"
          :value="(status.rag?.indexedChunks ?? 0) + ' чанков'"
          :status="status.rag?.indexedChunks > 0 ? 'ok' : 'unknown'"
          :subtitle="status.indexing?.completedAt ? 'Обновлено: ' + formatDate(status.indexing.completedAt) : ''"
        />
      </div>

      <div class="quick-actions">
        <button class="btn btn-primary" @click="reindex" :disabled="reindexing">
          Переиндексировать
        </button>
        <a class="btn btn-secondary" @click="router.push('/config')">Настройки</a>
        <a class="btn btn-secondary" @click="router.push('/playground')">Playground</a>
      </div>
    </div>
  `,
  setup() {
    const router = useRouter()
    const addToast = inject('addToast')
    const status = ref({})
    const reindexing = ref(false)
    let intervalId = null

    async function loadStatus() {
      try {
        status.value = await adminApi.getStatus()
      } catch (e) {
        addToast('Не удалось загрузить статус: ' + e.message, 'error')
      }
    }

    async function reindex() {
      reindexing.value = true
      try {
        await adminApi.startIndexing()
        addToast('Индексация запущена', 'success')
        router.push('/indexing')
      } catch (e) {
        addToast('Ошибка запуска индексации: ' + e.message, 'error')
      } finally {
        reindexing.value = false
      }
    }

    function formatDate(dateStr) {
      if (!dateStr) return ''
      try {
        return new Date(dateStr).toLocaleString('ru-RU')
      } catch (_) {
        return dateStr
      }
    }

    onMounted(() => {
      loadStatus()
      intervalId = setInterval(loadStatus, 10000)
    })

    onUnmounted(() => {
      if (intervalId) clearInterval(intervalId)
    })

    return { status, reindexing, reindex, router, formatDate }
  },
}
