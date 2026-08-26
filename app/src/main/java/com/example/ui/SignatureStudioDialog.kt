package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SignatureEntity
import com.example.ui.theme.PdfRed

@Composable
fun SignatureStudioDialog(
    savedSignatures: List<SignatureEntity>,
    onDismiss: () -> Unit,
    onSaveSignature: (title: String, points: String, color: String, strokeWidth: Float) -> Unit,
    onDeleteSignature: (SignatureEntity) -> Unit,
    onSelectSignatureToPlace: (String) -> Unit
) {
    var title by remember { mutableStateOf("My Signature") }
    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var strokeWidth by remember { mutableFloatStateOf(4f) }
    val points = remember { mutableStateListOf<Offset>() }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Draw,
                            contentDescription = null,
                            tint = PdfRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Signature Studio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Saved Signatures Tray
                if (savedSignatures.isNotEmpty()) {
                    Text(
                        text = "Saved Signatures Vault",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedSignatures) { sig ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .clickable {
                                        onSelectSignatureToPlace(sig.pointsJson)
                                        onDismiss()
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sig.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDeleteSignature(sig) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Signature Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signature_name_input")
                )

                Spacer(Modifier.height(10.dp))

                // Color Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(
                        Color(0xFF0F172A), // Jet Black
                        Color(0xFF1E3A8A), // Navy Blue
                        Color(0xFFDC2626), // Crimson
                        Color(0xFF047857)  // Emerald
                    )
                    for (c in colors) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(c, CircleShape)
                                .border(
                                    width = if (selectedColor == c) 3.dp else 1.dp,
                                    color = if (selectedColor == c) PdfRed else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = c }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Drawing Pad Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    points.add(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    points.add(change.position)
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        canvasWidth = size.width
                        canvasHeight = size.height

                        // Baseline guide
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(20f, size.height * 0.75f),
                            end = Offset(size.width - 20f, size.height * 0.75f),
                            strokeWidth = 1.5f
                        )

                        if (points.size >= 2) {
                            val path = Path()
                            path.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                path.lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = path,
                                color = selectedColor,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (points.isEmpty()) {
                        Text(
                            text = "✍️ Sign here with your finger",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    IconButton(
                        onClick = { points.clear() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear Pad",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (points.size >= 2 && canvasWidth > 0 && canvasHeight > 0) {
                                val pointsStr = points.joinToString(";") {
                                    "${(it.x / canvasWidth).coerceIn(0f, 1f)},${(it.y / canvasHeight).coerceIn(0f, 1f)}"
                                }
                                val hex = "#" + Integer.toHexString(selectedColor.hashCode()).padStart(8, '0').takeLast(8)
                                onSaveSignature(title.ifBlank { "Signature" }, pointsStr, hex, strokeWidth)
                                points.clear()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_signature_button")
                    ) {
                        Text("Save to Vault")
                    }

                    Button(
                        onClick = {
                            if (points.size >= 2 && canvasWidth > 0 && canvasHeight > 0) {
                                val pointsStr = points.joinToString(";") {
                                    "${(it.x / canvasWidth).coerceIn(0f, 1f)},${(it.y / canvasHeight).coerceIn(0f, 1f)}"
                                }
                                onSelectSignatureToPlace(pointsStr)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PdfRed),
                        modifier = Modifier.weight(1f).testTag("apply_signature_button")
                    ) {
                        Text("Use Signature")
                    }
                }
            }
        }
    }
}
