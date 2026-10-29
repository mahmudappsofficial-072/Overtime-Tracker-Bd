package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.DeductionRecord
import com.example.data.model.OvertimeRecord
import com.example.data.model.UserProfile
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MonthlySummaryState
import com.example.util.LaborLawCalculator
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    selectedMonth: String,
    summary: MonthlySummaryState,
    profile: UserProfile,
    recentRecords: List<OvertimeRecord>,
    deductions: List<DeductionRecord> = emptyList(),
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleLanguage: () -> Unit,
    onQuickAddOtClick: () -> Unit,
    onAddDeduction: (type: String, date: String, amount: Double, desc: String) -> Unit = { _, _, _, _ -> },
    onDeleteDeduction: (DeductionRecord) -> Unit = {},
    onEditTargetClick: () -> Unit,
    onRecordClick: (OvertimeRecord) -> Unit,
    onViewAllClick: () -> Unit
) {
    val lang = profile.language
    val isBn = lang == "bn"

    var showAdvanceDialog by remember { mutableStateOf(false) }
    var showAponBazarDialog by remember { mutableStateOf(false) }
    var selectedDeductionListType by remember { mutableStateOf<String?>(null) }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: User Greeting & Month Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (profile.userName.isNotBlank()) profile.userName else if (isBn) "ওভারটাইম ট্র্যাকার বিডি" else "Overtime Tracker BD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subInfo = listOfNotNull(
                            profile.companyName.takeIf { it.isNotBlank() },
                            profile.designation.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                        if (subInfo.isNotBlank()) {
                            Text(
                                text = subInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = if (isBn) "প্রোফাইল থেকে তথ্য সেট করুন" else "Set info from profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Language Chip
                    AssistChip(
                        onClick = onToggleLanguage,
                        label = { Text(if (isBn) "বাংলা" else "EN", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("language_toggle_chip")
                    )
                }
            }
        }

        // Month Selector Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.testTag("prev_month_button")
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("next_month_button")
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        // Hero Card: Total Income & Overtime Highlights
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LanguageManager.get("estimated_income", lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (profile.isCustomSalary) {
                                        if (isBn) "কাস্টম নিয়ম" else "Custom Rules"
                                    } else {
                                        if (isBn) "শ্রম আইন ২০০৬" else "Labor Law 2006"
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Big Total Salary
                        Text(
                            text = LaborLawCalculator.formatCurrency(summary.totalEstimatedEarnings, isBn),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))

                        // 3 Key Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // OT Hours
                            Column {
                                Text(
                                    text = LanguageManager.get("total_ot_hours", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${LaborLawCalculator.formatNumber(summary.totalOtHours, isBn)} ${if (isBn) "ঘণ্টা" else "hrs"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            // OT Amount
                            Column {
                                Text(
                                    text = LanguageManager.get("total_ot_amount", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = LaborLawCalculator.formatCurrency(summary.totalOtAmount, isBn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            // Hourly Rate
                            Column {
                                Text(
                                    text = if (isBn) "প্রতি ঘণ্টা রেট" else "Hourly Rate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = LaborLawCalculator.formatCurrency(summary.hourlyOtRate, isBn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Monthly Target Goal Tracker Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = LanguageManager.get("ot_target", lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = onEditTargetClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("edit_target_button")
                        ) {
                            Text(if (isBn) "টার্গেট পরিবর্তন" else "Change Target", fontSize = 12.sp)
                        }
                    }

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { summary.targetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBn) {
                                "অর্জিত: ${LaborLawCalculator.formatNumber(summary.totalOtHours, isBn)} / ${LaborLawCalculator.formatNumber(summary.targetHours, isBn, 0)} ঘণ্টা"
                            } else {
                                "Earned: ${LaborLawCalculator.formatNumber(summary.totalOtHours, isBn)} / ${LaborLawCalculator.formatNumber(summary.targetHours, isBn, 0)} hrs"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isBn) "${(summary.targetProgress * 100).toInt()}% পূরণ" else "${(summary.targetProgress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (summary.targetHours <= 0) {
                        Text(
                            text = if (isBn) "কোনো লক্ষ্যমাত্রা নির্ধারণ করা নেই।" else "No monthly target set.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (summary.remainingTargetHours > 0) {
                        Text(
                            text = if (isBn) {
                                "টার্গেট পূরণ করতে এখনও ${LaborLawCalculator.formatNumber(summary.remainingTargetHours, isBn)} ঘণ্টা ওটি প্রয়োজন।"
                            } else {
                                "${LaborLawCalculator.formatNumber(summary.remainingTargetHours, isBn)} hrs needed to achieve target."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = if (isBn) "অভিনন্দন! আপনার মাসিক ওটি লক্ষ্যমাত্রা সফলভাবে পূরণ হয়েছে!" else "Congratulations! Monthly OT target achieved!",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3 Action Buttons: আজকের ওটি, অগ্রিম কর্তন, আপন বাজার
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: আজকের ওটি
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onQuickAddOtClick() }
                        .testTag("quick_add_ot_button"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AddAlarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBn) "আজকের ওটি" else "Today's OT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isBn) "ওটি যোগ" else "Add OT",
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }

                // Button 2: অগ্রিম কর্তন
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showAdvanceDialog = true }
                        .testTag("add_advance_button"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBn) "অগ্রিম কর্তন" else "Advance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (isBn) "টাকা তুলুন" else "Withdraw",
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                        )
                    }
                }

                // Button 3: আপন বাজার
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showAponBazarDialog = true }
                        .testTag("add_apon_bazar_button"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBn) "আপন বাজার" else "Apon Bazar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isBn) "বাজার খরচ" else "Grocery",
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        // Summary Breakdown Grid (Attendance Bonus, Night Bill, Tiffin Bill, Piece Rate)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Attendance Bonus & Night Bill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attendance Bonus Pill
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val isBonusEarned = summary.attendanceBonus > 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isBn) "হাজিরা বোনাস" else "Attendance Bonus", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (isBonusEarned) {
                                        if (isBn) "অর্জিত" else "Earned"
                                    } else if (summary.absentDays > 0) {
                                        if (isBn) "বাতিল" else "Lost"
                                    } else "0",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBonusEarned) SuccessGreen else if (summary.absentDays > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = LaborLawCalculator.formatCurrency(summary.attendanceBonus, isBn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isBonusEarned) SuccessGreen else if (summary.absentDays > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Night Bill Pill
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isBn) {
                                    "নাইট বিল (${LaborLawCalculator.formatNumber(summary.nightBillCount.toDouble(), isBn, 0)}টি)"
                                } else {
                                    "Night Bill (${LaborLawCalculator.formatNumber(summary.nightBillCount.toDouble(), isBn, 0)})"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = LaborLawCalculator.formatCurrency(summary.nightBillTotal, isBn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NightBlue
                            )
                        }
                    }
                }

                // Row 2: Tiffin Bill & Piece Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tiffin Bill Pill
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isBn) {
                                    "টিফিন বিল (${LaborLawCalculator.formatNumber(summary.tiffinBillCount.toDouble(), isBn, 0)}টি)"
                                } else {
                                    "Tiffin Bill (${LaborLawCalculator.formatNumber(summary.tiffinBillCount.toDouble(), isBn, 0)})"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = LaborLawCalculator.formatCurrency(summary.tiffinBillTotal, isBn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = WarningOrange
                            )
                        }
                    }

                    // Piece Rate Pill
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (isBn) "পিস রেট আয়" else "Piece Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = LaborLawCalculator.formatCurrency(summary.totalPieceAmount, isBn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Row 3: Advance Deduction & Apon Bazar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Advance Deduction Pill
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDeductionListType = "ADVANCE" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val advanceCount = deductions.count { it.type == "ADVANCE" }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "অগ্রিম কর্তন" else "Advance Deduction",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (advanceCount > 0) {
                                    Text(
                                        text = "${LaborLawCalculator.formatNumber(advanceCount.toDouble(), isBn, 0)}টি",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (summary.advanceDeduction > 0) {
                                    "- " + LaborLawCalculator.formatCurrency(summary.advanceDeduction, isBn)
                                } else {
                                    LaborLawCalculator.formatCurrency(0.0, isBn)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (summary.advanceDeduction > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Apon Bazar Pill
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDeductionListType = "APON_BAZAR" },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val aponCount = deductions.count { it.type == "APON_BAZAR" }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "আপন বাজার" else "Apon Bazar",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (aponCount > 0) {
                                    Text(
                                        text = "${LaborLawCalculator.formatNumber(aponCount.toDouble(), isBn, 0)}টি",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (summary.aponBazarDeduction > 0) {
                                    "- " + LaborLawCalculator.formatCurrency(summary.aponBazarDeduction, isBn)
                                } else {
                                    LaborLawCalculator.formatCurrency(0.0, isBn)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (summary.aponBazarDeduction > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Recent Overtime Records Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "সাম্প্রতিক ওটি এন্ট্রি" else "Recent OT Entries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAllClick) {
                    Text(if (isBn) "সব দেখুন" else "View All")
                }
            }
        }

        // Recent Records or Empty State
        if (recentRecords.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBn) "এই মাসে এখনও কোনো ওটি যোগ করা হয়নি" else "No OT records added for this month yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(recentRecords.take(4)) { record ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecordClick(record) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isAbsent = record.shiftType == "ABSENT"
                            val isLeave = record.shiftType == "LEAVE"
                            val isWeeklyOff = record.shiftType == "WEEKLY_OFF"

                            val circleBg = when {
                                isAbsent -> MaterialTheme.colorScheme.errorContainer
                                isLeave || isWeeklyOff -> MaterialTheme.colorScheme.secondaryContainer
                                record.isHoliday -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                            val circleTextColor = when {
                                isAbsent -> MaterialTheme.colorScheme.onErrorContainer
                                isLeave || isWeeklyOff -> MaterialTheme.colorScheme.onSecondaryContainer
                                record.isHoliday -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                            val circleText = when {
                                isAbsent -> if (isBn) "অনুপ" else "Abs"
                                isLeave -> if (isBn) "ছুটি" else "Leave"
                                isWeeklyOff -> if (isBn) "বন্ধ" else "Off"
                                else -> "${LaborLawCalculator.formatNumber(record.otHours, isBn)}${if (isBn) "ঘ" else "h"}"
                            }

                            Surface(
                                shape = CircleShape,
                                color = circleBg,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = circleText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isAbsent || isLeave || isWeeklyOff) 11.sp else 13.sp,
                                        color = circleTextColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = record.date,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val shiftType = ShiftType.fromCode(record.shiftType)
                                    Text(
                                        text = shiftType.getLabel(isBn),
                                        fontSize = 10.sp,
                                        color = shiftType.color,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (record.hasNightBill) {
                                        Text(if (isBn) "• নাইট" else "• Night", fontSize = 10.sp, color = NightBlue, fontWeight = FontWeight.Bold)
                                    }
                                    if (record.hasTiffinBill) {
                                        Text(if (isBn) "• টিফিন" else "• Tiffin", fontSize = 10.sp, color = WarningOrange, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (record.shiftType == "ABSENT") {
                            val deductionText = if (summary.dailyAbsentRate > 0.0) {
                                "- " + LaborLawCalculator.formatCurrency(summary.dailyAbsentRate, isBn)
                            } else {
                                if (isBn) "- কর্তন" else "- Deduction"
                            }
                            Text(
                                text = deductionText,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            val itemRate = if (record.hourlyRateSnapshot > 0.0) record.hourlyRateSnapshot else summary.hourlyOtRate
                            Text(
                                text = LaborLawCalculator.formatCurrency(record.otHours * itemRate, isBn),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Advance Deduction Dialog
    if (showAdvanceDialog) {
        var advDate by remember { mutableStateOf(todayDateStr) }
        var advAmount by remember { mutableStateOf("") }
        var advNote by remember { mutableStateOf("") }
        var advError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAdvanceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBn) "অগ্রিম কর্তন যোগ করুন" else "Add Advance Deduction")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfessionalDatePickerField(
                        selectedDate = advDate,
                        onDateSelected = { advDate = it },
                        isBn = isBn
                    )
                    OutlinedTextField(
                        value = advAmount,
                        onValueChange = {
                            advAmount = it
                            advError = false
                        },
                        label = { Text(if (isBn) "টাকার পরিমাণ (৳)" else "Amount (Tk)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = advError,
                        supportingText = if (advError) {
                            { Text(if (isBn) "সঠিক টাকার পরিমাণ দিন" else "Enter valid amount") }
                        } else null
                    )
                    OutlinedTextField(
                        value = advNote,
                        onValueChange = { advNote = it },
                        label = { Text(if (isBn) "বিবরণ / নোট (ঐচ্ছিক)" else "Note (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = advAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onAddDeduction("ADVANCE", advDate.trim().ifBlank { todayDateStr }, amt, advNote.trim())
                            showAdvanceDialog = false
                        } else {
                            advError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "সংরক্ষণ করুন" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvanceDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Apon Bazar Dialog
    if (showAponBazarDialog) {
        var aponDate by remember { mutableStateOf(todayDateStr) }
        var aponAmount by remember { mutableStateOf("") }
        var aponDesc by remember { mutableStateOf("") }
        var aponError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAponBazarDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBn) "আপন বাজার খরচ যোগ করুন" else "Add Apon Bazar Expense")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfessionalDatePickerField(
                        selectedDate = aponDate,
                        onDateSelected = { aponDate = it },
                        isBn = isBn
                    )
                    OutlinedTextField(
                        value = aponAmount,
                        onValueChange = {
                            aponAmount = it
                            aponError = false
                        },
                        label = { Text(if (isBn) "টাকার পরিমাণ (৳)" else "Amount (Tk)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = aponError,
                        supportingText = if (aponError) {
                            { Text(if (isBn) "সঠিক টাকার পরিমাণ দিন" else "Enter valid amount") }
                        } else null
                    )
                    OutlinedTextField(
                        value = aponDesc,
                        onValueChange = { aponDesc = it },
                        label = { Text(if (isBn) "বিবরণ (কী কী নিয়েছেন)" else "Description (Items bought)") },
                        placeholder = { Text(if (isBn) "যেমন: চাল, ডাল, তেল" else "e.g. Rice, Oil, Spices") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = aponAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onAddDeduction("APON_BAZAR", aponDate.trim().ifBlank { todayDateStr }, amt, aponDesc.trim())
                            showAponBazarDialog = false
                        } else {
                            aponError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(if (isBn) "সংরক্ষণ করুন" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAponBazarDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Deduction Details List Dialog (Viewing where how much was added)
    if (selectedDeductionListType != null) {
        val currentType = selectedDeductionListType!!
        val isAdv = currentType == "ADVANCE"
        val filteredList = deductions.filter { it.type == currentType }
        val totalAmt = filteredList.sumOf { it.amount }

        AlertDialog(
            onDismissRequest = { selectedDeductionListType = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isAdv) Icons.Default.Payments else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = if (isAdv) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isAdv) {
                            if (isBn) "অগ্রিম কর্তন বিবরণ ($selectedMonth)" else "Advance Deductions ($selectedMonth)"
                        } else {
                            if (isBn) "আপন বাজার বিবরণ ($selectedMonth)" else "Apon Bazar Details ($selectedMonth)"
                        }
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBn) "চলতি মাসে কোনো এন্ট্রি নেই।" else "No entries for this month.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(filteredList) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.date,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (item.description.isNotBlank()) {
                                                Text(
                                                    text = item.description,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                text = "- " + LaborLawCalculator.formatCurrency(item.amount, isBn),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        IconButton(onClick = { onDeleteDeduction(item) }) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBn) "সর্বমোট কর্তন:" else "Total Deduction:",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "- " + LaborLawCalculator.formatCurrency(totalAmt, isBn),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDeductionListType = null }) {
                    Text(if (isBn) "ঠিক আছে" else "Close")
                }
            }
        )
    }
}
