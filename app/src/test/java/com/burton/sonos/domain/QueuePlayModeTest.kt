package com.burton.sonos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class QueuePlayModeTest {
    @Test
    fun mapsSonosPlayModes() {
        assertEquals(QueuePlayMode(false, RepeatMode.OFF), QueuePlayMode.fromSonos("NORMAL"))
        assertEquals(QueuePlayMode(false, RepeatMode.ALL), QueuePlayMode.fromSonos("REPEAT_ALL"))
        assertEquals(QueuePlayMode(false, RepeatMode.ONE), QueuePlayMode.fromSonos("REPEAT_ONE"))
        assertEquals(QueuePlayMode(true, RepeatMode.OFF), QueuePlayMode.fromSonos("SHUFFLE_NOREPEAT"))
        assertEquals(QueuePlayMode(true, RepeatMode.ALL), QueuePlayMode.fromSonos("SHUFFLE"))
        assertEquals(QueuePlayMode(true, RepeatMode.ONE), QueuePlayMode.fromSonos("SHUFFLE_REPEAT_ONE"))
    }

    @Test
    fun cyclesRepeatAndKeepsShuffle() {
        val shuffled = QueuePlayMode(shuffle = true, repeat = RepeatMode.OFF)
        assertEquals("SHUFFLE_NOREPEAT", shuffled.toSonos())
        assertEquals("SHUFFLE", shuffled.cycleRepeat().toSonos())
        assertEquals("SHUFFLE_REPEAT_ONE", shuffled.cycleRepeat().cycleRepeat().toSonos())
        assertEquals("SHUFFLE_NOREPEAT", shuffled.cycleRepeat().cycleRepeat().cycleRepeat().toSonos())
    }

    @Test
    fun sleepTimerFormatsAndSelects() {
        assertEquals("", SleepTimer.sonosDuration(0))
        assertEquals("00:15:00", SleepTimer.sonosDuration(15 * 60))
        assertEquals("01:00:00", SleepTimer.sonosDuration(3600))
        assertEquals("02:00:00", SleepTimer.sonosDuration(7200))
        assertEquals(0, SleepTimer.selectedSeconds(0))
        assertEquals(15 * 60, SleepTimer.selectedSeconds(14 * 60))
        assertEquals(30 * 60, SleepTimer.selectedSeconds(28 * 60))
    }
}
