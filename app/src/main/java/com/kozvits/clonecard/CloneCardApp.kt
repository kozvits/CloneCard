package com.kozvits.clonecard

import android.app.Application
import com.kozvits.clonecard.data.SimulationData
import com.kozvits.clonecard.data.db.DumpDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CloneCardApp : Application() {

    val database by lazy { DumpDatabase.getInstance(this) }
    val repository by lazy { com.kozvits.clonecard.data.repository.DumpRepository.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        initSimulationData()
    }

    private fun initSimulationData() {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = repository.getSimulationDump()
            if (existing == null) {
                repository.saveDumps(SimulationData.defaultDumps)
            }
        }
    }
}
