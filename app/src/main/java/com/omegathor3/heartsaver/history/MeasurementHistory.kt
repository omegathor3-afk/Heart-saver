package com.omegathor3.heartsaver.history

import android.content.Context
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class MeasurementRecord(
    val timestampMillis: Long,
    val bpm: Int,
    val confidence: Double
)

data class HistorySummary(
    val count: Int,
    val averageBpm: Int,
    val minimumBpm: Int,
    val maximumBpm: Int,
    val changeBpm: Int
)

object MeasurementHistory {
    private const val PREFS = "measurement_history"
    private const val KEY_RECORDS = "records"
    const val MAX_RECORDS = 50

    private val timestampFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    fun load(context: Context): List<MeasurementRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_RECORDS, "").orEmpty()
        return decode(raw)
    }

    fun save(context: Context, record: MeasurementRecord) {
        require(isValid(record)) { "Measurement record is outside supported bounds" }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val records = (decode(prefs.getString(KEY_RECORDS, "").orEmpty()) + record)
            .sortedBy { it.timestampMillis }
            .takeLast(MAX_RECORDS)
        prefs.edit().putString(KEY_RECORDS, encode(records)).apply()
    }

    fun summarize(records: List<MeasurementRecord>): HistorySummary? {
        val valid = records.filter(::isValid).sortedBy { it.timestampMillis }
        if (valid.isEmpty()) return null
        val values = valid.map { it.bpm }
        return HistorySummary(
            count = valid.size,
            averageBpm = values.average().roundToInt(),
            minimumBpm = values.min(),
            maximumBpm = values.max(),
            changeBpm = values.last() - values.first()
        )
    }

    fun toCsv(records: List<MeasurementRecord>): String {
        val rows = records.filter(::isValid).sortedBy { it.timestampMillis }
        return buildString {
            append("timestamp_utc,bpm,correlation_confidence\r\n")
            rows.forEach { record ->
                append(timestampFormatter.format(Instant.ofEpochMilli(record.timestampMillis)))
                append(',')
                append(record.bpm)
                append(',')
                append(String.format(Locale.US, "%.4f", record.confidence))
                append("\r\n")
            }
        }
    }

    internal fun encode(records: List<MeasurementRecord>): String =
        records.filter(::isValid)
            .sortedBy { it.timestampMillis }
            .takeLast(MAX_RECORDS)
            .joinToString("\n") { "${it.timestampMillis},${it.bpm},${it.confidence}" }

    internal fun decode(raw: String): List<MeasurementRecord> =
        raw.lineSequence().mapNotNull { line ->
            val fields = line.split(',')
            if (fields.size != 3) return@mapNotNull null
            val timestamp = fields[0].toLongOrNull() ?: return@mapNotNull null
            val bpm = fields[1].toIntOrNull() ?: return@mapNotNull null
            val confidence = fields[2].toDoubleOrNull() ?: return@mapNotNull null
            MeasurementRecord(timestamp, bpm, confidence).takeIf(::isValid)
        }.sortedBy { it.timestampMillis }.takeLast(MAX_RECORDS)

    private fun isValid(record: MeasurementRecord): Boolean =
        record.timestampMillis > 0L &&
            record.bpm in 40..180 &&
            record.confidence.isFinite() &&
            record.confidence in 0.0..1.0
}
