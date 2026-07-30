package com.kozvits.clonecard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "dumps")
@TypeConverters(DumpConverters::class)
data class DumpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val uidBytes: List<Int>,
    val blocks: List<Int>,
    val label: String = "",
    val fileName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isMagicCard: Boolean = false,
    val isSimulation: Boolean = false
)

class DumpConverters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isEmpty()) emptyList()
        else value.split(",").mapNotNull { it.trim().toIntOrNull() }
}
