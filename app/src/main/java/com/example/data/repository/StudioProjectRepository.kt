package com.example.data.repository

import com.example.data.local.StudioProjectDao
import com.example.data.model.StudioProject
import kotlinx.coroutines.flow.Flow

class StudioProjectRepository(private val dao: StudioProjectDao) {
    val allProjects: Flow<List<StudioProject>> = dao.getAllProjects()
    val totalCount: Flow<Int> = dao.getTotalProjectsCount()

    fun getRecentProjects(limit: Int = 5): Flow<List<StudioProject>> =
        dao.getRecentProjects(limit)

    fun getProjectsByTool(toolType: String): Flow<List<StudioProject>> =
        dao.getProjectsByTool(toolType)

    suspend fun getProjectById(id: Long): StudioProject? =
        dao.getProjectById(id)

    suspend fun insertProject(project: StudioProject): Long =
        dao.insertProject(project)

    suspend fun updateProject(project: StudioProject) =
        dao.updateProject(project)

    suspend fun deleteProject(project: StudioProject) =
        dao.deleteProject(project)

    suspend fun deleteProjectById(id: Long) =
        dao.deleteProjectById(id)
}
