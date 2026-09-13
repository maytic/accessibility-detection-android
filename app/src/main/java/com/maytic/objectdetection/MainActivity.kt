package com.maytic.objectdetection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.media.ImageReader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.maytic.objectdetection.ui.theme.AccessibilityObjectDetectionTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
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
}

@Composable
fun MainScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current

    val permissionRequester = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val hasCameraPermission = remember {
        mutableIntStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) )
    }

//    val lifecycleOwner = LocalLifecycleOwner.current
//    val cameraController = remember { LifecycleCameraController(context) }
//
//    val imageAnalysis = remember {
//        ImageAnalysis.Builder()
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .build()
//    }
//
//    val executor = remember { Executors.newSingleThreadExecutor() }

    Column {
        Row(modifier = Modifier.padding(16.dp)) {
            Button(onClick = {
                if ( ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                    permissionRequester.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text(text = "Camera Permission: ${hasCameraPermission.intValue}")
            }
        }

    }
}