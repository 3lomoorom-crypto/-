package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 220
) {
    // Generate deterministic 21x21 QR pattern based on data hash
    val matrix = remember(data) {
        val seed = data.hashCode().toLong()
        val random = Random(seed)
        val size = 21
        val grid = Array(size) { BooleanArray(size) }

        // Fill data area
        for (r in 0 until size) {
            for (c in 0 until size) {
                grid[r][c] = random.nextBoolean()
            }
        }

        // Apply 3 standard QR finder patterns (7x7) at corners
        fun stampFinderPattern(rowStart: Int, colStart: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    grid[rowStart + r][colStart + c] = isBorder || isInner
                }
            }
        }

        stampFinderPattern(0, 0)
        stampFinderPattern(0, 14)
        stampFinderPattern(14, 0)

        // Clear separation zones around finders
        for (i in 0..7) {
            if (i < size) {
                grid[7][i] = false
                grid[i][7] = false
                grid[7][size - 1 - i] = false
                grid[i][size - 8] = false
                grid[size - 8][i] = false
                grid[size - 1 - i][7] = false
            }
        }

        grid
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = 21
            val cellSize = size.width / count

            for (r in 0 until count) {
                for (c in 0 until count) {
                    if (matrix[r][c]) {
                        // Check if in finder pattern corner for smooth rounded styling
                        val inTopLeft = r < 7 && c < 7
                        val inTopRight = r < 7 && c >= 14
                        val inBottomLeft = r >= 14 && c < 7

                        val color = if (inTopLeft || inTopRight || inBottomLeft) {
                            Color(0xFF0F172A)
                        } else {
                            Color(0xFF1E293B)
                        }

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.94f, cellSize * 0.94f),
                            cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f)
                        )
                    }
                }
            }
        }
    }
}
