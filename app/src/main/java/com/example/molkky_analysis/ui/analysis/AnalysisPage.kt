package com.example.molkky_analysis.ui.analysis

import android.graphics.Paint as NativePaint // Changed alias for clarity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
// import androidx.compose.ui.text.style.TextAlign // Not directly used in this file's Composables
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
// import kotlin.math.max // Not used
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    onReturnToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUserDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis") },
                actions = {
                    Box {
                        Button(onClick = { showUserDropdown = true }) {
                            Text(uiState.selectedUserId?.let { id -> uiState.availableUsers.find { it.id == id }?.name } ?: "All Users")
                        }
                        DropdownMenu(
                            expanded = showUserDropdown,
                            onDismissRequest = { showUserDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Users") },
                                onClick = {
                                    viewModel.selectUser(null)
                                    showUserDropdown = false
                                }
                            )
                            uiState.availableUsers.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.name) },
                                    onClick = {
                                        viewModel.selectUser(user.id)
                                        showUserDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SummaryStatsCard(stats = uiState.summaryStats)
                Spacer(Modifier.height(16.dp))

                Text("Success Rate by Distance:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (uiState.plotData.isEmpty()) {
                    Text("No data to plot.")
                } else {
                    DistanceSuccessRatePlot(
                        plotData = uiState.plotData,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.weight(0.1f, fill = false))
            Button(
                onClick = onReturnToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Return to Home")
            }
        }
    }
}

@Composable
fun SummaryStatsCard(stats: SummaryStats) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Summary", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Total Throws: ${stats.totalThrows}")
            stats.overallSuccessRate?.let { Text("Overall Success Rate: %.1f %%".format(it * 100)) }
            stats.averageDistance?.let { Text("Average Throw Distance: %.2f m".format(it)) }
            if (stats.successRateByAngle.isNotEmpty()){
                Text("Success Rate by Angle:")
                stats.successRateByAngle.forEach { (angle, rate) ->
                    Text("  ${angle.uppercase(Locale.getDefault())}: ${rate?.let { "%.1f %%".format(it * 100)} ?: "N/A"}")
                }
            }
        }
    }
}


@Composable
fun DistanceSuccessRatePlot(plotData: List<DistanceSuccessRateData>, modifier: Modifier = Modifier) {
    if (plotData.isEmpty()) {
        Text("No data to plot.", modifier = modifier.padding(16.dp))
        return
    }

    val density = LocalDensity.current
    // Explicitly type the remembered NativePaint objects
    val textPaint: NativePaint = remember {
        NativePaint().apply {
            color = android.graphics.Color.BLACK
            textSize = with(density) { 12.sp.toPx() }
            textAlign = NativePaint.Align.CENTER
        }
    }
    val yAxisTextPaint: NativePaint = remember { // This was line 147 context
        NativePaint().apply {
            color = android.graphics.Color.BLACK
            textSize = with(density) { 12.sp.toPx() }
            textAlign = NativePaint.Align.RIGHT
        }
    }


    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasPadding = with(density) { 50.dp.toPx() }
        val chartWidth = size.width - (canvasPadding * 2)
        val chartHeight = size.height - (canvasPadding * 2)

        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

        val minDistanceActual = plotData.minOfOrNull { it.distance } ?: 0f
        val maxDistanceActual = plotData.maxOfOrNull { it.distance } ?: 1f

        val axisMinDistance = floor(minDistanceActual)
        val axisMaxDistance = ceil(maxDistanceActual)
        val distanceRange = (axisMaxDistance - axisMinDistance).coerceAtLeast(1f)

        val maxSuccessRate = 1.0f

        val originX = canvasPadding
        val originY = size.height - canvasPadding

        drawLine(
            color = Color.Black,
            start = Offset(originX, originY),
            end = Offset(originX + chartWidth, originY),
            strokeWidth = 2.dp.toPx()
        )

        drawLine(
            color = Color.Black,
            start = Offset(originX, originY),
            end = Offset(originX, originY - chartHeight),
            strokeWidth = 2.dp.toPx()
        )

        val xTickCount = floor(distanceRange).toInt() + 1
        for (i in 0..xTickCount) {
            val distanceValue = axisMinDistance + i
            if (distanceValue > axisMaxDistance + 0.1f && i > 0) continue

            val xPos = originX + ((distanceValue - axisMinDistance) / distanceRange) * chartWidth

            if (xPos >= originX && xPos <= originX + chartWidth + 2.dp.toPx()) {
                drawLine(
                    color = Color.Gray,
                    start = Offset(xPos, originY),
                    end = Offset(xPos, originY + 5.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${distanceValue.roundToInt()}m",
                    xPos,
                    originY + textPaint.textSize + 5.dp.toPx(),
                    textPaint
                )
            }
        }
        drawContext.canvas.nativeCanvas.drawText(
            "Distance (m)",
            originX + chartWidth / 2,
            originY + textPaint.textSize * 2 + 10.dp.toPx(),
            textPaint
        )

        for (i in 0..5) {
            val rateValue = i * 0.2f
            val yPos = originY - (rateValue / maxSuccessRate) * chartHeight

            drawLine(
                color = Color.Gray,
                start = Offset(originX - 5.dp.toPx(), yPos),
                end = Offset(originX, yPos),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                "${(rateValue * 100).roundToInt()}%",
                originX - 10.dp.toPx(),
                yPos + (yAxisTextPaint.textSize / 3),
                yAxisTextPaint
            )
        }
        val yAxisLabelX = originX - yAxisTextPaint.textSize - 20.dp.toPx()
        val yAxisLabelY = originY - chartHeight / 2

        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-90f, yAxisLabelX, yAxisLabelY)
        drawContext.canvas.nativeCanvas.drawText(
            "Success Rate (%)",
            yAxisLabelX,
            yAxisLabelY + textPaint.textSize /2,
            textPaint
        )
        drawContext.canvas.nativeCanvas.restore()

        plotData.forEach { data ->
            if (data.distance < axisMinDistance || data.distance > axisMaxDistance) {
                // Points outside defined axis range are not plotted by this logic
            }

            val x = originX + (((data.distance - axisMinDistance) / distanceRange) * chartWidth)
            val y = originY - (data.successRate / maxSuccessRate) * chartHeight

            if (x >= originX && x <= originX + chartWidth && y >= originY - chartHeight && y <= originY) {
                drawCircle(
                    color = Color.Blue,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )

                data.errorMargin?.let { margin ->
                    if (margin > 0) {
                        val errorBarHalfHeight = (margin / maxSuccessRate) * chartHeight
                        val yTop = (y - errorBarHalfHeight).coerceAtMost(originY).coerceAtLeast(originY - chartHeight)
                        val yBottom = (y + errorBarHalfHeight).coerceAtMost(originY).coerceAtLeast(originY - chartHeight)

                        drawLine(
                            color = Color.Blue.copy(alpha = 0.7f),
                            start = Offset(x, yTop),
                            end = Offset(x, yBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.Blue.copy(alpha = 0.7f),
                            start = Offset(x - 4.dp.toPx(), yTop),
                            end = Offset(x + 4.dp.toPx(), yTop),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.Blue.copy(alpha = 0.7f),
                            start = Offset(x - 4.dp.toPx(), yBottom),
                            end = Offset(x + 4.dp.toPx(), yBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}