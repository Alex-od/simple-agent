package com.danichapps.simpleagent.data.local

import com.danichapps.simpleagent.domain.model.RagChunk

data class ScoredChunk(val chunk: RagChunk, val similarity: Float)
