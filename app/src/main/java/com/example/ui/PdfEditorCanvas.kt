package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnnotationEntity
import com.example.data.AnnotationType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class ActiveEditorTool {
    NONE,
    HIGHLIGHT,
    UNDERLINE,
    STRIKETHROUGH,
    PENCIL,
    TEXT_NOTE,
    FREE_TEXT,
    SIGNATURE,
    STAMP,
    SHAPE_RECT,
    SHAPE_CIRCLE,
    SHAPE_ARROW,
    SHAPE_LINE,
    ERASER
}

@Composable
fun PdfEditorCanvas(
    modifier: Modifier = Modifier,
    activeTool: ActiveEditorTool,
    selectedColor: Color,
    selectedStrokeWidth: Float,
    annotations: List<AnnotationEntity>,
    onAddAnnotation: (AnnotationEntity) -> Unit,
    onDeleteAnnotation: (Long) -> Unit,
    selectedStampText: String = "APPROVED",
    signaturePointsToPlace: String? = null
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    val freehandPoints = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Note preview dialog state
    var expandedNoteText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeTool, selectedColor, selectedStrokeWidth, selectedStampText, signaturePointsToPlace) {
                if (activeTool == ActiveEditorTool.NONE) return@pointerInput

                detectTapGestures(
                    onTap = { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0 || h <= 0) return@detectTapGestures

                        when (activeTool) {
                            ActiveEditorTool.TEXT_NOTE -> {
                                val normX = tapOffset.x / w
                                val normY = tapOffset.y / h
                                val bounds = "${normX - 0.05f},${normY - 0.04f},${normX + 0.35f},${normY + 0.12f}"
                                onAddAnnotation(
                                    AnnotationEntity(
                                        documentId = 0,
                                        pageIndex = 0,
                                        type = AnnotationType.TEXT_NOTE,
                                        colorHex = "#" + Integer.toHexString(selectedColor.hashCode()).padStart(8, '0').takeLast(8),
                                        rectBoundsJson = bounds,
                                        textContent = "Note: Tap to edit or view remarks.",
                                        strokeWidth = selectedStrokeWidth
                                    )
                                )
                            }
                            ActiveEditorTool.FREE_TEXT -> {
                                val normX = tapOffset.x / w
                                val normY = tapOffset.y / h
                                val bounds = "${normX - 0.05f},${normY - 0.03f},${normX + 0.40f},${normY + 0.06f}"
                                onAddAnnotation(
                                    AnnotationEntity(
                                        documentId = 0,
                                        pageIndex = 0,
                                        type = AnnotationType.FREE_TEXT,
                                        colorHex = "#" + Integer.toHexString(selectedColor.hashCode()).padStart(8, '0').takeLast(8),
                                        rectBoundsJson = bounds,
                                        textContent = "Sample Text",
                                        strokeWidth = selectedStrokeWidth
                                    )
                                )
                            }
                            ActiveEditorTool.STAMP -> {
                                val normX = tapOffset.x / w
                                val normY = tapOffset.y / h
                                val bounds = "${(normX - 0.18f).coerceAtLeast(0.05f)},${(normY - 0.05f).coerceAtLeast(0.05f)},${(normX + 0.18f).coerceAtMost(0.95f)},${(normY + 0.05f).coerceAtMost(0.95f)}"
                                val stampColor = when (selectedStampText) {
                                    "APPROVED" -> "#10B981"
                                    "REJECTED" -> "#EF4444"
                                    "CONFIDENTIAL" -> "#DC2626"
                                    "DRAFT" -> "#F59E0B"
                                    "FINAL" -> "#2563EB"
                                    "PAID" -> "#059669"
                                    else -> "#64748B"
                                }
                                onAddAnnotation(
                                    AnnotationEntity(
                                        documentId = 0,
                                        pageIndex = 0,
                                        type = AnnotationType.STAMP,
                                        colorHex = stampColor,
                                        rectBoundsJson = bounds,
                                        textContent = selectedStampText,
                                        strokeWidth = 3f
                                    )
                                )
                            }
                            ActiveEditorTool.SIGNATURE -> {
                                if (!signaturePointsToPlace.isNullOrBlank()) {
                                    onAddAnnotation(
                                        AnnotationEntity(
                                            documentId = 0,
                                            pageIndex = 0,
                                            type = AnnotationType.SIGNATURE,
                                            colorHex = "#" + Integer.toHexString(selectedColor.hashCode()).padStart(8, '0').takeLast(8),
                                            pointsJson = signaturePointsToPlace,
                                            strokeWidth = selectedStrokeWidth
                                        )
                                    )
                                }
                            }
                            ActiveEditorTool.ERASER -> {
                                // Find nearest annotation to tap
                                val tapX = tapOffset.x / w
                                val tapY = tapOffset.y / h
                                for (ann in annotations) {
                                    if (isPointNearAnnotation(tapX, tapY, ann)) {
                                        onDeleteAnnotation(ann.id)
                                        break
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                )
            }
            .pointerInput(activeTool, selectedColor, selectedStrokeWidth) {
                if (activeTool == ActiveEditorTool.NONE ||
                    activeTool == ActiveEditorTool.TEXT_NOTE ||
                    activeTool == ActiveEditorTool.FREE_TEXT ||
                    activeTool == ActiveEditorTool.STAMP ||
                    activeTool == ActiveEditorTool.ERASER
                ) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                        dragCurrent = offset
                        if (activeTool == ActiveEditorTool.PENCIL) {
                            freehandPoints.clear()
                            freehandPoints.add(offset)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragCurrent = change.position
                        if (activeTool == ActiveEditorTool.PENCIL) {
                            freehandPoints.add(change.position)
                        }
                    },
                    onDragEnd = {
                        val start = dragStart
                        val end = dragCurrent
                        val w = canvasSize.width
                        val h = canvasSize.height

                        if (start != null && end != null && w > 0 && h > 0) {
                            val colorHex = "#" + Integer.toHexString(selectedColor.hashCode()).padStart(8, '0').takeLast(8)

                            when (activeTool) {
                                ActiveEditorTool.HIGHLIGHT -> {
                                    val left = min(start.x, end.x) / w
                                    val top = min(start.y, end.y) / h
                                    val right = max(start.x, end.x) / w
                                    val bottom = max(start.y, end.y) / h
                                    onAddAnnotation(
                                        AnnotationEntity(
                                            documentId = 0,
                                            pageIndex = 0,
                                            type = AnnotationType.HIGHLIGHT,
                                            colorHex = colorHex,
                                            rectBoundsJson = "$left,$top,$right,$bottom",
                                            opacity = 0.45f
                                        )
                                    )
                                }
                                ActiveEditorTool.UNDERLINE -> {
                                    val left = min(start.x, end.x) / w
                                    val top = min(start.y, end.y) / h
                                    val right = max(start.x, end.x) / w
                                    val bottom = max(start.y, end.y) / h
                                    onAddAnnotation(
                                        AnnotationEntity(
                                            documentId = 0,
                                            pageIndex = 0,
                                            type = AnnotationType.UNDERLINE,
                                            colorHex = colorHex,
                                            rectBoundsJson = "$left,$top,$right,$bottom",
                                            strokeWidth = selectedStrokeWidth
                                        )
                                    )
                                }
                                ActiveEditorTool.STRIKETHROUGH -> {
                                    val left = min(start.x, end.x) / w
                                    val top = min(start.y, end.y) / h
                                    val right = max(start.x, end.x) / w
                                    val bottom = max(start.y, end.y) / h
                                    onAddAnnotation(
                                        AnnotationEntity(
                                            documentId = 0,
                                            pageIndex = 0,
                                            type = AnnotationType.STRIKETHROUGH,
                                            colorHex = colorHex,
                                            rectBoundsJson = "$left,$top,$right,$bottom",
                                            strokeWidth = selectedStrokeWidth
                                        )
                                    )
                                }
                                ActiveEditorTool.PENCIL -> {
                                    if (freehandPoints.size >= 2) {
                                        val pointsStr = freehandPoints.joinToString(";") {
                                            "${it.x / w},${it.y / h}"
                                        }
                                        onAddAnnotation(
                                            AnnotationEntity(
                                                documentId = 0,
                                                pageIndex = 0,
                                                type = AnnotationType.DRAWING,
                                                colorHex = colorHex,
                                                pointsJson = pointsStr,
                                                strokeWidth = selectedStrokeWidth
                                            )
                                        )
                                    }
                                }
                                ActiveEditorTool.SHAPE_RECT -> {
                                    val left = min(start.x, end.x) / w
                                    val top = min(start.y, end.y) / h
                                    val right = max(start.x, end.x) / w
                                    val bottom = max(start.y, end.y) / h
                                    onAddAnnotation(
                                        AnnotationEntity(
                                            documentId = 0,
                                            pageIndex = 0,
                                            type = AnnotationType.SHAPE_RECT,
                                            colorHex = colorHex,
                                            rectBoundsJson = "$left,$top,$right,$bottom",
                                            strokeWidth = selectedStrokeWidth
                                        )
                                    )
                                }
                                ActiveEditorTool.SHAPE_CIRCLE -> {
                                    val left = min(start.x, end.x) / w
                                    val top = min(start.y, end.y) / h
                                    val right = max(start.x, end.x) / w
                                    val bottom = max(start.y, end.y) / h
                                    onAddAnnotation(
                                        AnnotationEntity(
                                            documentId = 0,
                                            pageIndex = 0,
                                            type = AnnotationType.SHAPE_CIRCLE,
                                            colorHex = colorHex,
                                            rectBoundsJson = "$left,$top,$right,$bottom",
                                            strokeWidth = selectedStrokeWidth
                                        )
                                    )
                                }
                                ActiveEditorTool.SHAPE_ARROW -> {
                                    val left = start.x / w
                                    val top = start.y / h
                                    val right = end.x / w
                                    val bottom = end.y / h
                                    onAddAnnotation(
                                        AnnotationEntity(
                                            documentId = 0,
                                            pageIndex = 0,
                                            type = AnnotationType.SHAPE_ARROW,
                                            colorHex = colorHex,
                                            rectBoundsJson = "$left,$top,$right,$bottom",
                                            strokeWidth = selectedStrokeWidth
                                        )
                                    )
                                }
                                ActiveEditorTool.SHAPE_LINE -> {
                                    val left = start.x / w
                                    val top = start.y / h
                                    val right = end.x / w
                                    val bottom = end.y / h
                                    onAddAnnotation(
                                        AnnotationEntity(
                                            documentId = 0,
                                            pageIndex = 0,
                                            type = AnnotationType.SHAPE_LINE,
                                            colorHex = colorHex,
                                            rectBoundsJson = "$left,$top,$right,$bottom",
                                            strokeWidth = selectedStrokeWidth
                                        )
                                    )
                                }
                                else -> {}
                            }
                        }

                        dragStart = null
                        dragCurrent = null
                        freehandPoints.clear()
                    },
                    onDragCancel = {
                        dragStart = null
                        dragCurrent = null
                        freehandPoints.clear()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("pdf_editor_canvas")) {
            canvasSize = size
            val w = size.width
            val h = size.height

            // 1. Draw all saved annotations
            for (ann in annotations) {
                drawSavedAnnotation(ann, w, h)
            }

            // 2. Draw active live drag gesture preview
            val start = dragStart
            val curr = dragCurrent
            if (start != null && curr != null) {
                when (activeTool) {
                    ActiveEditorTool.HIGHLIGHT -> {
                        val left = min(start.x, curr.x)
                        val top = min(start.y, curr.y)
                        val width = kotlin.math.abs(curr.x - start.x)
                        val height = kotlin.math.abs(curr.y - start.y)
                        drawRect(
                            color = selectedColor.copy(alpha = 0.45f),
                            topLeft = Offset(left, top),
                            size = Size(width, height)
                        )
                    }
                    ActiveEditorTool.UNDERLINE -> {
                        val left = min(start.x, curr.x)
                        val right = max(start.x, curr.x)
                        val y = max(start.y, curr.y)
                        drawLine(
                            color = selectedColor,
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = selectedStrokeWidth
                        )
                    }
                    ActiveEditorTool.STRIKETHROUGH -> {
                        val left = min(start.x, curr.x)
                        val right = max(start.x, curr.x)
                        val y = (start.y + curr.y) / 2f
                        drawLine(
                            color = selectedColor,
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = selectedStrokeWidth
                        )
                    }
                    ActiveEditorTool.PENCIL -> {
                        if (freehandPoints.size >= 2) {
                            val path = Path()
                            path.moveTo(freehandPoints[0].x, freehandPoints[0].y)
                            for (i in 1 until freehandPoints.size) {
                                path.lineTo(freehandPoints[i].x, freehandPoints[i].y)
                            }
                            drawPath(
                                path = path,
                                color = selectedColor,
                                style = Stroke(
                                    width = selectedStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                    ActiveEditorTool.SHAPE_RECT -> {
                        val left = min(start.x, curr.x)
                        val top = min(start.y, curr.y)
                        val width = kotlin.math.abs(curr.x - start.x)
                        val height = kotlin.math.abs(curr.y - start.y)
                        drawRect(
                            color = selectedColor,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = selectedStrokeWidth)
                        )
                    }
                    ActiveEditorTool.SHAPE_CIRCLE -> {
                        val left = min(start.x, curr.x)
                        val top = min(start.y, curr.y)
                        val width = kotlin.math.abs(curr.x - start.x)
                        val height = kotlin.math.abs(curr.y - start.y)
                        drawOval(
                            color = selectedColor,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = selectedStrokeWidth)
                        )
                    }
                    ActiveEditorTool.SHAPE_LINE -> {
                        drawLine(
                            color = selectedColor,
                            start = start,
                            end = curr,
                            strokeWidth = selectedStrokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                    ActiveEditorTool.SHAPE_ARROW -> {
                        drawLine(
                            color = selectedColor,
                            start = start,
                            end = curr,
                            strokeWidth = selectedStrokeWidth,
                            cap = StrokeCap.Round
                        )
                        // Arrow head
                        val angle = atan2((curr.y - start.y).toDouble(), (curr.x - start.x).toDouble())
                        val arrowLen = 24f
                        val x1 = curr.x - arrowLen * cos(angle - Math.PI / 6).toFloat()
                        val y1 = curr.y - arrowLen * sin(angle - Math.PI / 6).toFloat()
                        val x2 = curr.x - arrowLen * cos(angle + Math.PI / 6).toFloat()
                        val y2 = curr.y - arrowLen * sin(angle + Math.PI / 6).toFloat()
                        drawLine(color = selectedColor, start = curr, end = Offset(x1, y1), strokeWidth = selectedStrokeWidth)
                        drawLine(color = selectedColor, start = curr, end = Offset(x2, y2), strokeWidth = selectedStrokeWidth)
                    }
                    else -> {}
                }
            }
        }

        // Overlay Sticky Notes & Text markers
        for (ann in annotations) {
            if (ann.type == AnnotationType.TEXT_NOTE || ann.type == AnnotationType.FREE_TEXT) {
                val bounds = parseRect(ann.rectBoundsJson)
                val leftPx = bounds[0] * canvasSize.width
                val topPx = bounds[1] * canvasSize.height
                val widthPx = (bounds[2] - bounds[0]) * canvasSize.width
                val heightPx = (bounds[3] - bounds[1]) * canvasSize.height

                if (leftPx >= 0 && topPx >= 0 && widthPx > 30 && heightPx > 20) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(leftPx.toInt(), topPx.toInt()) }
                            .size(widthPx.dp.coerceAtLeast(60.dp), heightPx.dp.coerceAtLeast(40.dp))
                            .background(
                                color = if (ann.type == AnnotationType.TEXT_NOTE) Color(0xFFFFF9C4) else Color.White.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(1.dp, parseColor(ann.colorHex), RoundedCornerShape(6.dp))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = ann.textContent,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSavedAnnotation(ann: AnnotationEntity, w: Float, h: Float) {
    val col = parseColor(ann.colorHex)

    when (ann.type) {
        AnnotationType.HIGHLIGHT -> {
            val b = parseRect(ann.rectBoundsJson)
            drawRect(
                color = col.copy(alpha = ann.opacity.coerceIn(0.2f, 0.8f)),
                topLeft = Offset(b[0] * w, b[1] * h),
                size = Size((b[2] - b[0]) * w, (b[3] - b[1]) * h)
            )
        }
        AnnotationType.UNDERLINE -> {
            val b = parseRect(ann.rectBoundsJson)
            drawLine(
                color = col,
                start = Offset(b[0] * w, b[3] * h),
                end = Offset(b[2] * w, b[3] * h),
                strokeWidth = ann.strokeWidth
            )
        }
        AnnotationType.STRIKETHROUGH -> {
            val b = parseRect(ann.rectBoundsJson)
            val midY = (b[1] + b[3]) / 2f * h
            drawLine(
                color = col,
                start = Offset(b[0] * w, midY),
                end = Offset(b[2] * w, midY),
                strokeWidth = ann.strokeWidth
            )
        }
        AnnotationType.DRAWING, AnnotationType.SIGNATURE -> {
            val points = parsePoints(ann.pointsJson)
            if (points.size >= 2) {
                val path = Path()
                path.moveTo(points[0].first * w, points[0].second * h)
                for (i in 1 until points.size) {
                    path.lineTo(points[i].first * w, points[i].second * h)
                }
                drawPath(
                    path = path,
                    color = col,
                    style = Stroke(
                        width = ann.strokeWidth.coerceAtLeast(2f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        AnnotationType.STAMP -> {
            val b = parseRect(ann.rectBoundsJson)
            val rect = androidx.compose.ui.geometry.Rect(b[0] * w, b[1] * h, b[2] * w, b[3] * h)
            drawRoundRect(
                color = col.copy(alpha = 0.15f),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = col,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                style = Stroke(width = ann.strokeWidth.coerceAtLeast(3f))
            )
        }
        AnnotationType.SHAPE_RECT -> {
            val b = parseRect(ann.rectBoundsJson)
            drawRect(
                color = col,
                topLeft = Offset(b[0] * w, b[1] * h),
                size = Size((b[2] - b[0]) * w, (b[3] - b[1]) * h),
                style = Stroke(width = ann.strokeWidth)
            )
        }
        AnnotationType.SHAPE_CIRCLE -> {
            val b = parseRect(ann.rectBoundsJson)
            drawOval(
                color = col,
                topLeft = Offset(b[0] * w, b[1] * h),
                size = Size((b[2] - b[0]) * w, (b[3] - b[1]) * h),
                style = Stroke(width = ann.strokeWidth)
            )
        }
        AnnotationType.SHAPE_LINE -> {
            val b = parseRect(ann.rectBoundsJson)
            drawLine(
                color = col,
                start = Offset(b[0] * w, b[1] * h),
                end = Offset(b[2] * w, b[3] * h),
                strokeWidth = ann.strokeWidth,
                cap = StrokeCap.Round
            )
        }
        AnnotationType.SHAPE_ARROW -> {
            val b = parseRect(ann.rectBoundsJson)
            val start = Offset(b[0] * w, b[1] * h)
            val end = Offset(b[2] * w, b[3] * h)
            drawLine(color = col, start = start, end = end, strokeWidth = ann.strokeWidth, cap = StrokeCap.Round)
            val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
            val arrowLen = 22f
            val x1 = end.x - arrowLen * cos(angle - Math.PI / 6).toFloat()
            val y1 = end.y - arrowLen * sin(angle - Math.PI / 6).toFloat()
            val x2 = end.x - arrowLen * cos(angle + Math.PI / 6).toFloat()
            val y2 = end.y - arrowLen * sin(angle + Math.PI / 6).toFloat()
            drawLine(color = col, start = end, end = Offset(x1, y1), strokeWidth = ann.strokeWidth)
            drawLine(color = col, start = end, end = Offset(x2, y2), strokeWidth = ann.strokeWidth)
        }
        else -> {}
    }
}

private fun isPointNearAnnotation(x: Float, y: Float, ann: AnnotationEntity): Boolean {
    val b = parseRect(ann.rectBoundsJson)
    return x in (b[0] - 0.05f)..(b[2] + 0.05f) && y in (b[1] - 0.05f)..(b[3] + 0.05f)
}

private fun parseRect(json: String): FloatArray {
    return try {
        val parts = json.split(",").map { it.trim().toFloat() }
        if (parts.size >= 4) {
            floatArrayOf(parts[0], parts[1], parts[2], parts[3])
        } else {
            floatArrayOf(0.1f, 0.1f, 0.5f, 0.2f)
        }
    } catch (e: Exception) {
        floatArrayOf(0.1f, 0.1f, 0.5f, 0.2f)
    }
}

private fun parsePoints(json: String): List<Pair<Float, Float>> {
    val list = mutableListOf<Pair<Float, Float>>()
    try {
        val pairs = json.split(";")
        for (p in pairs) {
            val coords = p.split(",")
            if (coords.size >= 2) {
                list.add(Pair(coords[0].toFloat(), coords[1].toFloat()))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Red
    }
}
