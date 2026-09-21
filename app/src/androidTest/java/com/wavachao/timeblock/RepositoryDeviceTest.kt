package com.wavachao.timeblock

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wavachao.timeblock.data.OfflineTimeBlockRepository
import com.wavachao.timeblock.data.PlanHorizon
import com.wavachao.timeblock.data.local.TimeBlockDatabase
import com.wavachao.timeblock.data.model.RecurrenceRule
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.data.model.TimeBlockDraft
import com.wavachao.timeblock.ui.toBlock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RepositoryDeviceTest {
    @Test fun versionOneUpgradePreservesExistingSchedule() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { legacy ->
            legacy.execSQL("CREATE TABLE IF NOT EXISTS `time_blocks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `startEpochMillis` INTEGER NOT NULL, `endEpochMillis` INTEGER NOT NULL, `startLocal` TEXT NOT NULL, `endLocal` TEXT NOT NULL, `category` TEXT NOT NULL, `done` INTEGER NOT NULL, `notes` TEXT, `reminderMinutes` INTEGER NOT NULL, `recurrence` TEXT NOT NULL, `subtasks` TEXT NOT NULL DEFAULT '', `createdAt` INTEGER NOT NULL)")
            legacy.execSQL("INSERT INTO time_blocks (id,title,startEpochMillis,endEpochMillis,startLocal,endLocal,category,done,notes,reminderMinutes,recurrence,createdAt) VALUES (1,'升级保留',0,3600000,'2026-09-19T09:00','2026-09-19T10:00','WORK',0,'原有备注',-1,'NONE',123)")
            legacy.version = 1
        }
        val db = Room.databaseBuilder(context, TimeBlockDatabase::class.java, name).addMigrations(TimeBlockDatabase.MIGRATION_1_2).build()
        try {
            val repo = OfflineTimeBlockRepository(db.timeBlockDao())
            val old = requireNotNull(repo.blockById(1))
            assertEquals("升级保留", old.title)
            assertEquals("原有备注", old.notes)
            assertFalse(old.allDay)
            val day = LocalDate.now().plusYears(2)
            repo.save(TimeBlock(title="两年后的事项",start=day.atStartOfDay(),end=day.plusDays(1).atStartOfDay(),allDay=true))
            assertEquals(2,repo.observeAll().first().size)
            assertTrue(repo.observeAll().first().last().allDay)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun roomCreateCompleteEditObserveDelete() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, TimeBlockDatabase::class.java).build()
        try {
            val repo = OfflineTimeBlockRepository(db.timeBlockDao())
            val date = LocalDate.of(2026, 9, 20)
            val original = TimeBlock(title = "真实数据库测试", start = date.atTime(9, 0), end = date.atTime(10, 0), createdAt = 123)
            val id = repo.save(original)
            repo.setDone(id, true)
            val draft = TimeBlockDraft.from(requireNotNull(repo.blockById(id))).copy(notes = "保存备注")
            repo.save(draft.toBlock(), PlanHorizon.None)
            val observed = requireNotNull(repo.observeBlock(id).first())
            assertTrue(observed.done)
            assertEquals(123L, observed.createdAt)
            assertEquals("保存备注", observed.notes)
            assertEquals(1, repo.observeDay(date).first().size)
            repo.delete(id)
            assertNull(repo.observeBlock(id).first())
        } finally { db.close() }
    }

    @Test fun recurrenceUsesSelectedFutureDateAsHorizon() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, TimeBlockDatabase::class.java).build()
        try {
            val repo = OfflineTimeBlockRepository(db.timeBlockDao())
            val date = LocalDate.now().plusMonths(3)
            repo.save(TimeBlock(title = "未来重复", start = date.atTime(9, 0), end = date.atTime(10, 0), recurrence = RecurrenceRule.DAILY))
            val rows = repo.rangeBlocks(date, date.plusDays(40))
            assertEquals(28, rows.size)
            assertEquals(date, rows.first().date)
            assertEquals(date.plusDays(27), rows.last().date)
        } finally { db.close() }
    }
}
