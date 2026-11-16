package com.periodtracker.domain

import com.periodtracker.data.backup.*
import com.periodtracker.reminders.ReminderPolicy
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class BackupTest {
    private val today = LocalDate.of(2026, 11, 16)
    private val sample = JournalSnapshot(listOf(Period("one", today.minusDays(26))),
        listOf(DailyLog(today, flow = FlowLevel.NONE, symptoms = setOf(Symptom.CRAMPS), noSymptomsExplicit = false)),
        UserPreferences(onboardingComplete = true, dailyEnabled = true, upcomingEnabled = true))
    @Test fun jsonRoundTripPreservesExplicitNoneAndSettings() {
        assertEquals(sample, JournalJson.decode(JournalJson.encode(sample), today))
    }
    @Test fun encryptionIsRandomAndRoundTrips() {
        val cipher = BackupCipher()
        val plain = JournalJson.encode(sample)
        val a = cipher.encrypt(plain, "a long passphrase".toCharArray())
        val b = cipher.encrypt(plain, "a long passphrase".toCharArray())
        assertFalse(a.contentEquals(b))
        assertArrayEquals(plain, cipher.decrypt(a, "a long passphrase".toCharArray()))
        assertThrows(Exception::class.java) { cipher.decrypt(a, "wrong password".toCharArray()) }
        a[a.lastIndex] = (a.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { cipher.decrypt(a, "a long passphrase".toCharArray()) }
    }
    @Test fun unsupportedSchemaAndDuplicateRecordsFailBeforeRestore() {
        val json = JournalJson.encode(sample).toString(Charsets.UTF_8)
        assertThrows(IllegalArgumentException::class.java) { JournalJson.decode(json.replace("\"schemaVersion\":1", "\"schemaVersion\":2").toByteArray(), today) }
        assertThrows(IllegalArgumentException::class.java) { JournalJson.validate(sample.copy(logs = sample.logs + sample.logs), today) }
    }
    @Test fun remindersRespectTimeSavedLogsAndOngoingPeriods() {
        val noLog = sample.copy(logs = emptyList())
        assertEquals(2, ReminderPolicy.due(noLog, today.atTime(20, 0)).size)
        assertTrue(ReminderPolicy.due(noLog, today.atTime(19, 59)).isEmpty())
        assertEquals(1, ReminderPolicy.due(sample, today.atTime(20, 0)).size)
        assertEquals(1, ReminderPolicy.due(noLog.copy(periods = noLog.periods.map { it.copy(status = PeriodStatus.ONGOING) }), today.atTime(20, 0)).size)
        assertEquals(ReminderPolicy.due(noLog, today.atTime(20, 0)), ReminderPolicy.due(noLog, today.atTime(22, 0)))
    }
}

