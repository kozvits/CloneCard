package com.kozvits.clonecard.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DumpDao {
    @Query("SELECT * FROM dumps ORDER BY timestamp DESC")
    fun getAllDumps(): Flow<List<DumpEntity>>

    @Query("SELECT * FROM dumps WHERE id = :id")
    suspend fun getDumpById(id: Long): DumpEntity?

    @Query("SELECT * FROM dumps WHERE uid = :uid LIMIT 1")
    suspend fun getDumpByUid(uid: String): DumpEntity?

    @Query("SELECT * FROM dumps WHERE isSimulation = 1 LIMIT 1")
    suspend fun getSimulationDump(): DumpEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDump(dump: DumpEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDumps(dumps: List<DumpEntity>)

    @Update
    suspend fun updateDump(dump: DumpEntity)

    @Delete
    suspend fun deleteDump(dump: DumpEntity)

    @Query("DELETE FROM dumps WHERE id = :id")
    suspend fun deleteDumpById(id: Long)

    @Query("DELETE FROM dumps")
    suspend fun deleteAllDumps()

    @Query("DELETE FROM dumps WHERE isSimulation = 1")
    suspend fun deleteSimulationDumps()
}
