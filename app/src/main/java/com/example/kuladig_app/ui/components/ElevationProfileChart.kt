package com.example.kuladig_app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kuladig_app.data.model.ElevationProfile
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ElevationProfileChart(
    profile: ElevationProfile,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp,
    showStatistics: Boolean = true
) {
    Column(modifier = modifier) {
        if (showStatistics) {
            ElevationStatistics(profile)
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(16.dp)
        ) {
            ElevationChartCanvas(profile = profile)
        }
    }
}

@Composable
private fun ElevationStatistics(profile: ElevationProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Höhenstatistik",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                StatisticItem(
                    label = "Min",
                    value = "${profile.minElevation.roundToInt()}m",
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = "Max",
                    value = "${profile.maxElevation.roundToInt()}m",
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = "Anstieg",
                    value = "${profile.totalAscent.roundToInt()}m",
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = "Abstieg",
                    value = "${profile.totalDescent.roundToInt()}m",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = androidx.compose.ui.graphics.Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun ElevationChartCanvas(profile: ElevationProfile) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    val points = profile.points
    if (points.isEmpty()) {
        Text("Keine Höhendaten verfügbar")
        return
    }
    
    val padding = 40f
    val labelHeight = 20f
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val chartWidth = size.width - padding * 2
                    val relativeX = (tapOffset.x - padding).coerceIn(0f, chartWidth)
                    val index = ((relativeX / chartWidth) * (points.size - 1)).roundToInt()
                        .coerceIn(0, points.size - 1)
                    selectedIndex = index
                }
            }
    ) {
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding - labelHeight
        
        // Berechne Min/Max für Skalierung
        val minElevation = profile.minElevation
        val maxElevation = profile.maxElevation
        val elevationRange = max(maxElevation - minElevation, 1.0) // Mindestens 1m Range
        
        val maxDistance = profile.totalDistance
        
        // Zeichne Hintergrund-Grid
        drawGrid(
            width = chartWidth,
            height = chartHeight,
            offsetX = padding,
            offsetY = padding,
            minElevation = minElevation,
            maxElevation = maxElevation,
            maxDistance = maxDistance
        )
        
        // Zeichne Höhenprofil-Linie
        val path = Path()
        val firstPoint = points.first()
        val startX = padding
        val startY = padding + chartHeight - ((firstPoint.elevation - minElevation) / elevationRange * chartHeight).toFloat()
        path.moveTo(startX, startY)
        
        points.forEachIndexed { index, point ->
            if (index > 0) {
                val x = padding + (point.distance / maxDistance * chartWidth).toFloat()
                val y = padding + chartHeight - ((point.elevation - minElevation) / elevationRange * chartHeight).toFloat()
                path.lineTo(x, y)
            }
        }
        
        // Zeichne Füllung unter der Linie
        val fillPath = Path(path)
        fillPath.lineTo(padding + chartWidth, padding + chartHeight)
        fillPath.lineTo(padding, padding + chartHeight)
        fillPath.close()
        
        drawPath(
            path = fillPath,
            color = Color(0x33007AFF), // Leicht transparentes Blau
        )
        
        // Zeichne Linie
        drawPath(
            path = path,
            color = Color(0xFF007AFF), // Blau
            style = Stroke(width = 3f)
        )
        
        // Zeichne Min/Max Marker
        val minPoint = points.minByOrNull { it.elevation } ?: return@Canvas
        val maxPoint = points.maxByOrNull { it.elevation } ?: return@Canvas
        
        val minX = padding + (minPoint.distance / maxDistance * chartWidth).toFloat()
        val minY = padding + chartHeight - ((minPoint.elevation - minElevation) / elevationRange * chartHeight).toFloat()
        val maxX = padding + (maxPoint.distance / maxDistance * chartWidth).toFloat()
        val maxY = padding + chartHeight - ((maxPoint.elevation - minElevation) / elevationRange * chartHeight).toFloat()
        
        // Min-Marker
        drawCircle(
            color = Color(0xFF34C759), // Grün
            radius = 6f,
            center = Offset(minX, minY)
        )
        
        // Max-Marker
        drawCircle(
            color = Color(0xFFFF3B30), // Rot
            radius = 6f,
            center = Offset(maxX, maxY)
        )
        
        // Zeichne ausgewählten Punkt
        selectedIndex?.let { index ->
            if (index in points.indices) {
                val point = points[index]
                val x = padding + (point.distance / maxDistance * chartWidth).toFloat()
                val y = padding + chartHeight - ((point.elevation - minElevation) / elevationRange * chartHeight).toFloat()
                
                drawCircle(
                    color = Color(0xFFFF9500), // Orange
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
        }
        
        // Zeichne Achsen-Labels
        drawAxisLabels(
            width = chartWidth,
            height = chartHeight,
            offsetX = padding,
            offsetY = padding,
            minElevation = minElevation,
            maxElevation = maxElevation,
            maxDistance = maxDistance
        )
        
        // Zeichne Tooltip für ausgewählten Punkt
        selectedIndex?.let { index ->
            if (index in points.indices) {
                val point = points[index]
                val x = padding + (point.distance / maxDistance * chartWidth).toFloat()
                val y = padding + chartHeight - ((point.elevation - minElevation) / elevationRange * chartHeight).toFloat()
                
                val tooltipText = "Höhe: ${point.elevation.roundToInt()}m\nDistanz: ${(point.distance / 1000.0).let { if (it < 1) "${(it * 1000).roundToInt()}m" else "${String.format("%.1f", it)}km" }}"
                
                // Tooltip-Hintergrund
                val tooltipWidth = 120f
                val tooltipHeight = 50f
                val tooltipX = (x - tooltipWidth / 2).coerceIn(padding, size.width - padding - tooltipWidth)
                val tooltipY = (y - tooltipHeight - 15f).coerceIn(padding, size.height - padding - tooltipHeight)
                
                drawRect(
                    color = Color(0xE0000000), // Dunkelgrau mit Transparenz
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight)
                )
            }
        }
    }
    
    // Zeige ausgewählten Punkt-Info unter dem Chart
    selectedIndex?.let { index ->
        if (index in points.indices) {
            val point = points[index]
            Text(
                text = "Höhe: ${point.elevation.roundToInt()}m | Distanz: ${(point.distance / 1000.0).let { if (it < 1) "${(it * 1000).roundToInt()}m" else "${String.format("%.1f", it)}km" }}",
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    minElevation: Double,
    maxElevation: Double,
    maxDistance: Double
) {
    // Horizontale Linien (Höhe)
    val elevationSteps = 5
    for (i in 0..elevationSteps) {
        val elevation = minElevation + (maxElevation - minElevation) * i / elevationSteps
        val y = offsetY + height - (height * i / elevationSteps)
        
        drawLine(
            color = Color(0x33000000), // Leicht transparentes Grau
            start = Offset(offsetX, y),
            end = Offset(offsetX + width, y),
            strokeWidth = 1f
        )
    }
    
    // Vertikale Linien (Distanz)
    val distanceSteps = 5
    for (i in 0..distanceSteps) {
        val x = offsetX + (width * i / distanceSteps)
        
        drawLine(
            color = Color(0x33000000),
            start = Offset(x, offsetY),
            end = Offset(x, offsetY + height),
            strokeWidth = 1f
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxisLabels(
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    minElevation: Double,
    maxElevation: Double,
    maxDistance: Double
) {
    // Y-Achse Labels (Höhe) - links
    val elevationSteps = 5
    for (i in 0..elevationSteps) {
        val elevation = minElevation + (maxElevation - minElevation) * i / elevationSteps
        val y = offsetY + height - (height * i / elevationSteps)
        
        // Label wird durch Text() in Compose gezeichnet, hier nur Platzhalter
    }
    
    // X-Achse Label (Distanz) - unten
    // Wird ebenfalls durch Text() in Compose gezeichnet
}
