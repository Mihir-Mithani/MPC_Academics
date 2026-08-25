package com.example.calculator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                CalculatorScreen()
            }
        }
    }
}

@Composable
fun CalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC6),
            tertiary = Color(0xFF3700B3)
        ),
        content = content
    )
}

@Composable
fun CalculatorScreen() {
    var displayText by remember { mutableStateOf("0") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var pendingOperator by remember { mutableStateOf<String?>(null) }
    var isEnteringNewNumber by remember { mutableStateOf(true) }

    val context = LocalContext.current

    val buttons = listOf(
        "7", "8", "9", "/",
        "4", "5", "6", "*",
        "1", "2", "3", "-",
        "C", "0", "=", "+"
    )

    fun calculateResult(op1: Double, op2: Double, operator: String): Double? {
        return when (operator) {
            "+" -> op1 + op2
            "-" -> op1 - op2
            "*" -> op1 * op2
            "/" -> {
                if (op2 == 0.0) {
                    null
                } else {
                    op1 / op2
                }
            }
            else -> op2
        }
    }

    fun onButtonClick(button: String) {
        when {
            button in "0".."9" -> {
                if (isEnteringNewNumber || displayText == "0") {
                    displayText = button
                    isEnteringNewNumber = false
                } else {
                    displayText += button
                }
            }
            button == "C" -> {
                displayText = "0"
                operand1 = null
                pendingOperator = null
                isEnteringNewNumber = true
            }
            button == "=" -> {
                val op2 = displayText.toDoubleOrNull() ?: 0.0
                if (operand1 != null && pendingOperator != null) {
                    val result = calculateResult(operand1!!, op2, pendingOperator!!)
                    if (result == null) {
                        Toast.makeText(context, "Division by zero!", Toast.LENGTH_SHORT).show()
                        displayText = "0"
                    } else {
                        displayText = if (result % 1 == 0.0) result.toInt().toString() else result.toString()
                    }
                    operand1 = null
                    pendingOperator = null
                    isEnteringNewNumber = true
                }
            }
            button in listOf("+", "-", "*", "/") -> {
                val currentVal = displayText.toDoubleOrNull() ?: 0.0
                if (operand1 == null) {
                    operand1 = currentVal
                } else if (pendingOperator != null && !isEnteringNewNumber) {
                    val result = calculateResult(operand1!!, currentVal, pendingOperator!!)
                    if (result == null) {
                        Toast.makeText(context, "Division by zero!", Toast.LENGTH_SHORT).show()
                        operand1 = 0.0
                    } else {
                        operand1 = result
                    }
                    displayText = if (operand1!! % 1 == 0.0) operand1!!.toInt().toString() else operand1.toString()
                }
                pendingOperator = button
                isEnteringNewNumber = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = displayText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                textAlign = TextAlign.End,
                maxLines = 1
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(buttons) { button ->
                    Button(
                        onClick = { onButtonClick(button) },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize(),
                        colors = if (button in listOf("+", "-", "*", "/", "=", "C")) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        }
                    ) {
                        Text(text = button, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}
