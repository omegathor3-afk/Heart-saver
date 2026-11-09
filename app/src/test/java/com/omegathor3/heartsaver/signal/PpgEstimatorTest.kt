package com.omegathor3.heartsaver.signal
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
class PpgEstimatorTest {
 private val estimator=PpgEstimator(sampleRateHz=30.0)
 private fun pulse(bpm:Double,offset:Double=120.0,amplitude:Double=8.0,drift:Double=0.0)=(0 until 600).map{i->offset+amplitude*sin(2.0*PI*bpm/60.0*i/30.0)+drift*i/600.0}
 @Test fun estimatesSyntheticSeventyTwoBpmSignal(){val r=estimator.estimate(pulse(72.0));assertTrue(r!=null);assertTrue(abs(r!!.bpm-72)<=2)}
 @Test fun estimatesPulseWithDeterministicNoiseAndFractionalPeriod(){val random=Random(42);val s=(0 until 600).map{i->110.0+7.0*sin(2.0*PI*1.23*i/30.0)+random.nextDouble(-1.2,1.2)};val r=estimator.estimate(s);assertTrue(r!=null);assertTrue(abs(r!!.bpm-74)<=2)}
 @Test fun calibrationIsRelativelyInvariantToExposureScale(){val a=estimator.estimate(pulse(84.0,70.0,0.7));val b=estimator.estimate(pulse(84.0,180.0,1.8));assertTrue(a!=null);assertTrue(b!=null);assertTrue(abs(a!!.bpm-b!!.bpm)<=1)}
 @Test fun removesSlowLinearIlluminationDrift(){val r=estimator.estimate(pulse(66.0,drift=24.0));assertTrue(r!=null);assertTrue(abs(r!!.bpm-66)<=2)}
 @Test fun rejectsShortFlatAndNonFiniteSignals(){assertNull(estimator.estimate(List(100){100.0}));assertNull(estimator.estimate(List(600){100.0}));val s=MutableList(600){100.0};s[200]=Double.NaN;assertNull(estimator.estimate(s))}
 @Test fun rejectsInvalidConfiguration(){assertNull(PpgEstimator(sampleRateHz=Double.NaN).estimate(pulse(72.0)));assertNull(PpgEstimator(sampleRateHz=500.0).estimate(pulse(72.0)));assertNull(PpgEstimator(minBpm=180,maxBpm=180).estimate(pulse(72.0)))}
}