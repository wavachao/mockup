package com.wavachao.timeblock.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Per-range row used to draw one block (start/end are epoch millis). */
data class BlockRange(
    val id: Long,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val category: String,
    val done: Boolean,
)

/** Per-category totals for the insights screen. */
data class CategoryTotal(
    val category: String,
    val totalMillis: Long,
    val blockCount: Int,
)

@Dao
interface TimeBlockDao {

    @Query(
        """
        SELECT * FROM time_blocks
        WHERE startEpochMillis < :endExclusive AND endEpochMillis > :startInclusive
        ORDER BY startEpochMillis ASC
        """,
    )
    fun observeBetween(startInclusive: Long, endExclusive: Long): Flow<List<TimeBlockEntity>>

    @Query(
        """
        SELECT * FROM time_blocks
        WHERE startEpochMillis < :endExclusive AND endEpochMillis > :startInclusive
        ORDER BY startEpochMillis ASC
        """,
    )
    suspend fun between(startInclusive: Long, endExclusive: Long): List<TimeBlockEntity>

    @Query("SELECT * FROM time_blocks WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<TimeBlockEntity?>

    @Query("SELECT * FROM time_blocks WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TimeBlockEntity?

    @Query(
        """
        SELECT * FROM time_blocks
        WHERE startEpochMillis < :endExclusive AND endEpochMillis > :startInclusive
        ORDER BY startEpochMillis ASC
        """,
    )
    suspend fun inRange(startInclusive: Long, endExclusive: Long): List<TimeBlockEntity>

    @Query("SELECT * FROM time_blocks WHERE done = 0 AND endEpochMillis > :now ORDER BY startEpochMillis ASC")
    suspend fun upcoming(now: Long): List<TimeBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(block: TimeBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(blocks: List<TimeBlockEntity>): List<Long>

    @Update
    suspend fun update(block: TimeBlockEntity)

    @Delete
    suspend fun delete(block: TimeBlockEntity)

    @Query("DELETE FROM time_blocks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE time_blocks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("SELECT COUNT(*) FROM time_blocks")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM time_blocks")
    fun observeTotalCount(): Flow<Int>
}
