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
        <div class="form-group" style="margin-top: 12px;">
          <label>Путь к LLM моделям (.gguf)</label>
          <div style="display: flex; gap: 8px;">
            <input class="input" v-model="llmModelsPath" style="flex: 1;" placeholder="Путь к директории с .gguf файлами" />
            <button class="btn btn-primary btn-sm" @click="saveLlmModelsPath" :disabled="llmPathSaving">
              Сохранить
            </button>
          </div>
        </div>
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
        <div class="form-group" style="margin-top: 12px;">
          <label>Путь к Embedding моделям</label>
          <div style="display: flex; gap: 8px;">
            <input class="input" v-model="embeddingModelsPath" style="flex: 1;" placeholder="Путь к директории с embedding моделями" />
            <button class="btn btn-primary btn-sm" @click="saveEmbeddingModelsPath" :disabled="embeddingPathSaving">
              Сохранить
            </button>
          </div>
        </div>
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

    const llmModelsPath = ref('')
    const llmPathSaving = ref(false)

    const embeddingModelsPath = ref('')
    const embeddingPathSaving = ref(false)

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

    async function loadLlmModelsPath() {
      try {
        const data = await adminApi.getLlmModelsPath()
        llmModelsPath.value = data?.path || ''
      } catch (e) {
        addToast('Ошибка загрузки пути LLM моделей: ' + e.message, 'error')
      }
    }

    async function saveLlmModelsPath() {
      llmPathSaving.value = true
      try {
        await adminApi.setLlmModelsPath(llmModelsPath.value)
        addToast('Путь к LLM моделям сохранён', 'success')
        await loadLlm()
      } catch (e) {
        addToast('Ошибка сохранения пути LLM: ' + e.message, 'error')
      } finally {
        llmPathSaving.value = false
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

    async function loadEmbeddingModelsPath() {
      try {
        const data = await adminApi.getEmbeddingModelsPath()
        embeddingModelsPath.value = data?.path || ''
      } catch (e) {
        addToast('Ошибка загрузки пути Embedding моделей: ' + e.message, 'error')
      }
    }

    async function saveEmbeddingModelsPath() {
      embeddingPathSaving.value = true
      try {
        await adminApi.setEmbeddingModelsPath(embeddingModelsPath.value)
        addToast('Путь к Embedding моделям сохранён', 'success')
      } catch (e) {
        addToast('Ошибка сохранения пути Embedding: ' + e.message, 'error')
      } finally {
        embeddingPathSaving.value = false
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
      loadLlmModelsPath()
      loadEmbedding()
      loadEmbeddingModelsPath()
      loadDocsPath()
    })

    return {
      llmModels, activeLlm, llmLoading,
      llmModelsPath, llmPathSaving,
      embeddingModels, activeEmbedding, embeddingLoading,
      embeddingModelsPath, embeddingPathSaving,
      docsPath, docsInfo, pathSaving,
      onSelectLlm, onSelectEmbedding,
      saveLlmModelsPath, saveEmbeddingModelsPath,
      savePath, formatBytes,
    }
  },
}
