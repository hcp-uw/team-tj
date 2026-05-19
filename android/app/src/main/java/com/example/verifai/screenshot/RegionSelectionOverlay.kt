package com.example.verifai.screenshot

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun RegionSelectionOverlay(
    onCancel: () -> Unit,
    onConfirm: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStart = offset
                        dragEnd = offset
                    },
                    onDrag = { change, _ ->
                        dragEnd = change.position
                    },
                )
            },
    ) {
        val screenWidthPx = constraints.maxWidth
        val screenHeightPx = constraints.maxHeight

        val selectionRect = remember(dragStart, dragEnd, screenWidthPx, screenHeightPx) {
            val start = dragStart
            val end = dragEnd
            if (start == null || end == null) {
                null
            } else {
                ScreenCaptureHelper.normalizeSelection(
                    start.x,
                    start.y,
                    end.x,
                    end.y,
                    screenWidthPx,
                    screenHeightPx,
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayColor = Color.Black.copy(alpha = 0.55f)
            val highlightColor = Color.White.copy(alpha = 0.25f)
            val borderColor = Color.White

            if (selectionRect != null) {
                val sel = ComposeRect(
                    selectionRect.left.toFloat(),
                    selectionRect.top.toFloat(),
                    selectionRect.right.toFloat(),
                    selectionRect.bottom.toFloat(),
                )
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(ComposeRect(0f, 0f, size.width, size.height))
                    addRect(sel)
                }
                drawPath(path, overlayColor)
                drawRect(
                    color = highlightColor,
                    topLeft = sel.topLeft,
                    size = sel.size,
                )
                drawRect(
                    color = borderColor,
                    topLeft = sel.topLeft,
                    size = sel.size,
                    style = Stroke(width = 3f),
                )
            } else {
                drawRect(overlayColor)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "Drag to select an area, then tap Capture",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.White)
                }
                Button(
                    onClick = {
                        selectionRect?.let(onConfirm)
                    },
                    enabled = selectionRect != null,
                ) {
                    Text("Capture")
                }
            }
        }
    }
}
