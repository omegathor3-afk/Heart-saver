package com.omegathor3.heartsaver.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HistoryPanel = Color(0xFF151E28)
private val HistoryMuted = Color(0xFF9AA8B7)
private val HistoryMint = Color(0xFF75D7B0)

@Composable
fun MeasurementHistoryPanel(
    records: List<MeasurementRecord>,
    onExport: () -> Unit
) {
    val chronological = records.sortedBy { it.timestampMillis }
    val summary = MeasurementHistory.summarize(chronological)

    Column(
        Modifier.fillMaxWidth().background(HistoryPanel, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "MEASUREMENT HISTORY",
                color = HistoryMuted,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${chronological.size}/${MeasurementHistory.MAX_RECORDS}",
                color = HistoryMuted,
                fontSize = 10.sp
            )
        }

        if (summary == null) {
            Text("Completed measurements will appear here.", color = HistoryMuted, fontSize = 11.sp)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryValue("AVERAGE", "${summary.averageBpm}")
                SummaryValue("LOW–HIGH", "${summary.minimumBpm}–${summary.maximumBpm}")
                SummaryValue("CHANGE", signed(summary.changeBpm))
            }
            if (chronological.size >= 2) {
                BpmTrendChart(chronological)
            } else {
                Text("A second measurement will start your trend line.", color = HistoryMuted, fontSize = 10.sp)
            }
            Text("RECENT READINGS", color = HistoryMuted, fontSize = 9.sp, letterSpacing = 1.2.sp)
            chronological.takeLast(3).asReversed().forEach { record ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("${record.bpm} BPM", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Correlation ${(record.confidence * 100).toInt()}%", color = HistoryMuted, fontSize = 10.sp)
                    }
                    Text(
                        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(record.timestampMillis)),
                        color = HistoryMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263747)),
            shape = RoundedCornerShape(11.dp)
        ) {
            Text("Export history as CSV", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = HistoryMuted, fontSize = 8.sp, letterSpacing = 0.8.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BpmTrendChart(records: List<MeasurementRecord>) {
    val values = records.takeLast(MeasurementHistory.MAX_RECORDS).map { it.bpm }
    val minimum = (values.min() - 5).coerceAtLeast(35)
    val maximum = (values.max() + 5).coerceAtMost(185)
    val range = (maximum - minimum).coerceAtLeast(1)

    Canvas(
        Modifier.fillMaxWidth().height(72.dp)
            .background(Color(0xFF101821), RoundedCornerShape(9.dp)).padding(7.dp)
    ) {
        val top = 4f
        val bottom = size.height - 4f
        repeat(3) { index ->
            val y = top + (bottom - top) * index / 2f
            drawLine(
                color = Color(0xFF2A3947),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1f
            )
        }
        val path = Path()
        values.forEachIndexed { index, bpm ->
            val x = if (values.size == 1) size.width / 2f
                else index.toFloat() / (values.size - 1) * size.width
            val y = bottom - ((bpm - minimum).toFloat() / range) * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, HistoryMint, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        values.forEachIndexed { index, bpm ->
            val x = if (values.size == 1) size.width / 2f
                else index.toFloat() / (values.size - 1) * size.width
            val y = bottom - ((bpm - minimum).toFloat() / range) * (bottom - top)
            drawCircle(HistoryMint, radius = 2.5f, center = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
