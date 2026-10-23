package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.NightBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.util.LaborLawCalculator
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    profile: UserProfile,
    lang: String,
    currentUser: com.google.firebase.auth.FirebaseUser? = null,
    isAuthLoading: Boolean = false,
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSaveProfile: (UserProfile) -> Unit,
    onToggleLanguage: () -> Unit,
    onSyncCloud: ((Boolean, String) -> Unit) -> Unit = {},
    trashItems: List<com.example.data.model.TrashItem> = emptyList(),
    onRestoreTrash: (com.example.data.model.TrashItem) -> Unit = {},
    onDeleteTrashPermanently: (com.example.data.model.TrashItem) -> Unit = {},
    onClearAllTrash: () -> Unit = {},
    onDeleteAllData: () -> Unit = {}
) {
    val context = LocalContext.current
    val isBn = lang == "bn"
    var showTrashDialog by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    // Profile state
    var userName by remember(profile) { mutableStateOf(profile.userName) }
    var companyName by remember(profile) { mutableStateOf(profile.companyName) }
    var designation by remember(profile) { mutableStateOf(profile.designation) }
    var workerId by remember(profile) { mutableStateOf(profile.workerId) }
    var grade by remember(profile) { mutableStateOf(profile.grade) }

    // Salary state
    var isCustomSalary by remember(profile) { mutableStateOf(profile.isCustomSalary) }
    
    // Initial gross salary calculation from saved profile
    val initialGross = remember(profile) {
        if (profile.basicSalary > 0.0) {
            val auto = LaborLawCalculator.calculateAutoAllowances(profile.basicSalary)
            auto.grossSalary.toInt().toString()
        } else {
            "0"
        }
    }
    var grossSalaryText by remember(profile) { mutableStateOf(initialGross) }

    var basicSalaryText by remember(profile) {
        mutableStateOf(if (profile.basicSalary == 0.0) "0" else profile.basicSalary.toInt().toString())
    }
    var houseRentText by remember(profile) {
        mutableStateOf(if (profile.houseRent == 0.0) "0" else profile.houseRent.toInt().toString())
    }
    var medicalText by remember(profile) {
        mutableStateOf(if (profile.medicalAllowance == 0.0 && profile.basicSalary == 0.0) "0" else if (profile.medicalAllowance == 0.0 || profile.medicalAllowance == 1250.0) "750" else profile.medicalAllowance.toInt().toString())
    }
    var conveyanceText by remember(profile) {
        mutableStateOf(if (profile.conveyanceAllowance == 0.0) "0" else profile.conveyanceAllowance.toInt().toString())
    }
    var foodText by remember(profile) {
        mutableStateOf(if (profile.foodAllowance == 0.0) "0" else profile.foodAllowance.toInt().toString())
    }
    var attendanceBonusText by remember(profile) {
        mutableStateOf(if (profile.attendanceBonus == 0.0) "0" else profile.attendanceBonus.toInt().toString())
    }
    var maxAbsentForBonusText by remember(profile) {
        mutableStateOf(if (profile.maxAbsentForBonus <= 0) "0" else profile.maxAbsentForBonus.toString())
    }
    var nightBillRateText by remember(profile) {
        mutableStateOf(if (profile.nightBillRate == 0.0) "0" else profile.nightBillRate.toInt().toString())
    }
    var tiffinBillRateText by remember(profile) {
        mutableStateOf(if (profile.tiffinBillRate == 0.0) "0" else profile.tiffinBillRate.toInt().toString())
    }
    var customOtRateText by remember(profile) {
        mutableStateOf(if (profile.customOtRate == 0.0) "0" else profile.customOtRate.toInt().toString())
    }

    var whatsappNumber by remember(profile) { mutableStateOf(profile.supportWhatsapp) }

    LaunchedEffect(profile) {
        if (profile.basicSalary == 0.0 && profile.userName.isBlank() && profile.companyName.isBlank() && profile.workerId.isBlank()) {
            userName = ""
            companyName = ""
            designation = ""
            workerId = ""
            grade = ""
            grossSalaryText = "0"
            basicSalaryText = "0"
            houseRentText = "0"
            medicalText = "0"
            conveyanceText = "0"
            foodText = "0"
            attendanceBonusText = "0"
            maxAbsentForBonusText = "0"
            nightBillRateText = "0"
            tiffinBillRateText = "0"
            customOtRateText = "0"
            whatsappNumber = ""
            isCustomSalary = false
        }
    }

    // Dialog flags
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    // Auto-calculate allowances from Gross Salary if mode is Automatic
    val currentGross = grossSalaryText.toDoubleOrNull() ?: 0.0
    val autoResult = remember(currentGross) {
        LaborLawCalculator.calculateFromGrossSalary(currentGross)
    }

    val effectiveHourlyOtRate = if (isCustomSalary) {
        val customRate = customOtRateText.toDoubleOrNull() ?: 0.0
        val customBasic = basicSalaryText.toDoubleOrNull() ?: 0.0
        if (customRate > 0.0) customRate else if (customBasic > 0.0) (customBasic / 104.0) else 0.0
    } else {
        autoResult.otHourlyRate
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Bar with Settings Icon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "প্রোফাইল ও বেতন সেটিংস" else "Profile & Salary Setup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    FilledTonalIconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        }

        // Account Card (International Google Authentication System)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("account_card")
            ) {
                if (currentUser == null) {
                    // State: Unauthenticated
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isBn) "গুগল অ্যাকাউন্ট" else "Google Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBn) "লগইন করা নেই" else "Signed out",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Minimalist international Google Sign-In button
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable(enabled = !isAuthLoading) { onSignInWithGoogle() }
                                .testTag("google_sign_in_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isBn) "সাইন-ইন হচ্ছে..." else "Signing in...",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(Color(0xFF4285F4), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isBn) "Sign in with Google" else "Sign in with Google",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // State: Authenticated
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (!currentUser.photoUrl?.toString().isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = currentUser.photoUrl,
                                    contentDescription = "Profile Photo",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (currentUser.displayName?.take(1) ?: "U").uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = currentUser.displayName ?: (if (isBn) "গুগল ইউজার" else "Google User"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentUser.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SuccessGreen.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "সক্রিয় অ্যাকাউন্ট" else "Active Account",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = { showSignOutConfirmDialog = true },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Text(
                                text = if (isBn) "লগআউট" else "Sign out",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Worker Identity Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = userName.ifBlank { if (isBn) "আপনার নাম" else "Your Name" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${companyName.ifBlank { if (isBn) "ফ্যাক্টরি" else "Company" }} • ${designation.ifBlank { if (isBn) "পদবি" else "Designation" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (isBn) "আইডি" else "ID"}: ${workerId.ifBlank { "N/A" }} • ${grade.ifBlank { if (isBn) "গ্রেড" else "Grade" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider()

                    // Editable Personal Info
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text(if (isBn) "শ্রমিকের নাম" else "Worker Name") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text(if (isBn) "কোম্পানি / ফ্যাক্টরি" else "Company / Factory") },
                            modifier = Modifier.weight(1f).testTag("profile_company_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text(if (isBn) "পদবি" else "Designation") },
                            modifier = Modifier.weight(1f).testTag("profile_designation_input"),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = workerId,
                            onValueChange = { workerId = it },
                            label = { Text(if (isBn) "আইডি নম্বর" else "Worker ID") },
                            modifier = Modifier.weight(1f).testTag("profile_id_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text(if (isBn) "গ্রেড" else "Grade") },
                            modifier = Modifier.weight(1f).testTag("profile_grade_input"),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Salary Setup Mode Switcher Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isBn) "বেতন কাঠামো ও হিসাব পদ্ধতি" else "Salary Structure & Calculation",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Mode Toggle: Automatic vs Custom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isCustomSalary) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isCustomSalary = false
                                    // sync auto values
                                    basicSalaryText = autoResult.basicSalary.toInt().toString()
                                    houseRentText = autoResult.houseRent.toInt().toString()
                                    medicalText = "750"
                                    conveyanceText = autoResult.conveyanceAllowance.toInt().toString()
                                    foodText = autoResult.foodAllowance.toInt().toString()
                                    customOtRateText = "0"
                                }
                                .testTag("mode_auto_tab")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "স্বয়ংক্রিয় (আইন অনুযায়ী)" else "Automatic (Labor Law)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isCustomSalary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCustomSalary) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isCustomSalary = true
                                    // ensure basic and allowances carry over
                                    if (basicSalaryText.isBlank() || basicSalaryText == "0") {
                                        basicSalaryText = autoResult.basicSalary.toInt().toString()
                                    }
                                }
                                .testTag("mode_custom_tab")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "কাস্টম (নিজস্ব নিয়ম)" else "Custom Rules",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCustomSalary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Explanatory note
                    if (!isCustomSalary) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (isBn) {
                                    "⚖️ সরকারি নিয়ম অনুযায়ী মোট বেতন দিলে মূল বেতন, বাড়িভাড়া ও ভাতাসমূহ স্বয়ংক্রিয়ভাবে হিসাব হবে। ওটি হার = মূল বেতন ÷ ১০৪ (দ্বিগুণ হার)।"
                                } else {
                                    "⚖️ Under labor law, basic salary, house rent & allowances are automatically calculated from gross salary. OT Rate = Basic ÷ 104 (2x rate)."
                                },
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (isBn) {
                                    "⚙️ কাস্টম মোড: আপনার ফ্যাক্টরির নিয়ম অনুযায়ী মূল বেতন, যেকোনো ভাতা বা প্রতি ঘণ্টার ওটি রেট নিজের মতো বসিয়ে নিতে পারবেন।"
                                } else {
                                    "⚙️ Custom mode: Configure basic salary, any allowances, or hourly OT rate according to your company rules."
                                },
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (!isCustomSalary) {
                        // In Automatic mode: User enters Gross Salary
                        OutlinedTextField(
                            value = grossSalaryText,
                            onValueChange = { grossSalaryText = it },
                            label = { Text(if (isBn) "মোট বেতন (Gross Salary ৳)" else "Gross Salary (৳)") },
                            modifier = Modifier.fillMaxWidth().testTag("gross_salary_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    } else {
                        // In Custom mode: User can edit Basic Salary directly here or in breakdown
                        OutlinedTextField(
                            value = basicSalaryText,
                            onValueChange = { basicSalaryText = it },
                            label = { Text(if (isBn) "মূল বেতন (Basic Salary ৳)" else "Basic Salary (৳)") },
                            modifier = Modifier.fillMaxWidth().testTag("basic_salary_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) }
                        )
                    }

                    // Allowances Section
                    Text(
                        text = if (!isCustomSalary) {
                            if (isBn) "সরকারি নিয়মে স্বয়ংক্রিয় হিসাব (লকড):" else "Automatic Breakdown (Locked):"
                        } else {
                            if (isBn) "ভাতাসমূহ (Allowances):" else "Allowances:"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    // Basic Salary (Locked in Auto mode, editable in Custom mode)
                    if (!isCustomSalary) {
                        OutlinedTextField(
                            value = autoResult.basicSalary.toInt().toString(),
                            onValueChange = {},
                            label = { Text(if (isBn) "মূল বেতন (Basic Salary - লকড)" else "Basic Salary (Locked)") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth().testTag("locked_basic_salary_input"),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (isCustomSalary) houseRentText else autoResult.houseRent.toInt().toString(),
                            onValueChange = { if (isCustomSalary) houseRentText = it },
                            label = { Text(if (isBn) "বাড়িভাড়া (House Rent)" else "House Rent") },
                            enabled = isCustomSalary,
                            modifier = Modifier.weight(1f).testTag("house_rent_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = if (isCustomSalary) medicalText else "750",
                            onValueChange = { if (isCustomSalary) medicalText = it },
                            label = { Text(if (isBn) "চিকিৎসা ভাতা (Fixed ৳ ৭৫০)" else "Medical Allowance (Fixed ৳ 750)") },
                            enabled = isCustomSalary,
                            modifier = Modifier.weight(1f).testTag("medical_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (isCustomSalary) conveyanceText else autoResult.conveyanceAllowance.toInt().toString(),
                            onValueChange = { if (isCustomSalary) conveyanceText = it },
                            label = { Text(if (isBn) "যাতায়াত ভাতা" else "Conveyance Allowance") },
                            enabled = isCustomSalary,
                            modifier = Modifier.weight(1f).testTag("conveyance_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = if (isCustomSalary) foodText else autoResult.foodAllowance.toInt().toString(),
                            onValueChange = { if (isCustomSalary) foodText = it },
                            label = { Text(if (isBn) "খাদ্য ভাতা" else "Food Allowance") },
                            enabled = isCustomSalary,
                            modifier = Modifier.weight(1f).testTag("food_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    // Night & Tiffin Rates & Attendance Bonus
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nightBillRateText,
                            onValueChange = { nightBillRateText = it },
                            label = { Text(if (isBn) "নাইট বিল (প্রতি রাত ৳)" else "Night Bill (Per Night ৳)") },
                            modifier = Modifier.weight(1f).testTag("night_rate_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tiffinBillRateText,
                            onValueChange = { tiffinBillRateText = it },
                            label = { Text(if (isBn) "টিফিন বিল (প্রতি দিন ৳)" else "Tiffin Bill (Per Day ৳)") },
                            modifier = Modifier.weight(1f).testTag("tiffin_rate_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = attendanceBonusText,
                        onValueChange = { attendanceBonusText = it },
                        label = { Text(if (isBn) "হাজিরা বোনাস (৳)" else "Attendance Bonus (৳)") },
                        modifier = Modifier.fillMaxWidth().testTag("attendance_bonus_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxAbsentForBonusText,
                        onValueChange = { input ->
                            // Allow digits only
                            if (input.all { it.isDigit() }) {
                                maxAbsentForBonusText = input
                            }
                        },
                        label = { Text(if (isBn) "হাজিরা বোনাস কাটার শর্ত (অনুপস্থিতির দিন)" else "Bonus Deduction Threshold (Absent Days)") },
                        modifier = Modifier.fillMaxWidth().testTag("max_absent_bonus_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        supportingText = {
                            Text(
                                text = if (isBn) {
                                    "আপনার অফিসের নিয়মানুযায়ী লিখুন (যেমন: ৩ দিন দিলে ৩ দিন পর্যন্ত বোনাস অক্ষত থাকবে, ৪র্থ দিন থেকে বাতিল হবে)।"
                                } else {
                                    "Enter according to company policy (e.g. 3 days: bonus remains intact up to 3 absent days, cancelled on 4th day)."
                                },
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )

                    // Custom OT Rate if in custom mode
                    if (isCustomSalary) {
                        OutlinedTextField(
                            value = customOtRateText,
                            onValueChange = { customOtRateText = it },
                            label = { Text(if (isBn) "কাস্টম ওটি রেট (প্রতি ঘণ্টা ৳)" else "Custom OT Rate (Per Hour ৳)") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_ot_rate_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            supportingText = { Text(if (isBn) "০ রাখলে স্বয়ংক্রিয় মূল বেতন/১০৪ রেট প্রযোজ্য হবে" else "Set 0 to use automatic Basic/104 formula") }
                        )
                    }

                    // Live Calculated Rate Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isBn) "কার্যকর ওটি রেট:" else "Effective OT Rate:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = LaborLawCalculator.formatCurrency(effectiveHourlyOtRate, isBn) + (if (isBn) " / ঘণ্টা" else " / hr"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isBn) "মোট গ্রস সাধারণ বেতন:" else "Total Gross Regular Salary:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                val calculatedGross = if (isCustomSalary) {
                                    val customBasic = basicSalaryText.toDoubleOrNull() ?: 0.0
                                    customBasic + (houseRentText.toDoubleOrNull() ?: 0.0) + (medicalText.toDoubleOrNull() ?: 0.0) + (conveyanceText.toDoubleOrNull() ?: 0.0) + (foodText.toDoubleOrNull() ?: 0.0)
                                } else {
                                    autoResult.grossSalary
                                }
                                Text(
                                    text = LaborLawCalculator.formatCurrency(calculatedGross, isBn),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Save Profile Button
        item {
            Button(
                onClick = {
                    val basic = if (isCustomSalary) {
                        basicSalaryText.toDoubleOrNull() ?: 0.0
                    } else {
                        autoResult.basicSalary
                    }
                    val house = if (isCustomSalary) (houseRentText.toDoubleOrNull() ?: 0.0) else autoResult.houseRent
                    val medical = if (isCustomSalary) (medicalText.toDoubleOrNull() ?: 750.0) else 750.0
                    val conveyance = if (isCustomSalary) (conveyanceText.toDoubleOrNull() ?: 0.0) else autoResult.conveyanceAllowance
                    val food = if (isCustomSalary) (foodText.toDoubleOrNull() ?: 0.0) else autoResult.foodAllowance
                    val attendance = attendanceBonusText.toDoubleOrNull() ?: 0.0
                    val maxAbsentThreshold = (maxAbsentForBonusText.toIntOrNull() ?: 0).coerceAtLeast(0)
                    val night = nightBillRateText.toDoubleOrNull() ?: 0.0
                    val tiffin = tiffinBillRateText.toDoubleOrNull() ?: 0.0
                    val customOt = customOtRateText.toDoubleOrNull() ?: 0.0

                    val updated = profile.copy(
                        userName = userName.trim(),
                        companyName = companyName.trim(),
                        designation = designation.trim(),
                        workerId = workerId.trim(),
                        grade = grade.trim(),
                        isCustomSalary = isCustomSalary,
                        basicSalary = basic,
                        houseRent = house,
                        medicalAllowance = medical,
                        conveyanceAllowance = conveyance,
                        foodAllowance = food,
                        attendanceBonus = attendance,
                        maxAbsentForBonus = maxAbsentThreshold,
                        nightBillRate = night,
                        tiffinBillRate = tiffin,
                        customOtRate = customOt,
                        supportWhatsapp = whatsappNumber.trim()
                    )
                    onSaveProfile(updated)
                    Toast.makeText(context, if (isBn) "প্রোফাইল ও বেতন সেটিংস সংরক্ষিত হয়েছে!" else "Profile & salary settings saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_profile_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBn) "সকল সেটিংস সংরক্ষণ করুন" else "Save All Settings", fontWeight = FontWeight.Bold)
            }
        }

        // Help & Privacy Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(if (isBn) "সেটিংস ও সহায়তা (Settings & Support)" else "Settings & Support", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    // Recycle Bin / Trash Option
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTrashDialog = true }
                            .testTag("trash_bin_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isBn) "রিসাইকেল বিন (Trash)" else "Recycle Bin (Trash)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (isBn) "ডিলিট করা ওটি ও পিস রেট ১ দিন জমা থাকে" else "Deleted records are kept for 1 day",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                if (trashItems.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = if (isBn) "${trashItems.size}টি" else "${trashItems.size} items",
                                            color = MaterialTheme.colorScheme.onError,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // WhatsApp Direct Help Button
                    OutlinedButton(
                        onClick = {
                            val whatsappUrl = "https://wa.me/8801988697470"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, if (isBn) "হোয়াটসঅ্যাপ ওপেন করা যায়নি। লিংক: $whatsappUrl" else "Could not open WhatsApp. Link: $whatsappUrl", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("whatsapp_help_button")
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "হোয়াটসঅ্যাপে সরাসরি সাহায্য নিন" else "Get Direct WhatsApp Support", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }

                    // Privacy Policy Button
                    TextButton(
                        onClick = {
                            val privacyUrl = "https://mahmudappsofficial-072.github.io/overtime-tracker-privacy-policy/"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    if (isBn) "ব্রাউজার খুলতে সমস্যা হয়েছে" else "Unable to open browser",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("privacy_policy_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "গোপনীয়তা নীতিমালা (Privacy Policy) দেখুন" else "View Privacy Policy")
                    }
                }
            }
        }
    }

    // Settings Modal / Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(LanguageManager.get("settings", lang), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Language Switcher Row
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(if (isBn) "অ্যাপের ভাষা" else "App Language", fontWeight = FontWeight.SemiBold)
                                Text(if (isBn) "বর্তমান: বাংলা" else "Current: English", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = onToggleLanguage) {
                                Text(if (isBn) "Switch to English" else "বাংলায় পরিবর্তন")
                            }
                        }
                    }

                    // Cloud Sync Section (সম্পূর্ণ স্বয়ংক্রিয় ব্যাকআপ - কোনো ম্যানুয়াল বাটন নেই)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBn) "স্বয়ংক্রিয় ক্লাউড ব্যাকআপ" else "Auto Cloud Sync",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SuccessGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isBn) "সক্রিয় (Active)" else "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            val syncDateStr = if (profile.lastCloudSyncTime > 0) {
                                SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(profile.lastCloudSyncTime))
                            } else if (currentUser != null) {
                                if (isBn) "রিয়েল-টাইমে কানেক্টেড" else "Connected"
                            } else {
                                if (isBn) "গুগল সাইন-ইন করলে স্বয়ংক্রিয় সিঙ্ক হবে" else "Sign in to enable auto cloud sync"
                            }

                            Text(
                                text = if (isBn)
                                    "আপনার সকল ওটি, পিস রেট এবং প্রোফাইল তথ্য স্বয়ংক্রিয়ভাবে ক্লাউডে রিয়েল-টাইমে সংরক্ষিত হচ্ছে। কোনো ম্যানুয়াল বাটনে চাপ দেওয়ার প্রয়োজন নেই।"
                                else
                                    "All your OT, piece rate, and profile records are automatically backed up to the cloud in real time.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "সর্বশেষ অটো-সিঙ্ক: $syncDateStr" else "Last Auto-Sync: $syncDateStr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Delete All Button
                            OutlinedButton(
                                onClick = { showDeleteAllConfirmDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("delete_all_data_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "Delete All (সকল ডেটা মুছুন)" else "Delete All Data",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text(if (isBn) "সম্পন্ন" else "Done")
                }
            }
        )
    }

    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isBn) "সব ডেটা মুছে ফেলতে চান?" else "Delete All Data?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = if (currentUser != null) {
                        if (isBn)
                            "সতর্কতা: এটি নিশ্চিত করলে আপনার ফোনের লোকাল ডেটাবেজ এবং গুগল ক্লাউড (Firestore)-এর সকল ওভারটাইম, পিস রেট ও বেতনের হিসাব স্থায়ীভাবে মুছে শূন্য (0) হয়ে যাবে। এটি আর ফিরিয়ে আনা সম্ভব হবে না। আপনি কি নিশ্চিত?"
                        else
                            "Warning: Confirming this will permanently delete all your overtime, piece rate, and salary settings from both local device storage and Google Cloud (Firestore). This action cannot be undone. Are you sure?"
                    } else {
                        if (isBn)
                            "সতর্কতা: এটি নিশ্চিত করলে আপনার ফোনের লোকাল ডেটাবেজে সংরক্ষিত সকল ওভারটাইম, পিস রেট ও বেতনের হিসাব স্থায়ীভাবে মুছে শূন্য (0) হয়ে যাবে। এটি আর ফিরিয়ে আনা সম্ভব হবে না। আপনি কি নিশ্চিত?"
                        else
                            "Warning: Confirming this will permanently delete all your local overtime, piece rate, and salary settings. This action cannot be undone. Are you sure?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirmDialog = false
                        showSettingsDialog = false
                        userName = ""
                        companyName = ""
                        designation = ""
                        workerId = ""
                        grade = ""
                        grossSalaryText = "0"
                        basicSalaryText = "0"
                        houseRentText = "0"
                        medicalText = "0"
                        conveyanceText = "0"
                        foodText = "0"
                        attendanceBonusText = "0"
                        maxAbsentForBonusText = "0"
                        nightBillRateText = "0"
                        tiffinBillRateText = "0"
                        customOtRateText = "0"
                        whatsappNumber = ""
                        isCustomSalary = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_all_button")
                ) {
                    Text(
                        text = if (isBn) "হ্যাঁ, নিশ্চিত করুন" else "Yes, Delete All",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAllConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_delete_all_button")
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = {
                Text(
                    text = if (isBn) "লগআউট নিশ্চিতকরণ" else "Confirm Sign Out",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBn)
                        "আপনি কি নিশ্চিত যে আপনার গুগল অ্যাকাউন্ট থেকে লগআউট করতে চান? ক্লাউড ব্যাকআপ আবার সক্রিয় করতে পুনরায় লগইন করতে হবে।"
                    else
                        "Are you sure you want to sign out of your Google account? You will need to sign in again to enable cloud backup."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "লগআউট করুন" else "Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    if (showTrashDialog) {
        TrashManagementDialog(
            trashItems = trashItems,
            isBn = isBn,
            onRestore = { item ->
                onRestoreTrash(item)
                Toast.makeText(context, if (isBn) "রেকর্ডটি সফলভাবে রিস্টোর করা হয়েছে!" else "Record restored successfully!", Toast.LENGTH_SHORT).show()
            },
            onDeletePermanently = { item ->
                onDeleteTrashPermanently(item)
                Toast.makeText(context, if (isBn) "রেকর্ডটি স্থায়ীভাবে মুছে ফেলা হয়েছে" else "Record permanently deleted", Toast.LENGTH_SHORT).show()
            },
            onClearAll = {
                onClearAllTrash()
                Toast.makeText(context, if (isBn) "রিসাইকেল বিন সম্পূর্ণ খালি করা হয়েছে" else "Recycle bin emptied completely", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showTrashDialog = false }
        )
    }
}

@Composable
fun TrashManagementDialog(
    trashItems: List<com.example.data.model.TrashItem>,
    isBn: Boolean = true,
    onRestore: (com.example.data.model.TrashItem) -> Unit,
    onDeletePermanently: (com.example.data.model.TrashItem) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var itemToDeletePermanently by remember { mutableStateOf<com.example.data.model.TrashItem?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBn) "রিসাইকেল বিন (Trash)" else "Recycle Bin (Trash)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                if (trashItems.isNotEmpty()) {
                    TextButton(onClick = { showClearAllConfirm = true }) {
                        Text(if (isBn) "সব মুছুন" else "Empty All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isBn) {
                        "ডিলিট করা রেকর্ডগুলো এখানে ১ দিন (২৪ ঘণ্টা) জমা থাকে। এর মধ্যে রিস্টোর না করলে তা স্বয়ংক্রিয়ভাবে চিরতরে মুছে যাবে।"
                    } else {
                        "Deleted records are stored here for 1 day (24 hours). If not restored within this time, they will be permanently deleted."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (trashItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBn) "রিসাইকেল বিন সম্পূর্ণ খালি!" else "Recycle Bin is completely empty!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isBn) "কোনো ডিলিট করা ওটি বা পিস রেট নেই" else "No deleted overtime or piece rate records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(trashItems.size, key = { trashItems[it].id }) { index ->
                            val item = trashItems[index]
                            val now = System.currentTimeMillis()
                            val elapsedMillis = now - item.deletedAt
                            val remainingHours = kotlin.math.max(0, 24 - (elapsedMillis / (1000 * 60 * 60)).toInt())

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (item.itemType == "OT") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = if (item.itemType == "OT") {
                                                    if (isBn) "ওভারটাইম" else "Overtime"
                                                } else {
                                                    if (isBn) "পিস রেট" else "Piece Rate"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.itemType == "OT") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = if (isBn) "বাকি: $remainingHours ঘণ্টা" else "$remainingHours hrs left",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    val displayTitle = if (isBn) item.title else item.title.replace("ওভারটাইম:", "Overtime:").replace("পিস রেট:", "Piece Rate:")
                                    val displaySubtitle = if (isBn) item.subtitle else item.subtitle.replace("ঘণ্টা", "hrs").replace("পিস", "pcs")
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = displaySubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { itemToDeletePermanently = item }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isBn) "স্থায়ী মুছুন" else "Delete Permanently", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onRestore(item) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isBn) "রিস্টোর" else "Restore", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isBn) "বন্ধ করুন" else "Close")
            }
        }
    )

    // Permanent delete confirmation dialog
    if (itemToDeletePermanently != null) {
        AlertDialog(
            onDismissRequest = { itemToDeletePermanently = null },
            title = { Text(if (isBn) "স্থায়ীভাবে মুছে ফেলবেন?" else "Delete permanently?") },
            text = { Text(if (isBn) "এই রেকর্ডটি চিরতরে মুছে ফেলা হবে এবং আর ফিরিয়ে আনা যাবে না।" else "This record will be permanently deleted and cannot be restored.") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDeletePermanently?.let { onDeletePermanently(it) }
                        itemToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "স্থায়ী মুছুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeletePermanently = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Clear all confirm
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text(if (isBn) "রিসাইকেল বিন খালি করবেন?" else "Empty Recycle Bin?") },
            text = { Text(if (isBn) "ট্র্যাশের সকল রেকর্ড চিরতরে মুছে যাবে। আপনি কি নিশ্চিত?" else "All records in trash will be permanently deleted. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "সব মুছুন" else "Empty All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
