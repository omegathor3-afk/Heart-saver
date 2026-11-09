package com.omegathor3.heartsaver.history

import android.content.Context

data class MeasurementRecord(
    val timestampMillis: Long,
    val bpm: Int,
    val confidence: Double
)

object MeasurementHistory {
    private const val PREFS = "measurement_history"
    private const val KEY_RECORDS = "records"
    private const val MAX_RECORDS = 50

    fun load(context: Context): List<MeasurementRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_RECORDS, "").orEmpty()
        return decode(raw)
    }

    fun save(context: Context, record: MeasurementRecord) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val records = (decode(prefs.getString(KEY_RECORDS, "").orEmpty()) + record)
            .takeLast(MAX_RECORDS)
        prefs.edit().putString(KEY_RECORDS, encode(records)).apply()
    }

    internal fun encode(records: List<MeasurementRecord>): String =
        records.joinToString("\n") { "${it.timestampMillis},${it.bpm},${it.confidence}" }

    internal fun decode(raw: String): List<MeasurementRecord> = raw.lineSequence()
        .mapNotNull { line ->
            val fields = line.split(',')
            if (fields.size != 3) return@mapNotNull null
            val timestamp = fields[0].toLongOrNull() ?: return@mapNotNull null
            val bpm = fields[1].toIntOrNull() ?: return@mapNotNull null
            val confidence = fields[2].toDoubleOrNull() ?: return@mapNotNull null
            if (timestamp <= 0L || bpm !in 40..180 || !confidence.isFinite() || confidence !in 0.0..1.0) {
                return@mapNotNull null
            }
            MeasurementRecord(timestamp, bpm, confidence)
        }.toList()
}
