package com.pingplace.data.repository

import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.model.ReminderRepeatType
import com.pingplace.model.TriggerType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryPingPlaceRepositoryTest {

    @Test
    fun `completing non recurring reminder marks it completed`() = runBlocking {
        val repository = InMemoryPingPlaceRepository()
        val id = repository.saveReminder(reminder())

        repository.setReminderCompleted(id, true)

        assertEquals(true, repository.getReminder(id)?.isCompleted)
    }

    @Test
    fun `completing daily recurring reminder advances due date instead of archiving`() = runBlocking {
        val repository = InMemoryPingPlaceRepository()
        val created = reminder(
            dueDateEpochMillis = 1_700_000_000_000,
            repeatType = ReminderRepeatType.DAILY
        )
        val id = repository.saveReminder(created)

        repository.setReminderCompleted(id, true)

        val updated = repository.getReminder(id)
        assertNotNull(updated)
        assertFalse(updated!!.isCompleted)
        assertEquals(created.dueDateEpochMillis?.plus(86_400_000), updated.dueDateEpochMillis)
    }

    @Test
    fun `delete removes reminder`() = runBlocking {
        val repository = InMemoryPingPlaceRepository()
        val id = repository.saveReminder(reminder())

        repository.deleteReminder(id)

        assertNull(repository.getReminder(id))
    }

    private fun reminder(
        dueDateEpochMillis: Long? = null,
        repeatType: ReminderRepeatType = ReminderRepeatType.NONE
    ) = ReminderEntity(
        title = "Return package",
        brandName = "Whole Foods",
        brandQuery = "Whole Foods",
        triggerType = TriggerType.DISTANCE,
        triggerDistanceMeters = 1609,
        dueDateEpochMillis = dueDateEpochMillis,
        repeatType = repeatType,
        createdAtEpochMillis = 1_700_000_000_000,
        updatedAtEpochMillis = 1_700_000_000_000
    )
}
