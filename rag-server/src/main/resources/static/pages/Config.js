const { ref, onMounted, inject } = Vue
import { adminApi } from '../api/admin.js'
import ModelSelector from '../components/ModelSelector.js'

export default {
  name: 'Config',
  components: { ModelSelector },
  template: `
    <div>
      <h1 class="page-title">Config</h1>

      <div class="section">
        <h2 class="section-title">LLM Model</h2>
        <ModelSelector
          label="Активная LLM"
          :models="llmModels"
          :activeModel="activeLlm"
          :loading="llmLoading"
          @select="onSelectLlm"
        />
      </div>

      <div class="section">
        <h2 class="section-title">Embedding Model</h2>
        <ModelSelector
          label="Активная Embedding модель"
          :models="embeddingModels"
          :activeModel="activeEmbedding"
          :loading="embeddingLoading"
          @select="onSelectEmbedding"
        />
      </div>

      <div class="section">
        <h2 class="section-title">Documents Path</h2>
        <div class="form-group">
          <label>Путь к документам</label>
          <div style="display: flex; gap: 8px;">
            <input class="input" v-model="docsPath" style="flex: 1;" />
            <button class="btn btn-primary btn-sm" @click="savePath" :disabled="pathSaving">
              Сохранить путь
            </button>
          </div>
        </div>
        <div class="info-row" v-if="docsInfo">
          Файлов: {{ docsInfo.fileCount }} | Размер: {{ formatBytes(docsInfo.totalSizeBytes) }}
        </div>
      </div>
    </div>
  `,
  setup() {
    const addToast = inject('addToast')

    const llmModels = ref([])
    const activeLlm = ref('')
    const llmLoading = ref(false)

    const embeddingModels = ref([])
    const activeEmbedding = ref('')
    const embeddingLoading = ref(false)

    const docsPath = ref('')
    const docsInfo = ref(null)
    const pathSaving = ref(false)

    async function loadLlm() {
      try {
        const data = await adminApi.getLlmModels()
        llmModels.value = data.models || []
        activeLlm.value = data.active || ''
      } catch (e) {
        addToast('Ошибка загрузки LLM моделей: ' + e.message, 'error')
      }
    }

    async function loadEmbedding() {
      try {
        const data = await adminApi.getEmbeddingModels()
        embeddingModels.value = data.models || []
        activeEmbedding.value = data.active || ''
      } catch (e) {
        addToast('Ошибка загрузки Embedding моделей: ' + e.message, 'error')
      }
    }

    async function loadDocsPath() {
      try {
        const data = await adminApi.getDocumentsPath()
        docsPath.value = data.path || ''
        docsInfo.value = data
      } catch (e) {
        addToast('Ошибка загрузки пути документов: ' + e.message, 'error')
      }
    }

    async function onSelectLlm(modelId) {
      llmLoading.value = true
      try {
        await adminApi.setActiveLlm(modelId)
        activeLlm.value = modelId
        addToast('LLM сохранена', 'success')
      } catch (e) {
        addToast('Ошибка смены LLM: ' + e.message, 'error')
      } finally {
        llmLoading.value = false
      }
    }

    async function onSelectEmbedding(modelId) {
      const ok = window.confirm('Смена embedding потребует переиндексации. Продолжить?')
      if (!ok) return
      embeddingLoading.value = true
      try {
        await adminApi.setActiveEmbedding(modelId, true)
        activeEmbedding.value = modelId
        addToast('Embedding сохранена', 'success')
      } catch (e) {
        addToast('Ошибка смены Embedding: ' + e.message, 'error')
      } finally {
        embeddingLoading.value = false
      }
    }

    async function savePath() {
      pathSaving.value = true
      try {
        await adminApi.setDocumentsPath(docsPath.value)
        addToast('Путь сохранён', 'success')
        await loadDocsPath()
      } catch (e) {
        addToast('Ошибка сохранения пути: ' + e.message, 'error')
      } finally {
        pathSaving.value = false
      }
    }

    function formatBytes(bytes) {
      if (bytes == null) return '—'
      return (bytes / 1024 / 1024).toFixed(1) + ' MB'
    }

    onMounted(() => {
      loadLlm()
      loadEmbedding()
      loadDocsPath()
    })

    return {
      llmModels, activeLlm, llmLoading,
      embeddingModels, activeEmbedding, embeddingLoading,
      docsPath, docsInfo, pathSaving,
      onSelectLlm, onSelectEmbedding, savePath, formatBytes,
    }
  },
}
