package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
                    text = { Text("পিস রেট খাতা", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    modifier = Modifier.testTag("piece_tab_button")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("বেতন পে-স্লিপ", fontWeight = FontWeight.Bold) },
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
                                    text = "পিস রেট সারসংক্ষেপ • $selectedMonth",
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
                                        Text("মোট উৎপাদিত পিস", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(
                                            text = "${LaborLawCalculator.formatNumber(totalPieces.toDouble(), isBn, 0)} পিস",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("মোট পিস রেট আয়", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
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
                                        text = "এই মাসে কোনো পিস কাজের হিসাব নেই",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "অপারেটর ও শ্রমিক ভাইদের কাজের পিস হিসেব যোগ করুন '+' বাটন চেপে",
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
                                            text = "${item.date} • ${LaborLawCalculator.formatNumber(item.pieceCount.toDouble(), isBn, 0)} পিস × ${LaborLawCalculator.formatCurrency(item.ratePerPiece, isBn)}",
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
                                        text = profile.companyName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "মাসিক বেতন ও ওভারটাইম স্লিপ • $selectedMonth",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Divider()

                                // Employee Meta
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("শ্রমিক নাম: ${profile.userName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("আইডি: ${profile.workerId}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("পদবি: ${profile.designation}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(profile.grade, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }

                                Divider()

                                // Attendance & Shift Summary
                                Text(
                                    text = "হাজিরা ও শিফট বিবরণ:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                val totalDutyDays = summary.regularDutyDays + summary.dayShiftDays + summary.nightShiftDays + summary.holidayDutyDays
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("মোট ডিউটি: ${LaborLawCalculator.formatNumber(totalDutyDays.toDouble(), isBn, 0)} দিন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("নাইট শিফট: ${LaborLawCalculator.formatNumber(summary.nightShiftDays.toDouble(), isBn, 0)} দিন", fontSize = 12.sp, color = NightBlue, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ছুটি/সাপ্তাহিক ছুটি: ${LaborLawCalculator.formatNumber((summary.weeklyOffDays + summary.holidayDutyDays + summary.leaveDays).toDouble(), isBn, 0)} দিন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("অনুপস্থিত: ${LaborLawCalculator.formatNumber(summary.absentDays.toDouble(), isBn, 0)} দিন", fontSize = 12.sp, color = if (summary.absentDays > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                }

                                Divider()

                                // Earnings Breakdown Table
                                Text(
                                    text = "আয়ের বিবরণ (Earnings):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                SlipRow("মূল বেতন (Basic Salary)", LaborLawCalculator.formatCurrency(summary.basicSalary, isBn))
                                SlipRow("বাড়িভাড়া ভাতা (House Rent)", LaborLawCalculator.formatCurrency(profile.houseRent, isBn))
                                SlipRow("চিকিৎসা ভাতা (Medical)", LaborLawCalculator.formatCurrency(profile.medicalAllowance, isBn))
                                SlipRow("যাতায়াত ভাতা (Conveyance)", LaborLawCalculator.formatCurrency(profile.conveyanceAllowance, isBn))
                                SlipRow("খাদ্য ভাতা (Food Allowance)", LaborLawCalculator.formatCurrency(profile.foodAllowance, isBn))
                                val attendanceLabel = if (summary.attendanceBonus > 0) {
                                    if (summary.absentDays > 0) {
                                        "হাজিরা বোনাস (${LaborLawCalculator.formatNumber(summary.absentDays.toDouble(), isBn, 0)} দিন অনুপস্থিতি অনুমোদিত)"
                                    } else {
                                        "হাজিরা বোনাস (উপস্থিতি সন্তোষজনক)"
                                    }
                                } else if (summary.absentDays > 0) {
                                    "হাজিরা বোনাস (${LaborLawCalculator.formatNumber(summary.absentDays.toDouble(), isBn, 0)} দিন অনুপস্থিতির কারণে কর্তন)"
                                } else {
                                    "হাজিরা বোনাস"
                                }
                                SlipRow(attendanceLabel, LaborLawCalculator.formatCurrency(summary.attendanceBonus, isBn))

                                Divider()

                                Text(
                                    text = "অতিরিক্ত আয় (Overtime & Others):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                SlipRow(
                                    "ওভারটাইম আয় (${LaborLawCalculator.formatNumber(summary.totalOtHours, isBn)} ঘণ্টা)",
                                    LaborLawCalculator.formatCurrency(summary.totalOtAmount, isBn),
                                    isBold = true
                                )
                                if (summary.nightBillTotal > 0) {
                                    SlipRow("নাইট বিল (${summary.nightBillCount}টি)", LaborLawCalculator.formatCurrency(summary.nightBillTotal, isBn))
                                }
                                if (summary.tiffinBillTotal > 0) {
                                    SlipRow("টিফিন বিল (${summary.tiffinBillCount}টি)", LaborLawCalculator.formatCurrency(summary.tiffinBillTotal, isBn))
                                }
                                if (summary.absentDeduction > 0) {
                                    SlipRow(
                                        "অনুপস্থিতি কর্তন (${LaborLawCalculator.formatNumber(summary.absentDays.toDouble(), isBn, 0)} দিন)",
                                        "- " + LaborLawCalculator.formatCurrency(summary.absentDeduction, isBn),
                                        isBold = true,
                                        isDeduction = true
                                    )
                                }

                                Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)

                                // Total Net Payable - Compact Single Line
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "মোট প্রদেয় বেতন:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = LaborLawCalculator.formatCurrency(summary.totalEstimatedEarnings, isBn),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
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
                                val slipText = """
                                    📄 *${profile.companyName}*
                                    মাসিক বেতন ও ওভারটাইম স্লিপ ($selectedMonth)
                                    -------------------------------
                                    👤 শ্রমিকের নাম: ${profile.userName}
                                    🆔 আইডি: ${profile.workerId} | পদবি: ${profile.designation} (${profile.grade})
                                    -------------------------------
                                    💵 মূল বেতন: ${LaborLawCalculator.formatCurrency(summary.basicSalary, isBn)}
                                    🏠 বাড়িভাড়া: ${LaborLawCalculator.formatCurrency(profile.houseRent, isBn)}
                                    🩺 চিকিৎসা ও খাদ্য ভাতা: ${LaborLawCalculator.formatCurrency(profile.medicalAllowance + profile.foodAllowance, isBn)}
                                    ⏱️ ওভারটাইম (${LaborLawCalculator.formatNumber(summary.totalOtHours, isBn)} ঘ): ${LaborLawCalculator.formatCurrency(summary.totalOtAmount, isBn)}
                                    🌙 নাইট/টিফিন বিল: ${LaborLawCalculator.formatCurrency(summary.nightBillTotal + summary.tiffinBillTotal, isBn)}
                                    🎯 হাজিরা বোনাস: ${LaborLawCalculator.formatCurrency(summary.attendanceBonus, isBn)}${if (summary.absentDeduction > 0) "\n                                    ❌ অনুপস্থিতি কর্তন (${summary.absentDays} দিন): - " + LaborLawCalculator.formatCurrency(summary.absentDeduction, isBn) else ""}
                                    -------------------------------
                                    ✅ *মোট প্রদেয় বেতন:* ${LaborLawCalculator.formatCurrency(summary.totalEstimatedEarnings, isBn)}
                                    _ওভারটাইম ট্র্যাকার বিডি অ্যাপ দ্বারা প্রস্তুতকৃত_
                                """.trimIndent()

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, slipText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "বেতন স্লিপ শেয়ার করুন")
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
            title = { Text("পিস রেট রেকর্ড মুছবেন?") },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${itemToDelete?.itemName}' (${itemToDelete?.date}) এর রেকর্ডটি মুছে ফেলতে চান?"
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
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("বাতিল")
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
