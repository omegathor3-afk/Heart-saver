package com.omegathor3.heartsaver.camera
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class PpgFrameQualityTest {
 private fun stable(red:Double=150.0,green:Double=95.0,blue:Double=80.0,clipped:Double=0.02,coverage:Double=0.9)=FrameOptics(red,green,blue,clipped,coverage)
 @Test fun acceptsStableFingerLikeOptics(){val g=PpgFrameQuality();assertTrue(g.inspect(stable()).usable);assertTrue(g.inspect(stable(151.0,95.5,80.5)).usable)}
 @Test fun rejectsUncoveredPartialAndInvalidCoverage(){val g=PpgFrameQuality();assertFalse(g.inspect(stable(90.0,100.0,95.0)).fingerDetected);assertFalse(g.inspect(stable(coverage=0.30)).usable);assertFalse(g.inspect(stable(coverage=Double.NaN)).usable);assertFalse(g.inspect(stable(coverage=1.2)).usable)}
 @Test fun rejectsDarkBrightAndClippedFrames(){val g=PpgFrameQuality();assertFalse(g.inspect(stable(20.0,15.0,12.0)).usable);assertFalse(g.inspect(stable(250.0,245.0,240.0)).usable);assertFalse(g.inspect(stable(clipped=0.45)).usable);assertFalse(g.inspect(stable(clipped=-0.1)).usable)}
 @Test fun movementRequiresRecoveryFrames(){val g=PpgFrameQuality();assertTrue(g.inspect(stable()).usable);val moved=g.inspect(stable(205.0,130.0,110.0));assertFalse(moved.usable);assertTrue(moved.status.contains("Movement"));assertFalse(g.inspect(stable(205.5,130.2,110.1)).usable);assertFalse(g.inspect(stable(205.2,130.1,110.0)).usable);assertTrue(g.inspect(stable(205.1,130.0,110.0)).usable)}
 @Test fun rejectsNonFiniteOptics(){assertFalse(PpgFrameQuality().inspect(stable(Double.NaN)).usable)}
 @Test fun invalidOpticsResetBaseline(){val g=PpgFrameQuality();g.inspect(stable());assertFalse(g.inspect(stable(20.0,15.0,12.0)).usable);assertTrue(g.inspect(stable(205.0,130.0,110.0)).usable)}
}