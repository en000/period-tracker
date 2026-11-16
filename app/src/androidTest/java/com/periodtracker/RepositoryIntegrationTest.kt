package com.periodtracker

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.periodtracker.data.repository.OfflineJournalRepository
import com.periodtracker.data.source.*
import com.periodtracker.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RepositoryIntegrationTest {
    @Test fun deletionRetainsIndependentLogsAndFailedRestoreLeavesDataUntouched() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java).build()
        try {
            val repository = OfflineJournalRepository(database, PreferencesDataSource(context))
            val today = LocalDate.now()
            repository.savePeriod(Period("a", today))
            repository.saveLog(DailyLog(today, flow = FlowLevel.NONE))
            repository.deletePeriod("a")
            assertEquals(1, repository.snapshot().logs.size)
            val before = repository.snapshot()
            try {
                repository.replace(before.copy(periods = listOf(Period("future", today.plusDays(1)))))
                fail("Future record accepted")
            } catch (_: IllegalArgumentException) { }
            assertEquals(before, repository.snapshot())
        } finally { database.close() }
    }
}
