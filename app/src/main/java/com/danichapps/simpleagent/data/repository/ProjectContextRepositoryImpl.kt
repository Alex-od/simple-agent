package com.danichapps.simpleagent.data.repository

import com.danichapps.simpleagent.data.remote.ProjectContextService
import com.danichapps.simpleagent.domain.model.ProjectContext
import com.danichapps.simpleagent.domain.repository.ProjectContextRepository

class ProjectContextRepositoryImpl(
    private val projectContextService: ProjectContextService
) : ProjectContextRepository {

    override suspend fun getProjectContext(): ProjectContext {
        val response = projectContextService.getProjectContext()
        return ProjectContext(
            branch = response.branch,
            changedFiles = response.changedFiles,
            gitRepository = response.gitRepository,
            projectRoot = response.projectRoot
        )
    }
}
