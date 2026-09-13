package com.maytic.objectdetection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.maytic.objectdetection.ui.theme.AccessibilityObjectDetectionTheme
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

private const val TAG = "MainActivity"
private const val DEMO_VIDEO_ASSET = "demo_clip.mp4"

enum class DetectionMode { LIVE_CAMERA, DEMO_VIDEO }

class MainActivity : ComponentActivity() {


    /**
     * load model file
     */
    fun loadModelBuffer(context: Context, rawResId: Int = R.raw.accessibility_detector): MappedByteBuffer {
        val afd = context.resources.openRawResourceFd(rawResId)

        return afd.use {
            FileInputStream(it.fileDescriptor).use { stream ->
                stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    it.startOffset,
                    it.declaredLength
                )
            }
        }
    }


    fun buildObjectDetector(
        context: Context,
        runningMode: RunningMode = RunningMode.LIVE_STREAM,
        onResult: (ObjectDetectorResult, MPImage) -> Unit = { _, _ -> },
        onError: (RuntimeException) -> Unit = {}
    ): ObjectDetector {
        val modelBuffer = loadModelBuffer(context, R.raw.accessibility_detector)

        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(modelBuffer)
            .build()

        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(runningMode)
            .setScoreThreshold(0.5f)
            .setMaxResults(5)
            .setErrorListener { e -> onError(e) }

        // setResultListener is only valid (and only needed) for LIVE_STREAM;
        // VIDEO mode uses detectForVideo's synchronous return value instead.
        if (runningMode == RunningMode.LIVE_STREAM) {
            optionsBuilder.setResultListener { result, inputImage -> onResult(result, inputImage) }
        }

        return ObjectDetector.createFromOptions(context, optionsBuilder.build())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        enableEdgeToEdge()
        setContent {
            AccessibilityObjectDetectionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                   MainScreen(innerPadding)
                }
            }
        }
    }

    @Composable
    fun MainScreen(paddingValues: PaddingValues) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current


        val hasCameraPermission = remember {
            mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        }

        val permissionRequester = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission.value = granted
        }

        val cameraController = remember { LifecycleCameraController(context) }
        val executor = remember { Executors.newSingleThreadExecutor() }

        val mode = remember { mutableStateOf(DetectionMode.LIVE_CAMERA) }

        var latestCameraResult = remember {
            mutableStateOf<DetectionUISate?>(null)
        }

        val objectDetector = remember {
            buildObjectDetector(
                context = context,
                onResult = { result, inputImage ->
                    latestCameraResult.value = DetectionUISate(result, inputImage.width, inputImage.height)
                },
                onError = { error ->
                    Log.d(TAG, "Detection error", error)
                }
            )
        }

        val videoObjectDetector = remember {
            buildObjectDetector(context = context, runningMode = RunningMode.VIDEO)
        }

        var latestVideoResult = remember { mutableStateOf<DetectionUISate?>(null) }

        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri("asset:///$DEMO_VIDEO_ASSET"))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                objectDetector.close()
                videoObjectDetector.close()
                exoPlayer.release()
            }
        }


        Column(modifier = Modifier.padding(paddingValues)) {
            Row(modifier = Modifier.padding(16.dp)) {
                Button(onClick = {
                    if (!hasCameraPermission.value) {
                        permissionRequester.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text(text = "Camera Permission: ${hasCameraPermission.value}")
                }
            }

            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(onClick = { mode.value = DetectionMode.LIVE_CAMERA }) {
                    Text(text = "Live Camera")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { mode.value = DetectionMode.DEMO_VIDEO }) {
                    Text(text = "Demo Video")
                }
            }

            if (mode.value == DetectionMode.DEMO_VIDEO || hasCameraPermission.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    when (mode.value) {
                        DetectionMode.LIVE_CAMERA -> {
                            AndroidView(
                                factory = { ctx -> PreviewView(ctx).apply { controller = cameraController } },
                                modifier = Modifier.fillMaxSize()
                            )
                            DetectionOverlay(
                                detectionState = latestCameraResult.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        DetectionMode.DEMO_VIDEO -> {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        // Matches PreviewView's FILL_CENTER (center-crop) used for
                                        // the live camera, so DetectionOverlay's scale math applies
                                        // the same way to both modes.
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            DetectionOverlay(
                                detectionState = latestVideoResult.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(hasCameraPermission.value, mode.value) {
            if (mode.value == DetectionMode.LIVE_CAMERA && hasCameraPermission.value) {
                cameraController.setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
                cameraController.setImageAnalysisAnalyzer(executor, DetectorAnalyzer(objectDetector))
                cameraController.bindToLifecycle(lifecycleOwner)
            } else {
                cameraController.unbind()
            }
        }

        LaunchedEffect(mode.value) {
            exoPlayer.playWhenReady = mode.value == DetectionMode.DEMO_VIDEO
            if (mode.value == DetectionMode.DEMO_VIDEO) {
                sampleVideoDetections(
                    context = context,
                    assetFileName = DEMO_VIDEO_ASSET,
                    positionMsProvider = { exoPlayer.currentPosition },
                    objectDetector = videoObjectDetector,
                ) { _, result ->
                    latestVideoResult.value = result
                }
            }
        }
    }
}


