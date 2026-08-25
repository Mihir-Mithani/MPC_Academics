package com.example.sensordemo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawLine
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var sensorListener: SensorEventListener? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setContent {
            SensorDemoTheme {
                SensorDemoScreen(
                    hasAccelerometer = accelerometer != null,
                    hasGyroscope = gyroscope != null,
                    onStartClick = { startListening() },
                    onStopClick = { stopListening() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isListening) {
            startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        stopListening()
    }

    private fun startListening() {
        if (sensorManager == null) return

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        SensorDemoScreen.updateAccelerometer(event.values.copyOf())
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        SensorDemoScreen.updateGyroscope(event.values.copyOf())
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        accelerometer?.let { sensorManager?.registerListener(sensorListener!!, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager?.registerListener(sensorListener!!, it, SensorManager.SENSOR_DELAY_GAME) }
        isListening = true
    }

    private fun stopListening() {
        sensorListener?.let { sensorManager?.unregisterListener(it) }
        sensorListener = null
        isListening = false
    }

    companion object {
        // Accelerometer data (x, y, z in m/s²)
        private var accelerometerValues = floatArrayOf(0f, 0f, 0f)
        private var gyroscopeValues = floatArrayOf(0f, 0f, 0f)

        fun updateAccelerometer(values: FloatArray) {
            accelerometerValues = values.copyOf()
        }

        fun getAccelerometer(): FloatArray = accelerometerValues.copyOf()

        fun updateGyroscope(values: FloatArray) {
            gyroscopeValues = values.copyOf()
        }

        fun getGyroscope(): FloatArray = gyroscopeValues.copyOf()
    }
}

@Composable
fun SensorDemoScreen(
    hasAccelerometer: Boolean,
    hasGyroscope: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    var accelerometer by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    var gyroscope by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    var isListening by remember { mutableStateOf(false) }
    var shakeDetected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            accelerometer = MainActivity.getAccelerometer()
            gyroscope = MainActivity.getGyroscope()

            // Simple shake detection (acceleration magnitude > 15 m/s²)
            val magnitude = Math.sqrt(
                (accelerometer[0] * accelerometer[0] +
                 accelerometer[1] * accelerometer[1] +
                 accelerometer[2] * accelerometer[2]).toDouble()
            )
            shakeDetected = magnitude > 15.0

            androidx.compose.runtime.delay(50)
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
                        text = "Sensor Demo: Accelerometer & Gyroscope",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isListening) Icons.Filled.Sensors else Icons.Filled.SensorsOff,
                        contentDescription = "Sensor status",
                        tint = if (isListening) Color.Green else Color.Red
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

                // Sensor availability
                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SensorStatusCard(
                        title = "Accelerometer",
                        icon = Icons.Filled.Speed,
                        available = hasAccelerometer,
                        active = isListening
                    )
                    SensorStatusCard(
                        title = "Gyroscope",
                        icon = Icons.Filled.RotateRight,
                        available = hasGyroscope,
                        active = isListening
                    )
                }
            }
        }

        // Accelerometer Visualization
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Accelerometer (m/s²)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

                // Visual representation - ball moving based on tilt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    val ballX = ((accelerometer[0] / 10f + 1f) * 0.5f).coerceIn(0.1f, 0.9f)
                    val ballY = ((-accelerometer[1] / 10f + 1f) * 0.5f).coerceIn(0.1f, 0.9f)

                    androidx.compose.ui.platform.AndroidView(
                        factory = { context ->
                            android.view.View(context).apply {
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                            .graphicsLayer {
                                // We'll use Canvas instead
                            }
                    )

                    // Custom Canvas for ball visualization
                    androidx.compose.ui.graphics.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = 20f

                        // Draw center crosshair
                        drawLine(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                            start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                            end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                            start = androidx.compose.ui.geometry.Offset(0f, centerY),
                            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                            strokeWidth = 1f
                        )

                        // Draw ball
                        val ballXPos = centerX + (accelerometer[0] * 8f).coerceIn(-centerX + radius, centerX - radius)
                        val ballYPos = centerY - (accelerometer[1] * 8f).coerceIn(-centerY + radius, centerY - radius)

                        drawCircle(
                            color = if (shakeDetected) Color.Red else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            center = androidx.compose.ui.geometry.Offset(ballXPos, ballYPos),
                            radius = radius
                        )

                        // Draw Z-axis indicator (circle size)
                        val zRadius = (10f + accelerometer[2]).coerceIn(5f, 50f)
                        drawCircle(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                            radius = zRadius,
                            style = androidx.compose.ui.graphics.Stroke(width = 2f)
                        )
                    }
                }
            }
        }

        // Gyroscope Visualization
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gyroscope (rad/s)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

                // Three bars for X, Y, Z rotation rates
                androidx.compose.material3.Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AxisBar(label = "X (Roll)", value = gyroscope[0], color = Color.Red)
                    AxisBar(label = "Y (Pitch)", value = gyroscope[1], color = Color.Green)
                    AxisBar(label = "Z (Yaw)", value = gyroscope[2], color = Color.Blue)
                }
            }
        }

        // Raw Data Display
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Raw Sensor Data",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(listOf(
                        "Accel X: ${String.format("%.2f", accelerometer[0])} m/s²",
                        "Accel Y: ${String.format("%.2f", accelerometer[1])} m/s²",
                        "Accel Z: ${String.format("%.2f", accelerometer[2])} m/s²",
                        "Gyro X: ${String.format("%.2f", gyroscope[0])} rad/s",
                        "Gyro Y: ${String.format("%.2f", gyroscope[1])} rad/s",
                        "Gyro Z: ${String.format("%.2f", gyroscope[2])} rad/s"
                    )) { text ->
                        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Normal, modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth())
                    }
                }
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
                Button(onClick = onStartClick, enabled = !isListening) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    Text(text = "Start Sensors", fontSize = 16.sp)
                }
                Button(onClick = onStopClick, enabled = isListening, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer)) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    Text(text = "Stop Sensors", fontSize = 16.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // Shake Detection Alert
        if (shakeDetected) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text(
                    text = "🎉 SHAKE DETECTED! Magnitude: ${String.format("%.1f", Math.sqrt((accelerometer[0]*accelerometer[0] + accelerometer[1]*accelerometer[1] + accelerometer[2]*accelerometer[2]).toDouble()))} m/s²",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SensorStatusCard(
    title: String,
    icon: androidx.compose.material.icons.filled.FilledIcon,
    available: Boolean,
    active: Boolean
) {
    Card(
        modifier = Modifier.weight(1f).padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (available)
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            else
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (available) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                text = if (available) (if (active) "ACTIVE" else "READY") else "NOT AVAILABLE",
                fontSize = 12.sp,
                color = if (available) (if (active) Color.Green else Color.Orange) else Color.Red
            )
        }
    }
}

@Composable
fun AxisBar(label: String, value: Float, color: Color) {
    val maxValue = 5f // rad/s
    val normalized = (value / maxValue).coerceIn(-1f, 1f)
    val barWidth = (normalized * 150f).abs()

    androidx.compose.material3.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
        Box(
            modifier = Modifier
                .height(24.dp)
                .width(300.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            // Center line
            androidx.compose.ui.graphics.Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = 1f
                )
            }
            // Bar
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(barWidth)
                    .background(color.copy(alpha = 0.7f))
                    .align(if (normalized >= 0) Alignment.CenterStart else Alignment.CenterEnd)
            )
        }
        Text(text = String.format("%.2f", value), fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(50.dp))
    }
}