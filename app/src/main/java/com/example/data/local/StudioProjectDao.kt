package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StudioProject
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioProjectDao {
    @Query("SELECT * FROM studio_projects ORDER BY dateCreated DESC")
    fun getAllProjects(): Flow<List<StudioProject>>

    @Query("SELECT * FROM studio_projects ORDER BY dateCreated DESC LIMIT :limit")
    fun getRecentProjects(limit: Int): Flow<List<StudioProject>>

    @Query("SELECT * FROM studio_projects WHERE toolType = :toolType ORDER BY dateCreated DESC")
    fun getProjectsByTool(toolType: String): Flow<List<StudioProject>>

    @Query("SELECT * FROM studio_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): StudioProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: StudioProject): Long

    @Update
    suspend fun updateProject(project: StudioProject)

    @Delete
    suspend fun deleteProject(project: StudioProject)

    @Query("DELETE FROM studio_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM studio_projects")
    fun getTotalProjectsCount(): Flow<Int>
}
