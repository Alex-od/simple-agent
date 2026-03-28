async function request(method, url, body) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body !== undefined) {
    opts.body = JSON.stringify(body)
  }
  const res = await fetch(url, opts)
  if (!res.ok) {
    let msg = res.statusText
    try {
      const err = await res.json()
      if (err.message) msg = err.message
    } catch (_) {}
    throw new Error(`${res.status}: ${msg}`)
  }
  if (res.status === 204) return null
  const contentLength = res.headers.get('Content-Length')
  if (contentLength === '0') return null
  const text = await res.text()
  if (!text) return null
  return JSON.parse(text)
}

export const adminApi = {
  getStatus() {
    return request('GET', '/api/v1/admin/status')
  },

  getLlmModels() {
    return request('GET', '/api/v1/admin/llm/models')
  },

  setActiveLlm(modelId) {
    return request('PUT', '/api/v1/admin/llm/active', { modelId })
  },

  getEmbeddingModels() {
    return request('GET', '/api/v1/admin/embedding/models')
  },

  setActiveEmbedding(modelId, confirmReindex) {
    return request('PUT', '/api/v1/admin/embedding/active', { modelId, confirmReindex })
  },

  getDocumentsPath() {
    return request('GET', '/api/v1/admin/rag/documents-path')
  },

  setDocumentsPath(path) {
    return request('PUT', '/api/v1/admin/rag/documents-path', { path: path.replace(/\\/g, '/') })
  },

  startIndexing() {
    return request('POST', '/api/v1/admin/rag/indexing')
  },

  getIndexingStatus() {
    return request('GET', '/api/v1/admin/rag/indexing/status')
  },
}
