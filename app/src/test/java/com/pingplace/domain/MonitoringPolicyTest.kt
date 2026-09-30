package com.pingplace.domain

import com.pingplace.data.local.entity.BrandVisitStateEntity
import org.junit.Assert.*
import org.junit.Test

class MonitoringPolicyTest {
    @Test fun `highway travel requests a fix before passing a small store radius`() {
        val speed = 30f
        assertTrue(speed * MonitoringPolicy.intervalMillis(speed) / 1000 < 250)
        assertTrue(MonitoringPolicy.intervalMillis(speed) < MonitoringPolicy.intervalMillis(1.4f))
        assertTrue(MonitoringPolicy.intervalMillis(1.4f) < MonitoringPolicy.intervalMillis(0f))
    }

    @Test fun `driving gives advance notice without shrinking a larger user radius`() {
        assertEquals(1350.0, MonitoringPolicy.approachDistanceMeters(250, 30f), 0.1)
        assertEquals(8046.0, MonitoringPolicy.approachDistanceMeters(8046, 30f), 0.1)
        assertEquals(2000.0, MonitoringPolicy.approachDistanceMeters(250, 70f), 0.1)
        assertEquals(250.0, MonitoringPolicy.approachDistanceMeters(250, 1.4f), 0.1)
    }

    @Test fun `unsent alerts remain eligible even when a visit was marked active`() {
        val previous = BrandVisitStateEntity("store", placeId = "a", isActive = true)
        assertTrue(MonitoringPolicy.shouldAlert(previous, "a", 1000, 0f))
    }

    @Test fun `unfinished driving errands repeat while GPS boundary jitter is debounced`() {
        val previous = BrandVisitStateEntity("store", placeId = "a", isActive = false, lastNotifiedAtEpochMillis = 1000)
        assertFalse(MonitoringPolicy.shouldAlert(previous, "a", 6000, 30f))
        assertFalse(MonitoringPolicy.shouldAlert(previous, "a", 180999, 30f))
        assertTrue(MonitoringPolicy.shouldAlert(previous, "a", 181000, 30f))
        assertFalse(MonitoringPolicy.shouldAlert(previous, "a", 181000, 0f))
        assertTrue(MonitoringPolicy.shouldAlert(previous, "a", 601000, 0f))
    }

    @Test fun `a different store can alert during the previous store cooldown`() {
        val previous = BrandVisitStateEntity("store", placeId = "a", lastNotifiedAtEpochMillis = 1000)
        assertTrue(MonitoringPolicy.shouldAlert(previous, "b", 6000, 30f))
    }
}
