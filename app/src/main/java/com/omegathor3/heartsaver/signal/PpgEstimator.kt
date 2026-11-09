package com.omegathor3.heartsaver.signal
import kotlin.math.abs
import kotlin.math.sqrt
data class PulseEstimate(val bpm:Int,val confidence:Double,val quality:SignalQuality)
enum class SignalQuality { INSUFFICIENT_DATA, TOO_NOISY, USABLE }
/** Experimental camera-PPG estimator; not clinically validated. */
class PpgEstimator(private val sampleRateHz:Double=30.0,private val minBpm:Int=40,private val maxBpm:Int=180) {
 fun estimate(samples:List<Double>):PulseEstimate? {
  if(!sampleRateHz.isFinite()||sampleRateHz !in 10.0..120.0||minBpm<30||maxBpm>220||minBpm>=maxBpm||samples.size<(sampleRateHz*8.0).toInt()) return null
  if(samples.any{!it.isFinite()}) return null
  val mean=samples.average()
  if(!mean.isFinite()||mean<=1.0) return null
  val centered=samples.map{it-mean}
  val rawRms=sqrt(centered.sumOf{it*it}/centered.size)
  if(rawRms/mean<0.002) return null
  val radius=(sampleRateHz*0.75).toInt().coerceAtLeast(2)
  val detrended=DoubleArray(centered.size){i->val from=(i-radius).coerceAtLeast(0);val to=(i+radius).coerceAtMost(centered.lastIndex);centered[i]-centered.subList(from,to+1).average()}
  val relativeEnergy=sqrt(detrended.sumOf{it*it}/detrended.size)/mean
  if(!relativeEnergy.isFinite()||relativeEnergy<0.001) return null
  val minLag=(sampleRateHz*60.0/maxBpm).toInt().coerceAtLeast(1)
  val maxLag=(sampleRateHz*60.0/minBpm).toInt().coerceAtMost(detrended.size/3)
  if(maxLag<=minLag) return null
  val correlations=DoubleArray(maxLag+1){Double.NEGATIVE_INFINITY}
  var bestLag=-1;var bestCorrelation=Double.NEGATIVE_INFINITY
  for(lag in minLag..maxLag){
   var cross=0.0;var left=0.0;var right=0.0
   for(i in 0 until detrended.size-lag){val a=detrended[i];val b=detrended[i+lag];cross+=a*b;left+=a*a;right+=b*b}
   val denominator=sqrt(left*right);val correlation=if(denominator>0.0)cross/denominator else 0.0
   correlations[lag]=correlation
   if(correlation>bestCorrelation){bestCorrelation=correlation;bestLag=lag}
  }
  if(bestLag<=0||bestCorrelation<0.35)return null
  val offset=if(bestLag>minLag&&bestLag<maxLag){val l=correlations[bestLag-1];val c=correlations[bestLag];val r=correlations[bestLag+1];val curve=l-2.0*c+r;if(l.isFinite()&&r.isFinite()&&abs(curve)>1e-9)(0.5*(l-r)/curve).coerceIn(-0.5,0.5)else 0.0}else 0.0
  val bpm=(60.0*sampleRateHz/(bestLag+offset)).toInt()
  if(bpm !in minBpm..maxBpm)return null
  return PulseEstimate(bpm,bestCorrelation.coerceIn(0.0,1.0),if(bestCorrelation>=0.55)SignalQuality.USABLE else SignalQuality.TOO_NOISY)
 }
}