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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
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
        onResult: (ObjectDetectorResult, MPImage)  -> Unit,
        onError: (RuntimeException) -> Unit
    ): ObjectDetector {
        val modelBuffer = loadModelBuffer(context, R.raw.accessibility_detector)

        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(modelBuffer)
            .build()

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setScoreThreshold(0.5f)
            .setMaxResults(5)
            .setResultListener { result, inputImage ->
                onResult(result, inputImage)
            }
            .setErrorListener { e -> onError(e) }
            .build()

        return ObjectDetector.createFromOptions(context, options)
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

        DisposableEffect(Unit) {
            onDispose {
                objectDetector.close()
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

            if (hasCameraPermission.value) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx -> PreviewView(ctx).apply { controller = cameraController } },
                        modifier = Modifier.fillMaxSize()
                    )
                    DetectionOverlay(
                        detectionState = latestCameraResult.value,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        LaunchedEffect(hasCameraPermission.value) {
            if (hasCameraPermission.value) {
                cameraController.setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
                cameraController.setImageAnalysisAnalyzer(executor, DetectorAnalyzer(objectDetector))
                cameraController.bindToLifecycle(lifecycleOwner)
            }
        }
    }
}


