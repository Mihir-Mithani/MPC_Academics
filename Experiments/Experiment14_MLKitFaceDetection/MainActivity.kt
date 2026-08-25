package com.example.mlkitfacedetection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var faceDetector: FaceDetector? = null
    private var isCameraActive = false
    private var currentFaces = mutableListOf<Face>()
    private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ML Kit Face Detector
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        faceDetector = FaceDetection.getClient(options)

        setContent {
            MLKitFaceDetectionTheme {
                FaceDetectionScreen(
                    onCameraClick = { toggleCamera() },
                    onPermissionClick = { requestPermissions() },
                    onSwitchCameraClick = { switchCamera() }
                )
            }
        }
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (isCameraActive) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
        faceDetector?.close()
        cameraExecutor.shutdown()
    }

    private fun checkAndRequestPermissions() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        } else {
            startCamera()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                isCameraActive = true
                FaceDetectionScreen.updateCameraState(true)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        isCameraActive = false
        FaceDetectionScreen.updateCameraState(false)
    }

    private fun toggleCamera() {
        if (isCameraActive) {
            stopCamera()
        } else {
            startCamera()
        }
    }

    private fun switchCamera() {
        // Camera switching logic would go here
        Toast.makeText(this, "Camera switch not implemented in demo", Toast.LENGTH_SHORT).show()
    }

    private fun bindCameraUseCases() {
        cameraProvider?.let { provider ->
            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(FaceDetectionScreen.previewView?.surfaceProvider)
            }

            // Image Analysis for face detection
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, { imageProxy ->
                        analyzeImage(imageProxy)
                    })
                }

            // Camera selector (back camera)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera binding failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            faceDetector?.process(image)
                ?.addOnSuccessListener { faces ->
                    currentFaces = faces
                    FaceDetectionScreen.updateFaces(faces)
                }
                ?.addOnFailureListener { e ->
                    // Handle error silently
                }
                ?.addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        // State shared with Compose
        private var faces = mutableListOf<Face>()
        private var cameraActive = false
        private var previewView: PreviewView? = null

        fun updateFaces(newFaces: List<Face>) {
            faces.clear()
            faces.addAll(newFaces)
        }

        fun getFaces(): List<Face> = faces.toList()

        fun updateCameraState(active: Boolean) {
            cameraActive = active
        }

        fun isCameraActive(): Boolean = cameraActive

        fun setPreviewView(view: PreviewView) {
            previewView = view
        }

        fun getPreviewView(): PreviewView? = previewView
    }
}

@Composable
fun FaceDetectionScreen(
    onCameraClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onSwitchCameraClick: () -> Unit
) {
    var faces by remember { mutableStateOf(MainActivity.getFaces()) }
    var cameraActive by remember { mutableStateOf(MainActivity.isCameraActive()) }
    var faceCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            faces = MainActivity.getFaces()
            faceCount = faces.size
            cameraActive = MainActivity.isCameraActive()
            androidx.compose.runtime.delay(100)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Card
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ML Kit Face Detection",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    androidx.compose.material3.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Face,
                            contentDescription = "Face detection",
                            tint = if (cameraActive) Color.Green else Color.Red
                        )
                        Text(
                            text = "Faces: $faceCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (cameraActive) Color.Green else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Camera Preview with Overlay
        Card(
            modifier = Modifier.padding(16.dp).fillMaxSize().weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Camera Preview (AndroidView with PreviewView)
                androidx.compose.ui.platform.AndroidView(
                    factory = { context ->
                        androidx.camera.view.PreviewView(context).apply {
                            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            MainActivity.setPreviewView(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        // PreviewView updates handled by CameraX
                    }
                )

                // Face Overlay Canvas
                if (faces.isNotEmpty()) {
                    FaceOverlayCanvas(faces = faces)
                }
            }
        }

        // Face Details
        if (faces.isNotEmpty()) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detected Faces ($faceCount)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.foundation.lazy.items(faces) { face ->
                            FaceDetailCard(face = face)
                        }
                    }
                }
            }
        } else if (cameraActive) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Text(
                    text = "No faces detected. Point camera at a face.",
                    fontSize = 16.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                )
            }
        }

        // Control Buttons
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            androidx.compose.material3.Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onCameraClick) {
                    Icon(
                        imageVector = if (cameraActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (cameraActive) "Stop Camera" else "Start Camera"
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    Text(text = if (cameraActive) "Stop Camera" else "Start Camera", fontSize = 16.sp)
                }

                Button(onClick = onSwitchCameraClick) {
                    Icon(Icons.Filled.CameraFront, contentDescription = "Switch Camera")
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    Text(text = "Switch Camera", fontSize = 16.sp)
                }

                Button(onClick = onPermissionClick) {
                    Icon(Icons.Filled.CameraRear, contentDescription = "Permissions")
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    Text(text = "Permissions", fontSize = 16.sp)
                }
            }
        }

        // Info Card
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Features Detected:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "• Face bounding boxes\n• Facial landmarks (eyes, nose, mouth, ears, cheeks)\n• Face contours\n• Head pose (Euler angles)\n• Smiling probability\n• Eye open probability\n• Tracking ID",
                    fontSize = 13.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FaceOverlayCanvas(faces: List<Face>) {
    androidx.compose.ui.graphics.Canvas(modifier = Modifier.fillMaxSize()) {
        faces.forEach { face ->
            val boundingBox = face.boundingBox
            val rect = RectF(
                boundingBox.left.toFloat(),
                boundingBox.top.toFloat(),
                boundingBox.right.toFloat(),
                boundingBox.bottom.toFloat()
            )

            // Draw face bounding box
            drawRect(
                color = ComposeColor.Green.copy(alpha = 0.8f),
                style = androidx.compose.ui.graphics.Stroke(width = 3f),
                topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                size = androidx.compose.ui.geometry.Size(rect.width(), rect.height())
            )

            // Draw landmarks
            face.allLandmarks.forEach { landmark ->
                val position = landmark.position
                drawCircle(
                    color = ComposeColor.Yellow,
                    center = androidx.compose.ui.geometry.Offset(position.x.toFloat(), position.y.toFloat()),
                    radius = 6f
                )
            }

            // Draw contours
            face.allContours.forEach { contour ->
                val points = contour.points
                if (points.size > 1) {
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = ComposeColor.Cyan.copy(alpha = 0.6f),
                            start = androidx.compose.ui.geometry.Offset(points[i].x.toFloat(), points[i].y.toFloat()),
                            end = androidx.compose.ui.geometry.Offset(points[i + 1].x.toFloat(), points[i + 1].y.toFloat()),
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // Draw tracking ID and head pose
            drawText(
                text = "ID: ${face.trackingId} | Yaw: ${face.headEulerAngleY?.toInt() ?: 0}° Pitch: ${face.headEulerAngleZ?.toInt() ?: 0}°",
                color = ComposeColor.White,
                fontSize = 12.sp,
                topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top - 20f)
            )
        }
    }
}

@Composable
fun FaceDetailCard(face: Face) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Face ID: ${face.trackingId}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))

            androidx.compose.material3.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(text = "Bounding Box:", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "(${face.boundingBox.left}, ${face.boundingBox.top}) - (${face.boundingBox.right}, ${face.boundingBox.bottom})", fontSize = 12.sp)
                }
                Column {
                    Text(text = "Head Pose:", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Yaw: ${face.headEulerAngleY?.toInt() ?: 0}° | Pitch: ${face.headEulerAngleZ?.toInt() ?: 0}° | Roll: ${face.headEulerAngleX?.toInt() ?: 0}°", fontSize = 12.sp)
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

            androidx.compose.material3.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(text = "Smiling: ${String.format("%.0f%%", (face.smilingProbability ?: 0f) * 100)}", fontSize = 12.sp)
                    Text(text = "Left Eye Open: ${String.format("%.0f%%", (face.leftEyeOpenProbability ?: 0f) * 100)}", fontSize = 12.sp)
                }
                Column {
                    Text(text = "Right Eye Open: ${String.format("%.0f%%", (face.rightEyeOpenProbability ?: 0f) * 100)}", fontSize = 12.sp)
                    Text(text = "Landmarks: ${face.allLandmarks.size} | Contours: ${face.allContours.size}", fontSize = 12.sp)
                }
            }
        }
    }
}