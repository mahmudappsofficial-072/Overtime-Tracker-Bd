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
    onSyncCloud: ((Boolean, String) -> Unit) -> Unit = {}
) {
    val context = LocalContext.current
    val isBn = lang == "bn"

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
        mutableStateOf(if (profile.medicalAllowance == 0.0 || profile.medicalAllowance == 1250.0) "750" else profile.medicalAllowance.toInt().toString())
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

    // Dialog flags
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
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
                    text = "প্রোফাইল ও বেতন সেটিংস",
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
                                text = userName.ifBlank { "আপনার নাম" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${companyName.ifBlank { "ফ্যাক্টরি" }} • ${designation.ifBlank { "পদবি" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "আইডি: ${workerId.ifBlank { "N/A" }} • ${grade.ifBlank { "গ্রেড" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider()

                    // Editable Personal Info
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("শ্রমিকের নাম") },
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
                            label = { Text("কোম্পানি / ফ্যাক্টরি") },
                            modifier = Modifier.weight(1f).testTag("profile_company_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = designation,
                            onValueChange = { designation = it },
                            label = { Text("পদবি") },
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
                            label = { Text("আইডি নম্বর") },
                            modifier = Modifier.weight(1f).testTag("profile_id_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { grade = it },
                            label = { Text("গ্রেড") },
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
                        text = "বেতন কাঠামো ও হিসাব পদ্ধতি",
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
                                    text = "স্বয়ংক্রিয় (আইন অনুযায়ী)",
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
                                    text = "কাস্টম (নিজস্ব নিয়ম)",
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
                                text = "⚖️ সরকারি নিয়ম অনুযায়ী মোট বেতন দিলে মূল বেতন, বাড়িভাড়া ও ভাতাসমূহ স্বয়ংক্রিয়ভাবে হিসাব হবে। ওটি হার = মূল বেতন ÷ ১০৪ (দ্বিগুণ হার)।",
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
                                text = "⚙️ কাস্টম মোড: আপনার ফ্যাক্টরির নিয়ম অনুযায়ী মূল বেতন, যেকোনো ভাতা বা প্রতি ঘণ্টার ওটি রেট নিজের মতো বসিয়ে নিতে পারবেন।",
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
                            label = { Text("মোট বেতন (Gross Salary ৳)") },
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
                            label = { Text("মূল বেতন (Basic Salary ৳)") },
                            modifier = Modifier.fillMaxWidth().testTag("basic_salary_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) }
                        )
                    }

                    // Allowances Section
                    Text(
                        text = if (!isCustomSalary) "সরকারি নিয়মে স্বয়ংক্রিয় হিসাব (লকড):" else "ভাতাসমূহ (Allowances):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    // Basic Salary (Locked in Auto mode, editable in Custom mode)
                    if (!isCustomSalary) {
                        OutlinedTextField(
                            value = autoResult.basicSalary.toInt().toString(),
                            onValueChange = {},
                            label = { Text("মূল বেতন (Basic Salary - লকড)") },
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
                            label = { Text("বাড়িভাড়া (House Rent)") },
                            enabled = isCustomSalary,
                            modifier = Modifier.weight(1f).testTag("house_rent_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = if (isCustomSalary) medicalText else "750",
                            onValueChange = { if (isCustomSalary) medicalText = it },
                            label = { Text("চিকিৎসা ভাতা (Fixed ৳ ৭৫০)") },
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
                            label = { Text("যাতায়াত ভাতা") },
                            enabled = isCustomSalary,
                            modifier = Modifier.weight(1f).testTag("conveyance_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = if (isCustomSalary) foodText else autoResult.foodAllowance.toInt().toString(),
                            onValueChange = { if (isCustomSalary) foodText = it },
                            label = { Text("খাদ্য ভাতা") },
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
                            label = { Text("নাইট বিল (প্রতি রাত ৳)") },
                            modifier = Modifier.weight(1f).testTag("night_rate_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tiffinBillRateText,
                            onValueChange = { tiffinBillRateText = it },
                            label = { Text("টিফিন বিল (প্রতি দিন ৳)") },
                            modifier = Modifier.weight(1f).testTag("tiffin_rate_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = attendanceBonusText,
                        onValueChange = { attendanceBonusText = it },
                        label = { Text("হাজিরা বোনাস (৳)") },
                        modifier = Modifier.fillMaxWidth().testTag("attendance_bonus_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    // Custom OT Rate if in custom mode
                    if (isCustomSalary) {
                        OutlinedTextField(
                            value = customOtRateText,
                            onValueChange = { customOtRateText = it },
                            label = { Text("কাস্টম ওটি রেট (প্রতি ঘণ্টা ৳)") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_ot_rate_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            supportingText = { Text("০ রাখলে স্বয়ংক্রিয় মূল বেতন/১০৪ রেট প্রযোজ্য হবে") }
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
                                    text = "কার্যকর ওটি রেট:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = LaborLawCalculator.formatCurrency(effectiveHourlyOtRate, isBn) + " / ঘণ্টা",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "মোট গ্রস সাধারণ বেতন:",
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
                        nightBillRate = night,
                        tiffinBillRate = tiffin,
                        customOtRate = customOt,
                        supportWhatsapp = whatsappNumber.trim()
                    )
                    onSaveProfile(updated)
                    Toast.makeText(context, "প্রোফাইল ও বেতন সেটিংস সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_profile_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("সকল সেটিংস সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
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
                    Text("সহায়তা ও গোপনীয়তা (Help & Privacy)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    // WhatsApp Direct Help Button
                    OutlinedButton(
                        onClick = {
                            val whatsappUrl = "https://wa.me/8801988697470"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "হোয়াটসঅ্যাপ ওপেন করা যায়নি। লিংক: $whatsappUrl", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("whatsapp_help_button")
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("হোয়াটসঅ্যাপে সরাসরি সাহায্য নিন", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }

                    // Privacy Policy Button
                    TextButton(
                        onClick = {
                            val privacyUrl = "https://mahmudappsofficial-072.github.io/overtime-tracker-privacy-policy/"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to built-in dialog if browser cannot be launched
                                showPrivacyDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("privacy_policy_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("গোপনীয়তা নীতিমালা (Privacy Policy) দেখুন")
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
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
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
                                Text("অ্যাপের ভাষা", fontWeight = FontWeight.SemiBold)
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
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("সম্পন্ন")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            lang = lang,
            onDismiss = { showPrivacyDialog = false }
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
}
