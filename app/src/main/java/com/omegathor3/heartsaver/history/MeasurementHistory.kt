package com.omegathor3.heartsaver.history
import android.content.Context
data class MeasurementRecord(val timestampMillis:Long,val bpm:Int,val confidence:Double)
object MeasurementHistory {
 private const val PREFS="measurement_history";private const val KEY_RECORDS="records";const val MAX_RECORDS=50
 fun load(context:Context):List<MeasurementRecord>{val raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_RECORDS,"").orEmpty();return decode(raw)}
 fun save(context:Context,record:MeasurementRecord){require(isValid(record)){"Measurement record is outside supported bounds"};val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);val records=(decode(prefs.getString(KEY_RECORDS,"").orEmpty())+record).takeLast(MAX_RECORDS);prefs.edit().putString(KEY_RECORDS,encode(records)).apply()}
 internal fun encode(records:List<MeasurementRecord>):String=records.filter(::isValid).takeLast(MAX_RECORDS).joinToString("\n"){"${it.timestampMillis},${it.bpm},${it.confidence}"}
 internal fun decode(raw:String):List<MeasurementRecord> = raw.lineSequence().mapNotNull{line->val f=line.split(',');if(f.size!=3)return@mapNotNull null;val t=f[0].toLongOrNull()?:return@mapNotNull null;val b=f[1].toIntOrNull()?:return@mapNotNull null;val c=f[2].toDoubleOrNull()?:return@mapNotNull null;MeasurementRecord(t,b,c).takeIf(::isValid)}.toList().takeLast(MAX_RECORDS)
 private fun isValid(r:MeasurementRecord)=r.timestampMillis>0L&&r.bpm in 40..180&&r.confidence.isFinite()&&r.confidence in 0.0..1.0
}