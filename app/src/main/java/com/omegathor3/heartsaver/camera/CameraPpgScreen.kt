package com.omegathor3.heartsaver.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.IOException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.Executors
import com.omegathor3.heartsaver.history.MeasurementHistory
import com.omegathor3.heartsaver.history.MeasurementHistoryPanel
import com.omegathor3.heartsaver.history.MeasurementRecord
import java.util.concurrent.atomic.AtomicBoolean

private val Night = Color(0xFF090D12)
private val Panel = Color(0xFF151E28)
private val Coral = Color(0xFFFF5268)
private val Muted = Color(0xFF9AA8B7)
private val Mint = Color(0xFF75D7B0)

@Composable
fun CameraPpgScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var measuring by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var frameState by remember { mutableStateOf(PpgFrameState()) }
    var history by remember { mutableStateOf(MeasurementHistory.load(context)) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(MeasurementHistory.toCsv(history))
                } ?: throw IOException("Unable to open the selected file.")
                cameraError = "CSV export saved."
            } catch (error: Exception) {
                cameraError = "CSV export failed: ${error.localizedMessage ?: "unknown error"}"
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        if (!it) cameraError = "Camera permission is required to measure a pulse."
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (frameState.bpm != null) {
                    MeasurementHistory.save(context, MeasurementRecord(System.currentTimeMillis(), frameState.bpm!!, frameState.confidence))
                    history = MeasurementHistory.load(context)
                }
                measuring = false
                frameState = PpgFrameState(status = "Measurement paused")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("HEART-SAVER", color = Coral, fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                Text("Pulse measurement", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                Text("Place your fingertip gently over the rear camera and flash.", color = Muted, fontSize = 13.sp)
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Panel, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (measuring && hasPermission) {
                        CameraPreview(
                            onState = { frameState = it },
                            onError = { cameraError = it }
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("♥", color = Coral, fontSize = 58.sp)
                            Text(if (hasPermission) "Camera is paused" else "Camera permission needed", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("The camera is used only while measuring.", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT PULSE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(frameState.bpm?.toString() ?: "—", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.padding(3.dp))
                            Text("BPM", color = Muted, fontSize = 13.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PERIODIC FIT", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
                        Text(
                            if (frameState.confidence >= 0.55) "Higher" else "Building",
                            color = if (frameState.confidence >= 0.55) Mint else Color(0xFFFFC46B),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("${(frameState.progress * 100).toInt()}%", color = Muted, fontSize = 12.sp)
                    }
                }
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(72.dp)
                        .background(Panel, RoundedCornerShape(14.dp)).padding(10.dp)
                ) {
                    val centerY = size.height / 2f
                    drawLine(Color(0xFF344252), androidx.compose.ui.geometry.Offset(0f, centerY),
                        androidx.compose.ui.geometry.Offset(size.width, centerY), strokeWidth = 1f)
                    val points = frameState.waveform
                    if (points.size > 1) {
                        val path = Path()
                        points.forEachIndexed { index, value ->
                            val x = index.toFloat() / (points.size - 1) * size.width
                            val y = centerY - value * size.height * 0.42f
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = Mint, style = Stroke(width = 3f, cap = StrokeCap.Round))
                    }
                }
                Text(frameState.status, color = Muted, fontSize = 12.sp)
                MeasurementHistoryPanel(history) {
                    exportLauncher.launch("heart-saver-history.csv")
                }
                cameraError?.let { Text(it, color = Color(0xFFFFC46B), fontSize = 12.sp) }
                Button(
                    onClick = {
                        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
                        else {
                            if (measuring && frameState.bpm != null) {
                                MeasurementHistory.save(context, MeasurementRecord(System.currentTimeMillis(), frameState.bpm!!, frameState.confidence))
                                history = MeasurementHistory.load(context)
                            }
                            measuring = !measuring
                            cameraError = null
                            frameState = if (measuring) PpgFrameState(status = "Starting camera…")
                                else PpgFrameState(status = "Measurement stopped")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (measuring) Color(0xFF344252) else Coral),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        when {
                            !hasPermission -> "Allow camera"
                            measuring -> "Stop measurement"
                            else -> "Start measurement"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "Experimental pulse estimate only — not an ECG, SpO₂ reading, or medical diagnosis.",
                    color = Muted, fontSize = 10.sp, lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(onState: (PpgFrameState) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var analysis: ImageAnalysis? = null
        val disposed = AtomicBoolean(false)
        val analyzer = PpgFrameAnalyzer { state ->
            if (!disposed.get()) previewView.post { if (!disposed.get()) onState(state) }
        }
        providerFuture.addListener({
            if (disposed.get()) return@addListener
            try {
                provider = providerFuture.get()
                if (disposed.get()) return@addListener
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val frameAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build()
                    )
                    .build()
                analysis = frameAnalysis
                frameAnalysis.setAnalyzer(executor, analyzer)
                provider?.unbindAll()
                val camera = provider?.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, frameAnalysis
                )
                // ON_STOP or recomposition can dispose this effect while the
                // provider future is completing. Recheck after binding so a late
                // listener cannot leave a camera session alive after disposal.
                if (disposed.get()) {
                    frameAnalysis.clearAnalyzer()
                    provider?.unbindAll()
                    return@addListener
                }
                if (camera == null) {
                    frameAnalysis.clearAnalyzer()
                    provider?.unbindAll()
                    onError("Unable to open the rear camera.")
                } else if (camera.cameraInfo.hasFlashUnit()) {
                    val torchFuture = camera.cameraControl.enableTorch(true)
                    torchFuture.addListener({
                        try {
                            torchFuture.get()
                        } catch (error: Exception) {
                            frameAnalysis.clearAnalyzer()
                            provider?.unbindAll()
                            if (!disposed.get()) {
                                onError("Camera flash could not start: ${error.localizedMessage ?: "unknown error"}")
                            }
                        }
                    }, ContextCompat.getMainExecutor(context))
                } else {
                    frameAnalysis.clearAnalyzer()
                    provider?.unbindAll()
                    onError("No flash unit is available on this camera.")
                }
            } catch (error: Exception) {
                analysis?.clearAnalyzer()
                try { provider?.unbindAll() } catch (_: Exception) { }
                if (!disposed.get()) onError("Camera could not start: ${error.localizedMessage ?: "unknown error"}")
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed.set(true)
            analysis?.clearAnalyzer()
            try { provider?.unbindAll() } catch (_: Exception) { }
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
