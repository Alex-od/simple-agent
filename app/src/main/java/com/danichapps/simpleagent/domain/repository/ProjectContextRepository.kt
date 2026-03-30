package com.danichapps.simpleagent.domain.repository

import com.danichapps.simpleagent.domain.model.ProjectContext

interface ProjectContextRepository {
    suspend fun getProjectContext(): ProjectContext
}
