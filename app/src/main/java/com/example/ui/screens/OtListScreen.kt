package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OvertimeRecord
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.util.LaborLawCalculator
import com.example.util.LanguageManager

@Composable
fun OtListScreen(
    selectedMonth: String,
    records: List<OvertimeRecord>,
    hourlyRate: Double,
    dailyAbsentRate: Double = 0.0,
    attendanceBonus: Double = 0.0,
    lang: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (OvertimeRecord) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    val isBn = lang == "bn"
    var recordToDelete by remember { mutableStateOf<Long?>(null) }

    val totalHours = records.sumOf { it.otHours }
    val totalAmount = totalHours * hourlyRate

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_ot_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add OT")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("ot_list_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Switcher Header
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onPreviousMonth) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }
                            Text(
                                text = "ওটি খাতা • $selectedMonth",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = onNextMonth) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Monthly Summary Stats in List
                        val absentCount = records.count { it.shiftType == "ABSENT" }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("মোট দিন", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${LaborLawCalculator.formatNumber(records.size.toDouble(), isBn, 0)} দিন", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("মোট ওটি ঘণ্টা", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${LaborLawCalculator.formatNumber(totalHours, isBn)} ঘণ্টা", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ওটি টাকা", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(LaborLawCalculator.formatCurrency(totalAmount, isBn), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("হাজিরা বোনাস", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val isBonusEarned = attendanceBonus > 0
                                Text(
                                    text = if (isBonusEarned) "অর্জিত ✅" else if (absentCount > 0) "কর্তন ❌" else "০",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBonusEarned) SuccessGreen else if (absentCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (records.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "এই মাসে এখনও কোনো ওটি রেকর্ড নেই",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "নিচের '+' বাটনে ট্যাপ করে নতুন ওটি যোগ করুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(records, key = { it.id }) { record ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditClick(record) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isAbsent = record.shiftType == "ABSENT"
                                    val isLeave = record.shiftType == "LEAVE"
                                    val isWeeklyOff = record.shiftType == "WEEKLY_OFF"

                                    val badgeBg = when {
                                        isAbsent -> MaterialTheme.colorScheme.errorContainer
                                        isLeave || isWeeklyOff -> MaterialTheme.colorScheme.secondaryContainer
                                        record.isHoliday -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                    val badgeTextColor = when {
                                        isAbsent -> MaterialTheme.colorScheme.onErrorContainer
                                        isLeave || isWeeklyOff -> MaterialTheme.colorScheme.onSecondaryContainer
                                        record.isHoliday -> MaterialTheme.colorScheme.onTertiaryContainer
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                    val badgeText = when {
                                        isAbsent -> if (isBn) "অনুপস্থিত" else "Absent"
                                        isLeave -> if (isBn) "ছুটি" else "Leave"
                                        isWeeklyOff -> if (isBn) "সাপ্তাহিক ছুটি" else "Off Day"
                                        else -> "${LaborLawCalculator.formatNumber(record.otHours, isBn)} ঘণ্টা"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBg,
                                        modifier = Modifier.padding(end = 10.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isAbsent || isLeave || isWeeklyOff) 12.sp else 14.sp,
                                            color = badgeTextColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = record.date,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (record.note.isNotBlank()) {
                                            Text(
                                                text = record.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (record.shiftType == "ABSENT") {
                                    val deductionText = if (dailyAbsentRate > 0.0) {
                                        "- " + LaborLawCalculator.formatCurrency(dailyAbsentRate, isBn)
                                    } else {
                                        if (isBn) "- বেতন কর্তন" else "- Deduction"
                                    }
                                    Text(
                                        text = deductionText,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    val itemRate = if (record.hourlyRateSnapshot > 0.0) record.hourlyRateSnapshot else hourlyRate
                                    Text(
                                        text = LaborLawCalculator.formatCurrency(record.otHours * itemRate, isBn),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Badges
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val shiftType = ShiftType.fromCode(record.shiftType)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = shiftType.color.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = shiftType.getLabel(isBn),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = shiftType.color,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (record.hasNightBill) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NightBlue.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "🌙 নাইট বিল",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NightBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (record.hasTiffinBill) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = WarningOrange.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "☕ টিফিন বিল",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = WarningOrange,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (record.isHoliday) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                text = "🎉 ছুটির দিন",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Actions: Edit & Delete
                                Row {
                                    IconButton(
                                        onClick = { onEditClick(record) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { recordToDelete = record.id },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("ওটি রেকর্ড মুছবেন?") },
            text = { Text("আপনি কি নিশ্চিত যে এই তারিখের ওটি রেকর্ডটি মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        recordToDelete?.let { onDeleteClick(it) }
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
