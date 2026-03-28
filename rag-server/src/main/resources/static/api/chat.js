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
  return res.json()
}

export const chatApi = {
  sendMessage(message, useRag, topK) {
    return request('POST', '/api/v1/chat/completions', { message, useRag, topK })
  },

  ragSearch(query, topK) {
    return request('POST', '/api/v1/chat/rag/search', { query, top_k: topK })
  },

  getConfig() {
    return request('GET', '/api/v1/chat/config')
  },
}
