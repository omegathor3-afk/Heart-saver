package com.omegathor3.heartsaver.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementHistoryTest {
    @Test fun roundTripsRecords() {
        val records = listOf(
            MeasurementRecord(1_800_000_000_000, 72, 0.81),
            MeasurementRecord(1_800_000_060_000, 75, 0.74)
        )
        assertEquals(records, MeasurementHistory.decode(MeasurementHistory.encode(records)))
    }

    @Test fun ignoresMalformedAndOutOfRangeRecords() {
        val decoded = MeasurementHistory.decode(
            "broken\n1800000000000,220,0.8\n1800000000001,68,1.4\n1800000000002,68,0.8"
        )
        assertEquals(1, decoded.size)
        assertEquals(68, decoded.single().bpm)
    }

    @Test fun emptyHistoryIsEmpty() {
        assertTrue(MeasurementHistory.decode("").isEmpty())
    }
}
