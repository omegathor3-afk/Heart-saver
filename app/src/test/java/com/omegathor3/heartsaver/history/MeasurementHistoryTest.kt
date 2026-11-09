package com.omegathor3.heartsaver.history
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class MeasurementHistoryTest {
 @Test fun roundTripsRecords(){val records=listOf(MeasurementRecord(1_800_000_000_000,72,0.81),MeasurementRecord(1_800_000_060_000,75,0.74));assertEquals(records,MeasurementHistory.decode(MeasurementHistory.encode(records)))}
 @Test fun ignoresMalformedAndOutOfRangeRecords(){val d=MeasurementHistory.decode("broken\n1800000000000,220,0.8\n1800000000001,68,1.4\n1800000000002,68,0.8");assertEquals(1,d.size);assertEquals(68,d.single().bpm)}
 @Test fun emptyHistoryIsEmpty(){assertTrue(MeasurementHistory.decode("").isEmpty())}
 @Test fun historyIsBoundedToFiftyNewestValidEntries(){val rows=(1L..65L).joinToString("\n"){ "${1_800_000_000_000L+it},${60+it.toInt()},0.8" };val decoded=MeasurementHistory.decode(rows);assertEquals(50,decoded.size);assertEquals(1_800_000_000_065L,decoded.last().timestampMillis)}
 @Test fun encoderDropsInvalidAndOldEntries(){val rows=(1L..55L).map{MeasurementRecord(1_800_000_000_000L+it,70,0.8)}+MeasurementRecord(1_800_000_000_100L,220,0.8);val decoded=MeasurementHistory.decode(MeasurementHistory.encode(rows));assertEquals(50,decoded.size);assertEquals(1_800_000_000_006L,decoded.first().timestampMillis)}
}