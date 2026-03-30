package com.danichapps.ragserver.project

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectContextControllerTest {

    private val projectGitService = mockk<ProjectGitService>()
    private val controller = ProjectContextController(projectGitService)

    @Test
    fun `getContext returns branch changed files and project root`() {
        every { projectGitService.isGitRepository() } returns true
        every { projectGitService.getCurrentBranch() } returns "rag-and-gitmcp"
        every { projectGitService.getChangedFiles() } returns listOf(
            "README.md",
            "docs/project-structure.md",
            "rag-server/src/main/kotlin/com/danichapps/ragserver/project/ProjectGitService.kt"
        )
        every { projectGitService.getProjectRoot() } returns "C:\\StudioProjects\\SimpleAgent"

        val response = controller.getContext()

        assertTrue(response.gitRepository)
        assertEquals("rag-and-gitmcp", response.branch)
        assertEquals(
            listOf(
                "README.md",
                "docs/project-structure.md",
                "rag-server/src/main/kotlin/com/danichapps/ragserver/project/ProjectGitService.kt"
            ),
            response.changedFiles
        )
        assertEquals("C:\\StudioProjects\\SimpleAgent", response.projectRoot)
    }
}
