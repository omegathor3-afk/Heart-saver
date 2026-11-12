package com.omegathor3.heartsaver.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertNull(MeasurementHistory.summarize(emptyList()))
    }

    @Test fun historyIsBoundedToFiftyNewestValidEntries() {
        val rows = (1L..65L).joinToString("\n") {
            "${1_800_000_000_000L + it},${60 + it.toInt()},0.8"
        }
        val decoded = MeasurementHistory.decode(rows)
        assertEquals(50, decoded.size)
        assertEquals(1_800_000_000_065L, decoded.last().timestampMillis)
    }

    @Test fun encoderDropsInvalidAndOldEntries() {
        val rows = (1L..55L).map {
            MeasurementRecord(1_800_000_000_000L + it, 70, 0.8)
        } + MeasurementRecord(1_800_000_000_100L, 220, 0.8)
        val decoded = MeasurementHistory.decode(MeasurementHistory.encode(rows))
        assertEquals(50, decoded.size)
        assertEquals(1_800_000_000_006L, decoded.first().timestampMillis)
    }

    @Test fun summaryUsesChronologicalValidRecordsOnly() {
        val rows = listOf(
            MeasurementRecord(300L, 90, 0.8),
            MeasurementRecord(100L, 70, 0.7),
            MeasurementRecord(200L, 80, 0.9),
            MeasurementRecord(400L, 190, 0.9)
        )
        val summary = MeasurementHistory.summarize(rows)!!
        assertEquals(3, summary.count)
        assertEquals(80, summary.averageBpm)
        assertEquals(70, summary.minimumBpm)
        assertEquals(90, summary.maximumBpm)
        assertEquals(20, summary.changeBpm)
    }

    @Test fun csvIsChronologicalLocaleIndependentAndHasHeader() {
        val csv = MeasurementHistory.toCsv(
            listOf(
                MeasurementRecord(1_800_000_060_000, 75, 0.74621),
                MeasurementRecord(1_800_000_000_000, 72, 0.81),
                MeasurementRecord(1L, 220, 0.9)
            )
        )
        val lines = csv.trim().lines()
        assertEquals("timestamp_utc,bpm,correlation_confidence", lines.first())
        assertTrue(lines[1].contains(",72,0.8100"))
        assertTrue(lines[2].contains(",75,0.7462"))
        assertEquals(3, lines.size)
    }

    @Test fun encodeSortsOutOfOrderRecordsBeforeBounding() {
        val records = (1L..55L).map {
            MeasurementRecord(10_000L + it, 70, 0.8)
        }.reversed()
        val decoded = MeasurementHistory.decode(MeasurementHistory.encode(records))
        assertEquals(50, decoded.size)
        assertEquals(10_006L, decoded.first().timestampMillis)
        assertEquals(10_055L, decoded.last().timestampMillis)
    }
}
