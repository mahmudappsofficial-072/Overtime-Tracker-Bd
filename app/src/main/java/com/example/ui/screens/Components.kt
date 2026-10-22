package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OvertimeRecord
import com.example.util.LaborLawCalculator
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ShiftType(
    val code: String,
    val labelBn: String,
    val labelEn: String,
    val color: Color
) {
    REGULAR("REGULAR", "নিয়মিত", "Regular", Color(0xFF059669)),
    DAY("DAY", "দিন", "Day", Color(0xFFD97706)),
    NIGHT("NIGHT", "রাত", "Night", Color(0xFF2563EB)),
    HOLIDAY("HOLIDAY", "ছুটির দিন", "Holiday", Color(0xFF9333EA)),
    WEEKLY_OFF("WEEKLY_OFF", "সাপ্তাহিক ছুটি", "Weekly Off", Color(0xFF0D9488)),
    LEAVE("LEAVE", "সাময়িক ছুটি", "Leave", Color(0xFF0284C7)),
    ABSENT("ABSENT", "অনুপস্থিত", "Absent", Color(0xFFDC2626));

    fun getLabel(isBn: Boolean) = if (isBn) labelBn else labelEn

    companion object {
        fun fromCode(code: String): ShiftType = entries.firstOrNull { it.code == code } ?: REGULAR
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalDatePickerField(
    selectedDate: String, // "YYYY-MM-DD"
    onDateSelected: (String) -> Unit,
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayDateStr = remember { sdf.format(Date()) }
    val yesterdayDateStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        sdf.format(cal.time)
    }

    // Format display string
    val formattedDisplayDate = remember(selectedDate, isBn) {
        try {
            val parsed = sdf.parse(selectedDate)
            if (parsed != null) {
                val cal = Calendar.getInstance().apply { time = parsed }
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val dayName = when (dayOfWeek) {
                    Calendar.SATURDAY -> if (isBn) "শনিবার" else "Saturday"
                    Calendar.SUNDAY -> if (isBn) "রবিবার" else "Sunday"
                    Calendar.MONDAY -> if (isBn) "সোমবার" else "Monday"
                    Calendar.TUESDAY -> if (isBn) "মঙ্গলবার" else "Tuesday"
                    Calendar.WEDNESDAY -> if (isBn) "বুধবার" else "Wednesday"
                    Calendar.THURSDAY -> if (isBn) "বৃহস্পতিবার" else "Thursday"
                    Calendar.FRIDAY -> if (isBn) "শুক্রবার" else "Friday"
                    else -> ""
                }
                val formatted = SimpleDateFormat("dd MMMM yyyy", if (isBn) Locale.forLanguageTag("bn-BD") else Locale.US).format(parsed)
                val display = if (isBn) LaborLawCalculator.toBengaliDigits(formatted) else formatted
                val suffix = when (selectedDate) {
                    todayDateStr -> if (isBn) " • আজকের দিন" else " • Today"
                    yesterdayDateStr -> if (isBn) " • গতকাল" else " • Yesterday"
                    else -> ""
                }
                "$display ($dayName)$suffix"
            } else selectedDate
        } catch (e: Exception) {
            selectedDate
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Quick Action Chips: [ 📌 আজ ], [ ⏪ গতকাল ], [ 📅 ক্যালেন্ডার ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedDate == todayDateStr,
                onClick = { onDateSelected(todayDateStr) },
                label = { Text(if (isBn) "📌 আজ" else "📌 Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("date_chip_today")
            )

            FilterChip(
                selected = selectedDate == yesterdayDateStr,
                onClick = { onDateSelected(yesterdayDateStr) },
                label = { Text(if (isBn) "⏪ গতকাল" else "⏪ Yesterday", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("date_chip_yesterday")
            )

            AssistChip(
                onClick = { showDatePickerDialog = true },
                label = { Text(if (isBn) "📅 ক্যালেন্ডার" else "📅 Calendar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("date_chip_calendar")
            )
        }

        // Tappable Date Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePickerDialog = true }
                .testTag("tappable_date_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isBn) "নির্বাচিত তারিখ (চাপুন):" else "Selected Date (Tap):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedDisplayDate,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isBn) "বদলান" else "Change",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                sdf.parse(selectedDate)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val pickedStr = String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            onDateSelected(pickedStr)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(if (isBn) "ঠিক আছে" else "OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = if (isBn) "তারিখ বেছে নিন" else "Select Date",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }
}

@Composable
fun QuickOtEntryDialog(
    initialRecord: OvertimeRecord? = null,
    existingRecords: List<OvertimeRecord> = emptyList(),
    hourlyRate: Double,
    profile: com.example.data.model.UserProfile? = null,
    lang: String,
    onDismiss: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onSave: (date: String, hours: Double, nightBill: Boolean, tiffinBill: Boolean, isHoliday: Boolean, shiftType: String, note: String) -> Unit
) {
    val isBn = lang == "bn"
    val todayStr = remember {
        initialRecord?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    var date by remember { mutableStateOf(todayStr) }
    var selectedShift by remember {
        mutableStateOf(initialRecord?.shiftType ?: "REGULAR")
    }
    var hoursText by remember {
        mutableStateOf(if (initialRecord != null) initialRecord.otHours.toString() else "0")
    }
    var hasNightBill by remember { mutableStateOf(initialRecord?.hasNightBill ?: false) }
    var hasTiffinBill by remember { mutableStateOf(initialRecord?.hasTiffinBill ?: false) }
    var isHoliday by remember { mutableStateOf(initialRecord?.isHoliday ?: false) }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }

    // Warning dialog type: "SALARY", "NIGHT_BILL", "TIFFIN_BILL" or null
    var missingFieldWarning by remember { mutableStateOf<String?>(null) }
    // Overwrite confirmation dialog holds the existing record that would be overwritten
    var pendingOverwriteRecord by remember { mutableStateOf<OvertimeRecord?>(null) }

    val presetHours = listOf(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0)
    val parsedHours = hoursText.toDoubleOrNull() ?: 0.0
    val calculatedAmount = parsedHours * hourlyRate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialRecord != null && initialRecord.id != 0L) "ওটি তথ্য সম্পাদন" else LanguageManager.get("quick_ot_entry", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Professional Date Picker Field with Quick Chips
                ProfessionalDatePickerField(
                    selectedDate = date,
                    onDateSelected = { date = it },
                    isBn = isBn
                )

                // Shift Selector Section
                Text(
                    text = if (isBn) "শিফট / উপস্থিতির ধরন:" else "Shift / Attendance Type:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Row 1 of Shift Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(ShiftType.REGULAR, ShiftType.DAY, ShiftType.NIGHT, ShiftType.HOLIDAY).forEach { shift ->
                        val isSelected = selectedShift == shift.code
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedShift = shift.code
                                when (shift) {
                                    ShiftType.NIGHT -> {
                                        if ((profile?.nightBillRate ?: 0.0) <= 0.0) {
                                            hasNightBill = false
                                            missingFieldWarning = "NIGHT_BILL"
                                        } else {
                                            hasNightBill = true
                                        }
                                    }
                                    ShiftType.HOLIDAY -> isHoliday = true
                                    ShiftType.DAY -> {
                                        isHoliday = false
                                    }
                                    ShiftType.REGULAR -> {
                                        isHoliday = false
                                    }
                                    else -> {}
                                }
                            },
                            label = { Text(shift.getLabel(isBn), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = shift.color.copy(alpha = 0.2f),
                                selectedLabelColor = shift.color
                            )
                        )
                    }
                }

                // Row 2 of Shift Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(ShiftType.WEEKLY_OFF, ShiftType.LEAVE, ShiftType.ABSENT).forEach { shift ->
                        val isSelected = selectedShift == shift.code
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedShift = shift.code
                                when (shift) {
                                    ShiftType.WEEKLY_OFF -> isHoliday = true
                                    ShiftType.LEAVE, ShiftType.ABSENT -> {
                                        hoursText = "0.0"
                                        hasNightBill = false
                                        hasTiffinBill = false
                                    }
                                    else -> {}
                                }
                            },
                            label = { Text(shift.getLabel(isBn), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = shift.color.copy(alpha = 0.2f),
                                selectedLabelColor = shift.color
                            )
                        )
                    }
                }

                // When shift is Weekly Off, Leave, or Absent, hide all OT inputs and show status info
                val isOffOrLeaveShift = selectedShift == ShiftType.WEEKLY_OFF.code ||
                        selectedShift == ShiftType.LEAVE.code ||
                        selectedShift == ShiftType.ABSENT.code

                if (isOffOrLeaveShift) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (selectedShift) {
                            ShiftType.ABSENT.code -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ShiftType.LEAVE.code -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = when (selectedShift) {
                                ShiftType.ABSENT.code -> if (isBn)
                                    "⚠️ আজকের দিনটি অনুপস্থিত হিসেবে রেকর্ড হবে (দৈনিক বেতন কর্তন হবে এবং শর্তানুযায়ী হাজিরা বোনাস হিসাব হবে)।"
                                else
                                    "⚠️ Marked as Absent (Daily salary deducted; attendance bonus evaluated per policy)."
                                ShiftType.LEAVE.code -> if (isBn)
                                    "🏖️ আজকের দিনটি ছুটি হিসেবে গণ্য হবে (হাজিরা কাটা যাবে না)।"
                                else
                                    "🏖️ Marked as Leave (Attendance bonus preserved)."
                                else -> if (isBn)
                                    "🌴 আজকের দিনটি সাপ্তাহিক ছুটি হিসেবে সংরক্ষিত হবে (ওটি প্রযোজ্য নয়)।"
                                else
                                    "🌴 Marked as Weekly Off (No OT applicable)."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when (selectedShift) {
                                ShiftType.ABSENT.code -> MaterialTheme.colorScheme.onErrorContainer
                                ShiftType.LEAVE.code -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    // Regular OT Inputs (Visible only for regular/work shifts)

                    // Preset hour chips
                    Text(
                        text = if (isBn) "দ্রুত ওটি ঘণ্টা নির্বাচন:" else "Quick OT Hours Selection:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetHours.take(4).forEach { h ->
                            AssistChip(
                                onClick = { hoursText = h.toString() },
                                label = { Text("${LaborLawCalculator.formatNumber(h, isBn)} ${if (isBn) "ঘ" else "h"}") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (parsedHours == h) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetHours.drop(4).forEach { h ->
                            AssistChip(
                                onClick = { hoursText = h.toString() },
                                label = { Text("${LaborLawCalculator.formatNumber(h, isBn)} ${if (isBn) "ঘ" else "h"}") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (parsedHours == h) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    // Hours custom input
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it },
                        label = { Text(LanguageManager.get("hours", lang)) },
                        modifier = Modifier.fillMaxWidth().testTag("ot_hours_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        supportingText = {
                            Text(
                                text = if (isBn) "আনুমানিক ওটি টাকা: ${LaborLawCalculator.formatCurrency(calculatedAmount, isBn)}" else "Est. OT Amount: ${LaborLawCalculator.formatCurrency(calculatedAmount, isBn)}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    // Additional Checkboxes
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!hasNightBill) {
                                            if ((profile?.nightBillRate ?: 0.0) <= 0.0) {
                                                missingFieldWarning = "NIGHT_BILL"
                                            } else {
                                                hasNightBill = true
                                            }
                                        } else {
                                            hasNightBill = false
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = hasNightBill,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if ((profile?.nightBillRate ?: 0.0) <= 0.0) {
                                                missingFieldWarning = "NIGHT_BILL"
                                            } else {
                                                hasNightBill = true
                                            }
                                        } else {
                                            hasNightBill = false
                                        }
                                    },
                                    modifier = Modifier.testTag("night_bill_checkbox")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(LanguageManager.get("night_bill", lang), style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!hasTiffinBill) {
                                            if ((profile?.tiffinBillRate ?: 0.0) <= 0.0) {
                                                missingFieldWarning = "TIFFIN_BILL"
                                            } else {
                                                hasTiffinBill = true
                                            }
                                        } else {
                                            hasTiffinBill = false
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = hasTiffinBill,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if ((profile?.tiffinBillRate ?: 0.0) <= 0.0) {
                                                missingFieldWarning = "TIFFIN_BILL"
                                            } else {
                                                hasTiffinBill = true
                                            }
                                        } else {
                                            hasTiffinBill = false
                                        }
                                    },
                                    modifier = Modifier.testTag("tiffin_bill_checkbox")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(LanguageManager.get("tiffin_bill", lang), style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isHoliday = !isHoliday }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isHoliday,
                                    onCheckedChange = { isHoliday = it },
                                    modifier = Modifier.testTag("holiday_checkbox")
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(LanguageManager.get("holiday_ot", lang), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Note input
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(LanguageManager.get("note", lang)) },
                        modifier = Modifier.fillMaxWidth().testTag("ot_note_input"),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHours = hoursText.toDoubleOrNull() ?: 0.0
                    // Validation 1: If hours > 0 but basic salary or hourlyRate <= 0
                    if (finalHours > 0.0 && hourlyRate <= 0.0 && (profile?.basicSalary ?: 0.0) <= 0.0) {
                        missingFieldWarning = "SALARY"
                        return@Button
                    }
                    // Validation 2: If night bill selected but rate <= 0
                    if (hasNightBill && (profile?.nightBillRate ?: 0.0) <= 0.0) {
                        missingFieldWarning = "NIGHT_BILL"
                        return@Button
                    }
                    // Validation 3: If tiffin bill selected but rate <= 0
                    if (hasTiffinBill && (profile?.tiffinBillRate ?: 0.0) <= 0.0) {
                        missingFieldWarning = "TIFFIN_BILL"
                        return@Button
                    }

                    if (finalHours >= 0.0 && date.isNotBlank()) {
                        // Check if an existing record exists on the chosen date
                        val existingOnDate = existingRecords.find { it.date == date }
                        if (existingOnDate != null) {
                            val isDifferentRecord = initialRecord == null || initialRecord.id != existingOnDate.id
                            val isSameRecordModified = initialRecord != null && initialRecord.id == existingOnDate.id && (
                                    initialRecord.otHours != finalHours ||
                                    initialRecord.shiftType != selectedShift ||
                                    initialRecord.hasNightBill != hasNightBill ||
                                    initialRecord.hasTiffinBill != hasTiffinBill ||
                                    initialRecord.isHoliday != isHoliday ||
                                    initialRecord.note != note
                            )
                            if (isDifferentRecord || isSameRecordModified) {
                                pendingOverwriteRecord = existingOnDate
                                return@Button
                            }
                        }

                        onSave(date, finalHours, hasNightBill, hasTiffinBill, isHoliday, selectedShift, note)
                    }
                },
                modifier = Modifier.testTag("save_ot_button")
            ) {
                Text(LanguageManager.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageManager.get("cancel", lang))
            }
        }
    )

    // Interactive Alert Dialog when required rates are missing in profile
    if (missingFieldWarning != null) {
        val warningType = missingFieldWarning
        AlertDialog(
            onDismissRequest = { missingFieldWarning = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (warningType) {
                            "NIGHT_BILL" -> if (isBn) "নাইট বিল রেট যোগ করা নেই!" else "Night Bill Rate Missing!"
                            "TIFFIN_BILL" -> if (isBn) "টিফিন বিল রেট যোগ করা নেই!" else "Tiffin Bill Rate Missing!"
                            else -> if (isBn) "বেতন কাঠামো যোগ করা নেই!" else "Salary Setup Missing!"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Text(
                    text = when (warningType) {
                        "NIGHT_BILL" -> if (isBn)
                            "আপনার প্রোফাইলে এখনও প্রতি রাতের নাইট বিল রেট যোগ করা হয়নি। দয়া করে প্রোফাইলে গিয়ে নাইট বিলের রেট বসিয়ে নিন।"
                        else
                            "Night bill rate is not configured in your profile. Please add your night bill rate in profile settings."
                        "TIFFIN_BILL" -> if (isBn)
                            "আপনার প্রোফাইলে এখনও প্রতি দিনের টিফিন বিল রেট যোগ করা হয়নি। দয়া করে প্রোফাইলে গিয়ে টিফিন বিল রেট বসিয়ে নিন।"
                        else
                            "Tiffin bill rate is not configured in your profile. Please add your tiffin bill rate in profile settings."
                        else -> if (isBn)
                            "আপনার প্রোফাইলে এখনও মোট বেতন বা ওটি রেট যোগ করা হয়নি। ওটি যোগ করার আগে দয়া করে প্রোফাইলে গিয়ে বেতন কাঠামো নির্ধারণ করুন, অন্যথায় ওটি টাকা হিসাব করা যাবে না।"
                        else
                            "Salary or OT rate is not configured in your profile. Please set up your salary in profile settings first."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        missingFieldWarning = null
                        onNavigateToProfile()
                    }
                ) {
                    Text(if (isBn) "প্রোফাইলে যান" else "Go to Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { missingFieldWarning = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Overwrite Confirmation Alert Dialog
    if (pendingOverwriteRecord != null) {
        val oldRec = pendingOverwriteRecord!!
        val oldShift = ShiftType.fromCode(oldRec.shiftType).getLabel(isBn)
        val newShift = ShiftType.fromCode(selectedShift).getLabel(isBn)
        val finalHours = hoursText.toDoubleOrNull() ?: 0.0

        AlertDialog(
            onDismissRequest = { pendingOverwriteRecord = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isBn) "এই তারিখে ইতিমধ্যে ওটি তথ্য আছে!" else "Entry already exists for this date!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBn)
                            "আপনি $date তারিখের পূর্ববর্তী তথ্য পরিবর্তন করতে যাচ্ছেন:"
                        else
                            "You are about to overwrite existing data for $date:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isBn)
                                    "• আগের তথ্য: ${LaborLawCalculator.formatNumber(oldRec.otHours, isBn)} ঘণ্টা ($oldShift)"
                                else
                                    "• Current: ${oldRec.otHours} hrs ($oldShift)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (isBn)
                                    "• নতুন তথ্য: ${LaborLawCalculator.formatNumber(finalHours, isBn)} ঘণ্টা ($newShift)"
                                else
                                    "• New: $finalHours hrs ($newShift)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = if (isBn)
                            "আপনি কি নিশ্চিতভাবে আগের তথ্যটি পরিবর্তন (ওভাররাইট) করতে চান?"
                        else
                            "Are you sure you want to overwrite the previous entry?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toSaveId = oldRec.id
                        pendingOverwriteRecord = null
                        onSave(date, finalHours, hasNightBill, hasTiffinBill, isHoliday, selectedShift, note)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "পরিবর্তন নিশ্চিত করুন" else "Confirm Overwrite")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverwriteRecord = null }) {
                    Text(if (isBn) "বাতিল (আগেরটি রাখুন)" else "Cancel (Keep Current)")
                }
            }
        )
    }
}

@Composable
fun AddPieceRateDialog(
    lang: String,
    onDismiss: () -> Unit,
    onSave: (itemName: String, pieceCount: Int, ratePerPiece: Double, date: String, note: String) -> Unit
) {
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var itemName by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val count = countText.toIntOrNull() ?: 0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val total = count * rate

    val isBn = lang == "bn"
    val popularItems = if (isBn) {
        listOf("কলার তৈরি", "পকেট জয়েন্ট", "স্লিভ এটাচ", "বটম হেম", "বাটন স্টিচ")
    } else {
        listOf("Collar Making", "Pocket Joint", "Sleeve Attach", "Bottom Hem", "Button Stitch")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(LanguageManager.get("add_piece_work", lang), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Professional Date Picker Field with Quick Chips
                ProfessionalDatePickerField(
                    selectedDate = date,
                    onDateSelected = { date = it },
                    isBn = isBn
                )

                // Quick item suggestions
                Text(if (isBn) "জনপ্রিয় কাজের নাম:" else "Popular Task Suggestions:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    popularItems.take(3).forEach { item ->
                        AssistChip(
                            onClick = { itemName = item },
                            label = { Text(item, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(LanguageManager.get("item_name", lang)) },
                    modifier = Modifier.fillMaxWidth().testTag("piece_name_input"),
                    placeholder = { Text(if (isBn) "যেমন: শার্ট কলার জয়েন্ট" else "e.g. Shirt Collar Joint") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text(LanguageManager.get("piece_count", lang)) },
                        modifier = Modifier.weight(1f).testTag("piece_count_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text(LanguageManager.get("rate_per_piece", lang)) },
                        modifier = Modifier.weight(1f).testTag("piece_rate_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBn) "মোট পিস রেট আয়: ${LaborLawCalculator.formatCurrency(total, true)}" else "Total Piece Income: ${LaborLawCalculator.formatCurrency(total, false)}",
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(LanguageManager.get("note", lang)) },
                    modifier = Modifier.fillMaxWidth().testTag("piece_note_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isNotBlank() && count > 0 && rate > 0.0) {
                        onSave(itemName, count, rate, date, note)
                    }
                },
                modifier = Modifier.testTag("save_piece_button")
            ) {
                Text(LanguageManager.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageManager.get("cancel", lang))
            }
        }
    )
}

@Composable
fun TargetGoalDialog(
    currentTarget: Double,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val isBn = lang == "bn"
    var targetText by remember { mutableStateOf(currentTarget.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(LanguageManager.get("target_hours", lang), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isBn)
                        "মাসে আপনি মোট কত ঘণ্টা ওভারটাইম করার পরিকল্পনা করছেন? লক্ষ্য নির্ধারণ করলে প্রতিদিনের হিসাব সহজ হয়।"
                    else
                        "How many hours of overtime do you plan to do this month? Setting a target helps track daily progress.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text(if (isBn) "লক্ষ্যমাত্রা (ঘণ্টা)" else "Target (Hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("target_hours_input"),
                    singleLine = true
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(40, 50, 60, 70, 80).forEach { preset ->
                        FilterChip(
                            selected = targetText == preset.toString(),
                            onClick = { targetText = preset.toString() },
                            label = { Text("${LaborLawCalculator.formatNumber(preset.toDouble(), isBn, 0)} ${if (isBn) "ঘ" else "h"}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: currentTarget
                    onSave(target)
                },
                modifier = Modifier.testTag("save_target_button")
            ) {
                Text(LanguageManager.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageManager.get("cancel", lang))
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(
    lang: String,
    onDismiss: () -> Unit
) {
    val isBn = lang == "bn"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(LanguageManager.get("privacy_policy", lang), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isBn) "ওভারটাইম ট্র্যাকার বিডি - গোপনীয়তা ও সুরক্ষা নীতিমালা" else "Overtime Tracker BD - Privacy & Security Policy",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isBn) {
                        "১. ডাটা নিরাপত্তা: আপনার সকল ওভারটাইম ও ব্যক্তিগত তথ্য প্রাথমিকভাবে আপনার নিজস্ব ডিভাইসে এনক্রিপ্টেড ও সুরক্ষিতভাবে সংরক্ষিত থাকে।\n\n" +
                        "২. কোনো ট্র্যাকিং নেই: এই অ্যাপ্লিকেশন কোনো তৃতীয় পক্ষের বিজ্ঞাপন ট্র্যাকিং বা আপনার তথ্য অন্য কোনো কোম্পানির কাছে বিক্রি করে না।\n\n" +
                        "৩. ক্লাউড ব্যাকআপ: আপনি যখন ক্লাউড সিঙ্ক ফিচার ব্যবহার করবেন, শুধুমাত্র তখনই আপনার তথ্য ফায়ারবেস ক্লাউডে সুরক্ষিত ডাটাবেসে সেভ হবে যেন ফোন বদলালেও তথ্য নিরাপদ থাকে।\n\n" +
                        "৪. বাংলাদেশ শ্রম আইন সম্মতি: অ্যাপের সকল গাণিতিক সূত্র বাংলাদেশ শ্রম আইন ২০০৬ অনুযায়ী তৈরি।\n\n" +
                        "৫. যোগাযোগ ও অভিযোগ: যে কোনো জিজ্ঞাসা বা ডেটা মুছে ফেলার অনুরোধে সরাসরি অ্যাপের হোয়াটসঅ্যাপ সাপোর্ট বা ইমেইলে যোগাযোগ করতে পারবেন।\n\n" +
                        "৬. পুশ নোটিফিকেশন ও রিমাইন্ডার: আপনাকে গুরুত্বপূর্ণ নোটিশ, আপডেট ও দৈনিক ওটি রিমাইন্ডার পাঠাতে ফায়ারবেস ক্লাউড মেসেজিং (FCM) ও স্থানীয় অ্যালার্ম ব্যবহার করা হয়। এতে কোনো বিজ্ঞাপন বা স্প্যাম নেই এবং আপনি যেকোনো সময় ফোনের সেটিংস থেকে নোটিফিকেশন নিয়ন্ত্রণ করতে পারবেন।"
                    } else {
                        "1. Data Security: All your overtime logs and personal salary information are initially stored securely on your local device.\n\n" +
                        "2. Zero Tracking: This application does not track you with third-party advertising or sell your information to any third parties.\n\n" +
                        "3. Cloud Backup: Only when you enable Cloud Sync with your Google Account, your data is securely backed up to Firebase Cloud so you never lose your records.\n\n" +
                        "4. Labor Law Compliance: All mathematical calculation formulas strictly adhere to the Bangladesh Labor Act 2006.\n\n" +
                        "5. Contact & Deletion: For support or requesting full data deletion, you can reach out directly via WhatsApp support or email.\n\n" +
                        "6. Push Notifications & Reminders: Firebase Cloud Messaging (FCM) and local alarms send important announcements and daily OT reminders. No ads or spam are sent, and you can disable notifications anytime in settings."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isBn) "বুঝেছি" else "Understood")
            }
        }
    )
}

@Composable
fun InitialPrivacyConsentDialog(
    lang: String,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    onViewFullPolicy: () -> Unit,
    titleText: String? = null,
    confirmButtonText: String? = null,
    dismissButtonText: String? = null
) {
    val isBn = lang == "bn"

    AlertDialog(
        onDismissRequest = onDecline,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = titleText ?: if (isBn) "গোপনীয়তা ও ডেটা সুরক্ষার সম্মতি" else "Privacy & Data Consent",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isBn) "Overtime Tracker BD (ওভারটাইম ট্র্যাকার বিডি)" else "Overtime Tracker BD",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isBn)
                                "ওভারটাইম ট্র্যাকার বিডি অ্যাপটি আপনার কাজের সময়, বেতন ও হাজিরা সঠিকভাবে হিসাব করার জন্য প্রস্তুত করা হয়েছে।"
                            else
                                "Overtime Tracker BD is designed to accurately track your work hours, salary calculations, and attendance.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isBn)
                                "• আপনার অনুমতি সাপেক্ষে বেতন, ভাতা ও ওটি তথ্য ডিভাইসে ও ক্লাউডে (Google Firebase) সংরক্ষিত হবে।\n• আমরা কোনো তথ্য তৃতীয় পক্ষের কাছে বিক্রি বা শেয়ার করি না।\n• আপনি যেকোনো সময় সাইন-আউট করে ডেটা মুছে ফেলতে পারেন।"
                            else
                                "• With your consent, salary figures and OT logs are saved on device & cloud (Google Firebase).\n• We never sell or share your personal data with third parties.\n• You can sign out or request permanent data deletion at any time.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = if (isBn) "আমাদের পূর্ণাঙ্গ নীতিমালা:" else "Our complete policy:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onViewFullPolicy,
                        modifier = Modifier.testTag("consent_view_full_policy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBn) "বিস্তারিত পড়ুন" else "Read Details",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAgree,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("consent_agree_button")
            ) {
                Text(
                    text = confirmButtonText ?: if (isBn) "সম্মত আছি ও প্রবেশ করুন" else "Agree & Continue",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("consent_decline_button")
            ) {
                Text(
                    text = dismissButtonText ?: if (isBn) "বাতিল (বেরিয়ে যান)" else "Decline & Exit"
                )
            }
        }
    )
}

