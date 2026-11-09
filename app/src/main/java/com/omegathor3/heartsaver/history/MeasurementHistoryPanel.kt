package com.omegathor3.heartsaver.history
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
private val HistoryPanel=Color(0xFF151E28)
private val HistoryMuted=Color(0xFF9AA8B7)
@Composable
fun MeasurementHistoryPanel(records:List<MeasurementRecord>){
 Column(Modifier.fillMaxWidth().background(HistoryPanel,RoundedCornerShape(14.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Text("RECENT RESULTS",color=HistoryMuted,fontSize=10.sp,letterSpacing=1.4.sp,fontWeight=FontWeight.Bold)
   Text("${records.size}/${MeasurementHistory.MAX_RECORDS} saved",color=HistoryMuted,fontSize=10.sp)
  }
  if(records.isEmpty()) Text("Completed measurements will appear here.",color=HistoryMuted,fontSize=11.sp)
  else records.takeLast(3).asReversed().forEach{record->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
    Column(verticalArrangement=Arrangement.spacedBy(1.dp)){
     Text("${record.bpm} BPM",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
     Text("Signal confidence ${(record.confidence*100).toInt()}%",color=HistoryMuted,fontSize=10.sp)
    }
    Text(SimpleDateFormat("MMM d, HH:mm",Locale.getDefault()).format(Date(record.timestampMillis)),color=HistoryMuted,fontSize=11.sp)
   }
  }
 }
}