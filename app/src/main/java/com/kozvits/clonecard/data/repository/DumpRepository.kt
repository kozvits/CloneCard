package com.kozvits.clonecard.data.repository

import android.content.Context
import com.kozvits.clonecard.data.db.DumpDao
import com.kozvits.clonecard.data.db.DumpDatabase
import com.kozvits.clonecard.data.db.DumpEntity
import kotlinx.coroutines.flow.Flow

class DumpRepository private constructor(
    private val dao: DumpDao
) {
    val allDumps: Flow<List<DumpEntity>> = dao.getAllDumps()

    suspend fun getDumpById(id: Long): DumpEntity? = dao.getDumpById(id)

    suspend fun getDumpByUid(uid: String): DumpEntity? = dao.getDumpByUid(uid)

    suspend fun getSimulationDump(): DumpEntity? = dao.getSimulationDump()

    suspend fun saveDump(dump: DumpEntity): Long = dao.insertDump(dump)

    suspend fun saveDumps(dumps: List<DumpEntity>) = dao.insertDumps(dumps)

    suspend fun updateDump(dump: DumpEntity) = dao.updateDump(dump)

    suspend fun deleteDumpById(id: Long) = dao.deleteDumpById(id)

    suspend fun deleteAllDumps() = dao.deleteAllDumps()

    suspend fun deleteSimulationDumps() = dao.deleteSimulationDumps()

    companion object {
        @Volatile
        private var INSTANCE: DumpRepository? = null

        fun getInstance(context: Context): DumpRepository {
            return INSTANCE ?: synchronized(this) {
                val db = DumpDatabase.getInstance(context)
                INSTANCE ?: DumpRepository(db.dumpDao()).also { INSTANCE = it }
            }
        }
    }
}
