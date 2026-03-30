package com.danichapps.ragserver.project

import com.danichapps.ragserver.project.dto.ProjectContextResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/project")
class ProjectContextController(
    private val projectGitService: ProjectGitService
) {

    private val log = LoggerFactory.getLogger(ProjectContextController::class.java)

    @GetMapping("/context")
    fun getContext(): ProjectContextResponse {
        val gitRepository = projectGitService.isGitRepository()
        val branch = projectGitService.getCurrentBranch()
        val changedFiles = projectGitService.getChangedFiles()
        log.info(
            "Project context requested: gitRepository={}, branch={}, changedFiles={}",
            gitRepository,
            branch,
            changedFiles.size
        )
        return ProjectContextResponse(
            branch = branch,
            changedFiles = changedFiles,
            gitRepository = gitRepository,
            projectRoot = projectGitService.getProjectRoot()
        )
    }
}
