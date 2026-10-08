package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.OvertimeRecord
import com.example.data.model.PieceRateRecord
import com.example.data.model.UserProfile
import com.example.data.repository.OvertimeRepository
import com.example.util.LaborLawCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OvertimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OvertimeRepository
    init {
        val db = AppDatabase.getDatabase(application)
        repository = OvertimeRepository(db)
        viewModelScope.launch {
            com.example.data.auth.AuthManager.currentUser.collect { user ->
                if (user != null) {
                    repository.fullAutoSyncAndRestore()
                }
            }
        }
    }

    private val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private val _selectedYearMonth = MutableStateFlow(currentMonthStr)
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyOtRecords: StateFlow<List<OvertimeRecord>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getOvertimeRecords(ym) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyPieceRecords: StateFlow<List<PieceRateRecord>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getPieceRateRecords(ym) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Summary calculation state
    val monthlySummary = combine(
        userProfile,
        monthlyOtRecords,
        monthlyPieceRecords
    ) { profile, otList, pieceList ->
        val otRate = LaborLawCalculator.getHourlyOtRate(profile)
        var totalOtHours = 0.0
        var totalOtAmount = 0.0
        var nightBillCount = 0
        var tiffinBillCount = 0

        for (rec in otList) {
            totalOtHours += rec.otHours
            totalOtAmount += rec.otHours * otRate
            if (rec.hasNightBill) nightBillCount++
            if (rec.hasTiffinBill) tiffinBillCount++
        }

        val nightBillTotal = nightBillCount * profile.nightBillRate
        val tiffinBillTotal = tiffinBillCount * profile.tiffinBillRate

        var totalPieceAmount = 0.0
        for (p in pieceList) {
            totalPieceAmount += p.totalAmount
        }

        var regularDutyDays = 0
        var dayShiftDays = 0
        var nightShiftDays = 0
        var holidayDutyDays = 0
        var weeklyOffDays = 0
        var leaveDays = 0
        var absentDays = 0

        for (rec in otList) {
            when (rec.shiftType) {
                "REGULAR" -> regularDutyDays++
                "DAY" -> dayShiftDays++
                "NIGHT" -> nightShiftDays++
                "HOLIDAY" -> holidayDutyDays++
                "WEEKLY_OFF" -> weeklyOffDays++
                "LEAVE" -> leaveDays++
                "ABSENT" -> absentDays++
                else -> regularDutyDays++
            }
        }

        val allowances = if (profile.basicSalary <= 0.0) {
            0.0
        } else if (profile.isCustomSalary) {
            profile.houseRent + profile.medicalAllowance + profile.conveyanceAllowance + profile.foodAllowance
        } else {
            val auto = LaborLawCalculator.calculateAutoAllowances(profile.basicSalary)
            auto.houseRent + auto.medicalAllowance + auto.conveyanceAllowance + auto.foodAllowance
        }

        val grossSalary = if (profile.basicSalary > 0.0) profile.basicSalary + allowances else 0.0
        val attendanceBonus = if (profile.basicSalary > 0.0 && absentDays == 0) profile.attendanceBonus else 0.0
        val totalEstimatedEarnings = grossSalary + totalOtAmount + nightBillTotal + tiffinBillTotal + totalPieceAmount + attendanceBonus

        val targetHours = profile.monthlyOtTargetHours
        val progress = if (targetHours > 0) (totalOtHours / targetHours).coerceIn(0.0, 1.0).toFloat() else 0f
        val remainingTarget = (targetHours - totalOtHours).coerceAtLeast(0.0)

        MonthlySummaryState(
            hourlyOtRate = otRate,
            totalOtHours = totalOtHours,
            totalOtAmount = totalOtAmount,
            nightBillCount = nightBillCount,
            nightBillTotal = nightBillTotal,
            tiffinBillCount = tiffinBillCount,
            tiffinBillTotal = tiffinBillTotal,
            totalPieceAmount = totalPieceAmount,
            basicSalary = profile.basicSalary,
            allowances = allowances,
            grossSalary = grossSalary,
            attendanceBonus = attendanceBonus,
            totalEstimatedEarnings = totalEstimatedEarnings,
            targetHours = targetHours,
            targetProgress = progress,
            remainingTargetHours = remainingTarget,
            regularDutyDays = regularDutyDays,
            dayShiftDays = dayShiftDays,
            nightShiftDays = nightShiftDays,
            holidayDutyDays = holidayDutyDays,
            weeklyOffDays = weeklyOffDays,
            leaveDays = leaveDays,
            absentDays = absentDays
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlySummaryState()
    )

    fun setYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun previousMonth() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(_selectedYearMonth.value) ?: Date()
        cal.add(Calendar.MONTH, -1)
        _selectedYearMonth.value = sdf.format(cal.time)
    }

    fun nextMonth() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(_selectedYearMonth.value) ?: Date()
        cal.add(Calendar.MONTH, 1)
        _selectedYearMonth.value = sdf.format(cal.time)
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val current = userProfile.value
            val newLang = if (current.language == "bn") "en" else "bn"
            repository.saveProfile(current.copy(language = newLang))
        }
    }

    fun updateProfile(updated: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(updated)
        }
    }

    fun addOrUpdateOvertime(
        id: Long = 0,
        date: String,
        otHours: Double,
        hasNightBill: Boolean,
        hasTiffinBill: Boolean,
        isHoliday: Boolean,
        shiftType: String = "REGULAR",
        note: String
    ) {
        viewModelScope.launch {
            val ym = if (date.length >= 7) date.substring(0, 7) else _selectedYearMonth.value
            val profile = userProfile.value
            val rate = LaborLawCalculator.getHourlyOtRate(profile)
            val record = OvertimeRecord(
                id = id,
                date = date,
                yearMonth = ym,
                otHours = otHours,
                hasNightBill = hasNightBill,
                hasTiffinBill = hasTiffinBill,
                isHoliday = isHoliday,
                shiftType = shiftType,
                note = note,
                hourlyRateSnapshot = rate,
                calculatedAmount = otHours * rate
            )
            repository.insertOrUpdateOvertime(record)
        }
    }

    fun deleteOvertime(id: Long) {
        viewModelScope.launch {
            repository.deleteOvertime(id)
        }
    }

    fun addPieceRate(
        itemName: String,
        pieceCount: Int,
        ratePerPiece: Double,
        date: String,
        note: String = ""
    ) {
        viewModelScope.launch {
            val ym = if (date.length >= 7) date.substring(0, 7) else _selectedYearMonth.value
            val total = pieceCount * ratePerPiece
            val record = PieceRateRecord(
                date = date,
                yearMonth = ym,
                itemName = itemName,
                pieceCount = pieceCount,
                ratePerPiece = ratePerPiece,
                totalAmount = total,
                note = note
            )
            repository.insertPieceRate(record)
        }
    }

    fun deletePieceRate(id: Long) {
        viewModelScope.launch {
            repository.deletePieceRate(id)
        }
    }

    fun updateTargetHours(target: Double) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveProfile(current.copy(monthlyOtTargetHours = target))
        }
    }

    fun syncCloudNow(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.syncWithCloud()
            onComplete(res.first, res.second)
        }
    }

    val currentUser = com.example.data.auth.AuthManager.currentUser
    val isAuthLoading = com.example.data.auth.AuthManager.isLoading

    fun signInWithGoogle(activity: android.app.Activity, onError: (String) -> Unit) {
        com.example.data.auth.AuthManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = { user ->
                viewModelScope.launch {
                    // First restore all profile, overtime, and piece-rate data from Firebase
                    repository.fullAutoSyncAndRestore()

                    // Only update the user name with Google display name if the restored profile's name is still empty
                    val profileAfterSync = repository.getProfileOnce()
                    val newName = user.displayName
                    if (!newName.isNullOrBlank() && (profileAfterSync.userName.isBlank() || profileAfterSync.userName == "মোঃ শ্রমিক ভাই")) {
                        val updated = profileAfterSync.copy(userName = newName)
                        repository.saveProfile(updated)
                    }
                }
            },
            onError = onError
        )
    }

    fun signOut() {
        com.example.data.auth.AuthManager.signOut()
    }
}

data class MonthlySummaryState(
    val hourlyOtRate: Double = 0.0,
    val totalOtHours: Double = 0.0,
    val totalOtAmount: Double = 0.0,
    val nightBillCount: Int = 0,
    val nightBillTotal: Double = 0.0,
    val tiffinBillCount: Int = 0,
    val tiffinBillTotal: Double = 0.0,
    val totalPieceAmount: Double = 0.0,
    val basicSalary: Double = 0.0,
    val allowances: Double = 0.0,
    val grossSalary: Double = 0.0,
    val attendanceBonus: Double = 0.0,
    val totalEstimatedEarnings: Double = 0.0,
    val targetHours: Double = 0.0,
    val targetProgress: Float = 0f,
    val remainingTargetHours: Double = 0.0,
    val regularDutyDays: Int = 0,
    val dayShiftDays: Int = 0,
    val nightShiftDays: Int = 0,
    val holidayDutyDays: Int = 0,
    val weeklyOffDays: Int = 0,
    val leaveDays: Int = 0,
    val absentDays: Int = 0
)
