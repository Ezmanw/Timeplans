package com.grinkware.timeplans.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinkware.timeplans.data.AttendanceRecord
import com.grinkware.timeplans.data.SubjectStats
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceScreen(viewModel: AppViewModel) {
    val records = viewModel.attendanceRecords.value
    val subjectStats = viewModel.subjectStats.value
    val spacing = LocalSpacing.current

    // Calculations
    val totalDays = records.size
    val inDays = records.count { it.status == "IN" }
    val lateDays = records.count { it.status == "LATE" }
    val offDays = records.count { it.status == "OFF" }
    val sickDays = records.count { it.status == "SICK" }
    val holidayDays = records.count { it.status == "HOLIDAY" }

    // Attendance % calculation
    // Attendance % = (IN + LATE) / (Total - HOLIDAY)
    val divisor = totalDays - holidayDays
    val attendancePercentage = if (divisor > 0) {
        ((inDays + lateDays).toFloat() / divisor * 100)
    } else {
        100f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = 100.dp, top = spacing.small)
    ) {
        // Hero Chart Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendance Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Based on $totalDays total days logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))

                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                            StatMiniCounter(label = "In", count = inDays, color = Color(0xFF10B981))
                            StatMiniCounter(label = "Late", count = lateDays, color = Color(0xFFF59E0B))
                            StatMiniCounter(label = "Absent", count = offDays, color = Color(0xFFEF4444))
                        }
                    }

                    // Canvas Circular Progress Ring
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val strokeColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Canvas(modifier = Modifier.size(90.dp)) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = strokeColor,
                                startAngle = -90f,
                                sweepAngle = (attendancePercentage / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", attendancePercentage),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Attendance",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Monthly Breakdown Chart Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Text(
                        text = "Monthly Logging Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))

                    val monthlyData = remember(records) { getMonthlyBreakdown(records) }
                    MonthlyBarChart(data = monthlyData)
                }
            }
        }

        // Subject-Specific Attendance
        item {
            Text(
                text = "Subject Attendance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        if (subjectStats.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(spacing.medium), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No subject attendance logged yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(subjectStats) { stats ->
                SubjectAttendanceRow(stats = stats, spacing = spacing)
            }
        }

        // Term Breakdowns
        item {
            Text(
                text = "Term Summaries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        item {
            val terms = remember(records) { getTermBreakdown(records) }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                terms.forEach { term ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(term.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Logged days: ${term.totalDays}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", term.percentage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (term.percentage >= 90f) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                                Text("Attendance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMiniCounter(label: String, count: Int, color: Color) {
    Column {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SubjectAttendanceRow(stats: SubjectStats, spacing: com.grinkware.timeplans.ui.theme.Spacing) {
    val total = stats.present + stats.absent + stats.late
    val percentage = if (total > 0) {
        (stats.present + stats.late).toFloat() / total * 100
    } else {
        100f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(Color(stats.color))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stats.subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(stats.color)
                    )
                }

                LinearProgressIndicator(
                    progress = percentage / 100f,
                    color = Color(stats.color),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )

                Text(
                    text = "Present: ${stats.present} • Late: ${stats.late} • Absent: ${stats.absent}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MonthlyBarChart(data: List<MonthData>) {
    val maxCount = data.maxOfOrNull { it.total } ?: 5
    val targetMax = if (maxCount > 0) maxCount else 5

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { month ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(32.dp)
            ) {
                // Stacked Bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .width(12.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        val remaining = targetMax - month.total
                        if (remaining > 0) {
                            Spacer(modifier = Modifier.weight(remaining.toFloat()))
                        }
                        if (month.absentCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(month.absentCount.toFloat())
                                    .background(Color(0xFFEF4444), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                        }
                        if (month.lateCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(month.lateCount.toFloat())
                                    .background(Color(0xFFF59E0B))
                            )
                        }
                        if (month.presentCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(month.presentCount.toFloat())
                                    .background(Color(0xFF10B981), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = month.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Helpers for data aggregations
data class MonthData(
    val name: String,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int
) {
    val total: Int get() = presentCount + absentCount + lateCount
}

fun getMonthlyBreakdown(records: List<AttendanceRecord>): List<MonthData> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    
    // Ordered months
    val order = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val map = mutableMapOf<String, MonthData>()
    
    // Group records
    records.forEach { record ->
        try {
            val date = sdf.parse(record.date)
            if (date != null) {
                val mName = monthFormat.format(date)
                val existing = map[mName] ?: MonthData(mName, 0, 0, 0)
                val newObj = when (record.status) {
                    "IN" -> existing.copy(presentCount = existing.presentCount + 1)
                    "LATE" -> existing.copy(lateCount = existing.lateCount + 1)
                    "OFF" -> existing.copy(absentCount = existing.absentCount + 1)
                    "SICK" -> existing.copy(absentCount = existing.absentCount + 1)
                    else -> existing
                }
                map[mName] = newObj
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // Sort according to calendar flow or return recent 6 months
    val result = mutableListOf<MonthData>()
    order.forEach { m ->
        val item = map[m]
        if (item != null) {
            result.add(item)
        } else {
            // Fill empty active months for layout completeness
            // result.add(MonthData(m, 0, 0, 0))
        }
    }
    
    // Fallback if empty to draw axes
    if (result.isEmpty()) {
        result.add(MonthData("May", 0, 0, 0))
        result.add(MonthData("Jun", 0, 0, 0))
    }
    
    return result
}

data class TermData(
    val name: String,
    val totalDays: Int,
    val percentage: Float
)

fun getTermBreakdown(records: List<AttendanceRecord>): List<TermData> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    var autumnIn = 0
    var autumnTotal = 0
    var springIn = 0
    var springTotal = 0
    var summerIn = 0
    var summerTotal = 0

    records.forEach { record ->
        try {
            val date = sdf.parse(record.date)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val month = cal.get(Calendar.MONTH) // 0 = Jan, ..., 11 = Dec
                
                when (month) {
                    Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER -> {
                        if (record.status != "HOLIDAY") {
                            autumnTotal++
                            if (record.status == "IN" || record.status == "LATE") autumnIn++
                        }
                    }
                    Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL -> {
                        if (record.status != "HOLIDAY") {
                            springTotal++
                            if (record.status == "IN" || record.status == "LATE") springIn++
                        }
                    }
                    Calendar.MAY, Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> {
                        if (record.status != "HOLIDAY") {
                            summerTotal++
                            if (record.status == "IN" || record.status == "LATE") summerIn++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    return listOf(
        TermData("Autumn Term (Sep - Dec)", autumnTotal, if (autumnTotal > 0) (autumnIn.toFloat() / autumnTotal * 100) else 100f),
        TermData("Spring Term (Jan - Apr)", springTotal, if (springTotal > 0) (springIn.toFloat() / springTotal * 100) else 100f),
        TermData("Summer Term (May - Aug)", summerTotal, if (summerTotal > 0) (summerIn.toFloat() / summerTotal * 100) else 100f)
    )
}
