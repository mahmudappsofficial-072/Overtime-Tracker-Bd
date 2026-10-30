package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PieceRateRecord
import com.example.data.model.UserProfile
import com.example.ui.theme.NightBlue
import com.example.ui.viewmodel.MonthlySummaryState
import com.example.util.LaborLawCalculator
import com.example.util.LanguageManager

@Composable
fun PieceRateScreen(
    selectedMonth: String,
    pieceRecords: List<PieceRateRecord>,
    profile: UserProfile,
    summary: MonthlySummaryState,
    lang: String,
    onAddPieceClick: () -> Unit,
    onDeletePieceClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val isBn = lang == "bn"
    var selectedTab by remember { mutableStateOf(0) } // 0: Piece Rate, 1: Pay Slip

    val totalPieces = pieceRecords.sumOf { it.pieceCount }
    val totalPieceAmount = pieceRecords.sumOf { it.totalAmount }
    var itemToDelete by remember { mutableStateOf<PieceRateRecord?>(null) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onAddPieceClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_piece_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Piece")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("piece_rate_screen")
        ) {
            // Segmented Tab Switcher
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(if (isBn) "পিস রেট খাতা" else "Piece Rate", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    modifier = Modifier.testTag("piece_tab_button")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isBn) "বেতন পে-স্লিপ" else "Pay Slip", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    modifier = Modifier.testTag("slip_tab_button")
                )
            }

            if (selectedTab == 0) {
                // Piece Rate Tracker List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isBn) "পিস রেট সারসংক্ষেপ • $selectedMonth" else "Piece Rate Summary • $selectedMonth",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(if (isBn) "মোট উৎপাদিত পিস" else "Total Produced Pieces", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(
                                            text = "${LaborLawCalculator.formatNumber(totalPieces.toDouble(), isBn, 0)} ${if (isBn) "পিস" else "pcs"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(if (isBn) "মোট পিস রেট আয়" else "Total Piece Income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(
                                            text = LaborLawCalculator.formatCurrency(totalPieceAmount, isBn),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (pieceRecords.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isBn) "এই মাসে কোনো পিস কাজের হিসাব নেই" else "No piece work records for this month",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBn) "অপারেটর ও শ্রমিক ভাইদের কাজের পিস হিসেব যোগ করুন '+' বাটন চেপে" else "Tap '+' button below to add piece work records",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(pieceRecords, key = { it.id }) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.itemName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "${item.date} • ${LaborLawCalculator.formatNumber(item.pieceCount.toDouble(), isBn, 0)} ${if (isBn) "পিস" else "pcs"} × ${LaborLawCalculator.formatCurrency(item.ratePerPiece, isBn)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.note.isNotBlank()) {
                                            Text(
                                                text = item.note,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = LaborLawCalculator.formatCurrency(item.totalAmount, isBn),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = { itemToDelete = item }) {
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
                    }
                }
            } else {
                // Digital Pay Slip View
                val autoAllowances = if (!profile.isCustomSalary && summary.basicSalary > 0.0) {
                    LaborLawCalculator.calculateAutoAllowances(summary.basicSalary)
                } else null
                val effHouseRent = if (profile.isCustomSalary) profile.houseRent else (autoAllowances?.houseRent ?: profile.houseRent)
                val effMedical = if (profile.isCustomSalary) profile.medicalAllowance else (autoAllowances?.medicalAllowance ?: profile.medicalAllowance)
                val effConveyance = if (profile.isCustomSalary) profile.conveyanceAllowance else (autoAllowances?.conveyanceAllowance ?: profile.conveyanceAllowance)
                val effFood = if (profile.isCustomSalary) profile.foodAllowance else (autoAllowances?.foodAllowance ?: profile.foodAllowance)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Slip Company Header
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo),
                                        contentDescription = "App Logo",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (profile.companyName.isNotBlank()) profile.companyName else "Garments & Factory Ltd.",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isBn) "মাসিক বেতন ও ওভারটাইম স্লিপ • $selectedMonth" else "Monthly Salary & Overtime Pay Slip • $selectedMonth",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()

                                // Employee Meta
                                val formattedGrade = formatDisplayGrade(profile.grade, isBn)
                                val designationLine = if (formattedGrade.isNotBlank()) {
                                    if (isBn) "পদবি: ${profile.designation} ($formattedGrade)" else "Designation: ${profile.designation} ($formattedGrade)"
                                } else {
                                    if (isBn) "পদবি: ${profile.designation}" else "Designation: ${profile.designation}"
                                }
                                val totalDutyDays = summary.regularDutyDays + summary.dayShiftDays + summary.nightShiftDays
                                val dutySummaryLine = if (isBn) "মোট ডিউটি দিন: $totalDutyDays দিন | নাইট: ${summary.nightShiftDays} দিন" else "Total Duty: $totalDutyDays days | Night: ${summary.nightShiftDays} days"

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isBn) "কর্মী নাম: ${profile.userName}" else "Worker Name: ${profile.userName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(designationLine, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isBn) "কার্ড / আইডি নং: ${profile.workerId}" else "Card / ID No: ${profile.workerId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(dutySummaryLine, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider()

                                // Attendance & Shift Summary
                                Text(
                                    text = if (isBn) "হাজিরা ও শিফট বিবরণ:" else "Attendance & Shift Summary:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    style = MaterialTheme.typography.titleSmall
                                )

                                val weeklyAndHolidays = summary.weeklyOffDays + summary.holidayDutyDays
                                val leaveStatusText = if (summary.isBonusCancelledDueToLeave) {
                                    if (isBn) " (হাজিরা বোনাস বাতিল)" else " (Bonus Cancelled)"
                                } else ""
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isBn) "সাপ্তাহিক ও সরকারি ছুটি: $weeklyAndHolidays দিন" else "Weekly Off & Holiday: $weeklyAndHolidays days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (isBn) "সাময়িক ছুটি: ${summary.leaveDays} দিন$leaveStatusText" else "Leave: ${summary.leaveDays} days$leaveStatusText",
                                        fontSize = 12.sp,
                                        color = if (summary.isBonusCancelledDueToLeave) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (summary.isBonusCancelledDueToLeave) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                                val bonusStatusText = if (summary.attendanceBonus > 0) {
                                    if (isBn) "(হাজিরা বোনাস অর্জিত)" else "(Attendance Bonus Earned)"
                                } else if (summary.isBonusCancelledDueToAbsent || summary.absentDays > 0) {
                                    if (isBn) "(হাজিরা বোনাস কর্তন)" else "(Attendance Bonus Deducted)"
                                } else {
                                    ""
                                }
                                Text(
                                    text = if (isBn) "অনুপস্থিত: ${summary.absentDays} দিন $bonusStatusText".trim() else "Absent: ${summary.absentDays} days $bonusStatusText".trim(),
                                    fontSize = 12.sp,
                                    color = if (summary.absentDays > 0 || summary.isBonusCancelledDueToAbsent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )

                                HorizontalDivider()

                                // Section: Basic Salary & Fixed Allowances
                                Text(
                                    text = if (isBn) "মূল বেতন ও নির্ধারিত ভাতাসমূহ:" else "Basic Salary & Fixed Allowances:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    style = MaterialTheme.typography.titleSmall
                                )

                                SlipRow(if (isBn) "মূল বেতন" else "Basic Salary", LaborLawCalculator.formatCurrency(summary.basicSalary, isBn))
                                SlipRow(if (isBn) "বাড়িভাড়া ভাতা (৫০%)" else "House Rent Allowance (50%)", LaborLawCalculator.formatCurrency(effHouseRent, isBn))
                                SlipRow(if (isBn) "চিকিৎসা ভাতা" else "Medical Allowance", LaborLawCalculator.formatCurrency(effMedical, isBn))
                                SlipRow(if (isBn) "যাতায়াত ভাতা" else "Conveyance Allowance", LaborLawCalculator.formatCurrency(effConveyance, isBn))
                                SlipRow(if (isBn) "খাদ্য ভাতা" else "Food Allowance", LaborLawCalculator.formatCurrency(effFood, isBn))
                                val attendanceLabel = if (summary.attendanceBonus > 0) {
                                    if (summary.absentDays > 0) {
                                        if (isBn) "হাজিরা বোনাস (${summary.absentDays} দিন অনুপস্থিতি অনুমোদিত)" else "Attendance Bonus (${summary.absentDays} absent days allowed)"
                                    } else {
                                        if (isBn) "হাজিরা বোনাস (নিয়মিত উপস্থিতি)" else "Attendance Bonus (Regular Attendance)"
                                    }
                                } else if (summary.isBonusCancelledDueToAbsent && summary.isBonusCancelledDueToLeave) {
                                    if (isBn) "হাজিরা বোনাস (অনুপস্থিতি ও সাময়িক ছুটির কারণে বাতিল)" else "Attendance Bonus (Cancelled for Absent & Leave Limit)"
                                } else if (summary.isBonusCancelledDueToLeave) {
                                    if (isBn) "হাজিরা বোনাস (${summary.leaveDays} দিন সাময়িক ছুটির কারণে বাতিল)" else "Attendance Bonus (Cancelled for ${summary.leaveDays} leave days)"
                                } else if (summary.isBonusCancelledDueToAbsent || summary.absentDays > 0) {
                                    if (isBn) "হাজিরা বোনাস (${summary.absentDays} দিন অনুপস্থিতির জন্য বাতিল)" else "Attendance Bonus (Cancelled for ${summary.absentDays} absent days)"
                                } else {
                                    if (isBn) "হাজিরা বোনাস" else "Attendance Bonus"
                                }
                                SlipRow(attendanceLabel, LaborLawCalculator.formatCurrency(summary.attendanceBonus, isBn))

                                HorizontalDivider()

                                // Section: Overtime & Extra Earnings
                                Text(
                                    text = if (isBn) "অতিরিক্ত আয়:" else "Overtime & Extra Earnings:",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    style = MaterialTheme.typography.titleSmall
                                )

                                val otLabel = if (isBn) {
                                    "ওভারটাইম আয় (${String.format("%.1f", summary.totalOtHours)} ঘণ্টা @ Tk ${String.format("%.2f", summary.hourlyOtRate)}/ঘণ্টা)"
                                } else {
                                    "Overtime Income (${String.format("%.1f", summary.totalOtHours)} hrs @ Tk ${String.format("%.2f", summary.hourlyOtRate)}/hr)"
                                }
                                SlipRow(otLabel, LaborLawCalculator.formatCurrency(summary.totalOtAmount, isBn), isBold = true)

                                if (summary.nightBillTotal > 0) {
                                    val nightLabel = if (isBn) "নাইট বিল (${summary.nightBillCount}টি @ Tk ${String.format("%.0f", profile.nightBillRate)})" else "Night Bill (${summary.nightBillCount} nights @ Tk ${String.format("%.0f", profile.nightBillRate)})"
                                    SlipRow(nightLabel, LaborLawCalculator.formatCurrency(summary.nightBillTotal, isBn))
                                }
                                if (summary.tiffinBillTotal > 0) {
                                    val tiffinLabel = if (isBn) "টিফিন বিল (${summary.tiffinBillCount}টি @ Tk ${String.format("%.0f", profile.tiffinBillRate)})" else "Tiffin Bill (${summary.tiffinBillCount} days @ Tk ${String.format("%.0f", profile.tiffinBillRate)})"
                                    SlipRow(tiffinLabel, LaborLawCalculator.formatCurrency(summary.tiffinBillTotal, isBn))
                                }
                                SlipRow(
                                    if (isBn) "পিস রেট আয়" else "Piece Rate Earnings",
                                    LaborLawCalculator.formatCurrency(summary.totalPieceAmount, isBn),
                                    isBold = summary.totalPieceAmount > 0
                                )

                                val hasDeductions = summary.absentDeduction > 0 || summary.advanceDeduction > 0 || summary.aponBazarDeduction > 0
                                if (hasDeductions) {
                                    HorizontalDivider()

                                    Text(
                                        text = if (isBn) "কর্তন:" else "Deductions:",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    if (summary.absentDeduction > 0) {
                                        val absentLabel = if (isBn) {
                                            "অনুপস্থিতি কর্তন (${summary.absentDays} দিন @ Tk ${String.format("%.2f", summary.dailyAbsentRate)}/দিন)"
                                        } else {
                                            "Absent Deduction (${summary.absentDays} days @ Tk ${String.format("%.2f", summary.dailyAbsentRate)}/day)"
                                        }
                                        SlipRow(
                                            absentLabel,
                                            "- " + LaborLawCalculator.formatCurrency(summary.absentDeduction, isBn),
                                            isBold = true,
                                            isDeduction = true
                                        )
                                    }
                                    if (summary.advanceDeduction > 0) {
                                        SlipRow(
                                            if (isBn) "অগ্রিম কর্তন" else "Advance Deduction",
                                            "- " + LaborLawCalculator.formatCurrency(summary.advanceDeduction, isBn),
                                            isBold = true,
                                            isDeduction = true
                                        )
                                    }
                                    if (summary.aponBazarDeduction > 0) {
                                        SlipRow(
                                            if (isBn) "আপন বাজার কর্তন" else "Apon Bazar Deduction",
                                            "- " + LaborLawCalculator.formatCurrency(summary.aponBazarDeduction, isBn),
                                            isBold = true,
                                            isDeduction = true
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Total Net Payable - Highlighting Green Box like PDF
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(1.5.dp, Color(0xFF059669)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isBn) "মোট প্রদেয় বেতন:" else "Net Payable Salary:",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = LaborLawCalculator.formatCurrency(summary.totalEstimatedEarnings, isBn),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }

                                // Signatures area
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.width(110.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isBn) "শ্রমিকের স্বাক্ষর" else "Worker's Signature",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.width(135.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isBn) "অ্যাকাউন্টস / কারখানা কর্তৃপক্ষ" else "Accounts / Factory Authority",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Legal Footer Note
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isBn) "ওভারটাইম ট্র্যাকার বিডি • বাংলাদেশ শ্রম আইন অনুযায়ী প্রস্তুতকৃত" else "Generated via Overtime Tracker BD • Prepared under Bangladesh Labor Act",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Download PDF Button
                    item {
                        Button(
                            onClick = {
                                com.example.util.PdfSlipGenerator.generateAndDownloadPdf(
                                    context = context,
                                    profile = profile,
                                    summary = summary,
                                    selectedMonth = selectedMonth,
                                    isBn = isBn
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_pdf_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "পিডিএফ ডাউনলোড করুন" else "Download PDF",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Share Slip Button
                    item {
                        OutlinedButton(
                            onClick = {
                                val slipText = if (isBn) {
                                    val gradePart = formatDisplayGrade(profile.grade, true).let { if (it.isNotBlank()) " ($it)" else "" }
                                    """
                                        📄 *${profile.companyName}*
                                        মাসিক বেতন ও ওভারটাইম স্লিপ ($selectedMonth)
                                        -------------------------------
                                        👤 শ্রমিকের নাম: ${profile.userName}
                                        🆔 আইডি: ${profile.workerId} | পদবি: ${profile.designation}$gradePart
                                        -------------------------------
                                        💵 মূল বেতন: ${LaborLawCalculator.formatCurrency(summary.basicSalary, isBn)}
                                        🏠 বাড়িভাড়া: ${LaborLawCalculator.formatCurrency(effHouseRent, isBn)}
                                        🩺 চিকিৎসা ও খাদ্য ভাতা: ${LaborLawCalculator.formatCurrency(effMedical + effFood, isBn)}
                                        ⏱️ ওভারটাইম (${LaborLawCalculator.formatNumber(summary.totalOtHours, isBn)} ঘ): ${LaborLawCalculator.formatCurrency(summary.totalOtAmount, isBn)}
                                        🧵 পিস রেট আয়: ${LaborLawCalculator.formatCurrency(summary.totalPieceAmount, isBn)}
                                        🌙 নাইট/টিফিন বিল: ${LaborLawCalculator.formatCurrency(summary.nightBillTotal + summary.tiffinBillTotal, isBn)}
                                        🎯 হাজিরা বোনাস: ${LaborLawCalculator.formatCurrency(summary.attendanceBonus, isBn)}${if (summary.absentDeduction > 0) "\n                                        ❌ অনুপস্থিতি কর্তন (${summary.absentDays} দিন): - " + LaborLawCalculator.formatCurrency(summary.absentDeduction, isBn) else ""}${if (summary.advanceDeduction > 0) "\n                                        💸 অগ্রিম কর্তন: - " + LaborLawCalculator.formatCurrency(summary.advanceDeduction, isBn) else ""}${if (summary.aponBazarDeduction > 0) "\n                                        🛒 আপন বাজার কর্তন: - " + LaborLawCalculator.formatCurrency(summary.aponBazarDeduction, isBn) else ""}
                                        -------------------------------
                                        ✅ *মোট প্রদেয় বেতন:* ${LaborLawCalculator.formatCurrency(summary.totalEstimatedEarnings, isBn)}
                                        _ওভারটাইম ট্র্যাকার বিডি অ্যাপ দ্বারা প্রস্তুতকৃত_
                                    """.trimIndent()
                                } else {
                                    val gradePart = formatDisplayGrade(profile.grade, false).let { if (it.isNotBlank()) " ($it)" else "" }
                                    """
                                        📄 *${profile.companyName}*
                                        Monthly Salary & Overtime Slip ($selectedMonth)
                                        -------------------------------
                                        👤 Worker: ${profile.userName}
                                        🆔 ID: ${profile.workerId} | Designation: ${profile.designation}$gradePart
                                        -------------------------------
                                        💵 Basic Salary: ${LaborLawCalculator.formatCurrency(summary.basicSalary, isBn)}
                                        🏠 House Rent: ${LaborLawCalculator.formatCurrency(effHouseRent, isBn)}
                                        🩺 Medical & Food: ${LaborLawCalculator.formatCurrency(effMedical + effFood, isBn)}
                                        ⏱️ Overtime (${LaborLawCalculator.formatNumber(summary.totalOtHours, isBn)} hrs): ${LaborLawCalculator.formatCurrency(summary.totalOtAmount, isBn)}
                                        🧵 Piece Rate Earnings: ${LaborLawCalculator.formatCurrency(summary.totalPieceAmount, isBn)}
                                        🌙 Night/Tiffin Bill: ${LaborLawCalculator.formatCurrency(summary.nightBillTotal + summary.tiffinBillTotal, isBn)}
                                        🎯 Attendance Bonus: ${LaborLawCalculator.formatCurrency(summary.attendanceBonus, isBn)}${if (summary.absentDeduction > 0) "\n                                        ❌ Absent Deduction (${summary.absentDays} days): - " + LaborLawCalculator.formatCurrency(summary.absentDeduction, isBn) else ""}${if (summary.advanceDeduction > 0) "\n                                        💸 Advance Deduction: - " + LaborLawCalculator.formatCurrency(summary.advanceDeduction, isBn) else ""}${if (summary.aponBazarDeduction > 0) "\n                                        🛒 Apon Bazar Deduction: - " + LaborLawCalculator.formatCurrency(summary.aponBazarDeduction, isBn) else ""}
                                        -------------------------------
                                        ✅ *Total Payable Salary:* ${LaborLawCalculator.formatCurrency(summary.totalEstimatedEarnings, isBn)}
                                        _Generated by Overtime Tracker BD App_
                                    """.trimIndent()
                                }

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, slipText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, if (isBn) "বেতন স্লিপ শেয়ার করুন" else "Share Pay Slip")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_slip_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageManager.get("share_slip", lang))
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog for Piece Rate Record
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(if (isBn) "পিস রেট রেকর্ড মুছবেন?" else "Delete Piece Rate Record?") },
            text = {
                Text(
                    text = if (isBn) {
                        "আপনি কি নিশ্চিত যে '${itemToDelete?.itemName}' (${itemToDelete?.date}) এর রেকর্ডটি রিসাইকেল বিন (Trash)-এ পাঠাতে চান? সেখানে এটি ১ দিন জমা থাকবে।"
                    } else {
                        "Are you sure you want to move '${itemToDelete?.itemName}' (${itemToDelete?.date}) to Recycle Bin (Trash)? It will be kept there for 1 day."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { onDeletePieceClick(it.id) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun SlipRow(label: String, value: String, isBold: Boolean = false, isDeduction: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isDeduction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isDeduction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun formatDisplayGrade(grade: String, isBn: Boolean): String {
    val clean = grade.trim()
    if (clean.isBlank()) return ""
    val lower = clean.lowercase()
    val hasGradeWord = lower.contains("grade") || clean.contains("গ্রেড")
    return if (hasGradeWord) {
        if (isBn) clean.replace("Grade", "গ্রেড", ignoreCase = true)
        else clean.replace("গ্রেড", "Grade")
    } else {
        if (isBn) "গ্রেড $clean" else "Grade $clean"
    }
}
