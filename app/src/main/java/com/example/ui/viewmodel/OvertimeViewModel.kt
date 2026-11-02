package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.OvertimeRecord
import com.example.data.model.PieceRateRecord
import com.example.data.model.TrashItem
import com.example.data.model.UserProfile
import com.example.data.repository.OvertimeRepository
import com.example.util.LaborLawCalculator
import com.google.firebase.auth.FirebaseUser
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
    private val _pendingConsentUser = MutableStateFlow<FirebaseUser?>(null)
    val pendingConsentUser: StateFlow<FirebaseUser?> = _pendingConsentUser.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = OvertimeRepository(db, application)
        viewModelScope.launch {
            repository.cleanupDuplicateDates()
            repository.purgeExpiredTrash()
            com.example.data.auth.AuthManager.currentUser.collect { user ->
                if (user != null) {
                    val alreadyConsented = repository.checkUserConsentInCloud(user.uid)
                    if (alreadyConsented) {
                        repository.fullAutoSyncAndRestore()
                    }
                }
            }
        }
    }

    val trashItems: StateFlow<List<TrashItem>> = repository.allTrashItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonthSettings: StateFlow<com.example.data.model.MonthlySalarySettings?> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getMonthlySalarySettings(ym) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyDeductions: StateFlow<List<com.example.data.model.DeductionRecord>> = _selectedYearMonth
        .flatMapLatest { ym -> repository.getDeductionsForMonth(ym) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Summary calculation state - Combines userProfile, month snapshot settings, OT, piece, and deduction records
    val monthlySummary = combine(
        userProfile,
        currentMonthSettings,
        monthlyOtRecords,
        monthlyPieceRecords,
        monthlyDeductions
    ) { profile, monthSnapshot, otList, pieceList, deductionList ->
        val currentCalendarYm = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val isRunningMonth = _selectedYearMonth.value >= currentCalendarYm

        // For the running month, prioritize active profile so edits are live immediately.
        // For past months, prioritize saved monthSnapshot to keep historical months locked.
        val isCustom = if (isRunningMonth) profile.isCustomSalary else (monthSnapshot?.isCustomSalary ?: profile.isCustomSalary)
        val baseBasic = if (isRunningMonth) {
            profile.basicSalary
        } else {
            if (monthSnapshot != null && monthSnapshot.basicSalary > 0.0) monthSnapshot.basicSalary else profile.basicSalary
        }
        val otRate = if (isRunningMonth) {
            LaborLawCalculator.getHourlyOtRate(profile)
        } else {
            if (monthSnapshot != null && monthSnapshot.hourlyOtRate > 0.0) {
                monthSnapshot.hourlyOtRate
            } else {
                LaborLawCalculator.getHourlyOtRate(profile)
            }
        }

        var totalOtHours = 0.0
        var totalOtAmount = 0.0
        var nightBillCount = 0
        var tiffinBillCount = 0

        for (rec in otList) {
            totalOtHours += rec.otHours
            // If running month, calculate with the active running otRate.
            // If past month, respect stored record snapshot if available, otherwise effective past month rate
            val itemRate = if (isRunningMonth) otRate else (if (rec.hourlyRateSnapshot > 0.0) rec.hourlyRateSnapshot else otRate)
            totalOtAmount += rec.otHours * itemRate
            if (rec.hasNightBill) nightBillCount++
            if (rec.hasTiffinBill) tiffinBillCount++
        }

        val effectiveNightRate = if (isRunningMonth) {
            profile.nightBillRate
        } else {
            if (monthSnapshot != null && monthSnapshot.nightBillRate > 0.0) monthSnapshot.nightBillRate else profile.nightBillRate
        }
        val effectiveTiffinRate = if (isRunningMonth) {
            profile.tiffinBillRate
        } else {
            if (monthSnapshot != null && monthSnapshot.tiffinBillRate > 0.0) monthSnapshot.tiffinBillRate else profile.tiffinBillRate
        }

        val nightBillTotal = nightBillCount * effectiveNightRate
        val tiffinBillTotal = tiffinBillCount * effectiveTiffinRate

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

        val effHouseRent: Double
        val effMedical: Double
        val effConveyance: Double
        val effFood: Double

        val allowances = if (baseBasic <= 0.0) {
            effHouseRent = 0.0
            effMedical = 0.0
            effConveyance = 0.0
            effFood = 0.0
            0.0
        } else if (isCustom) {
            effHouseRent = if (isRunningMonth) profile.houseRent else (if (monthSnapshot != null && monthSnapshot.houseRent > 0.0) monthSnapshot.houseRent else profile.houseRent)
            effMedical = if (isRunningMonth) profile.medicalAllowance else (if (monthSnapshot != null && monthSnapshot.medicalAllowance > 0.0) monthSnapshot.medicalAllowance else profile.medicalAllowance)
            effConveyance = if (isRunningMonth) profile.conveyanceAllowance else (if (monthSnapshot != null && monthSnapshot.conveyanceAllowance > 0.0) monthSnapshot.conveyanceAllowance else profile.conveyanceAllowance)
            effFood = if (isRunningMonth) profile.foodAllowance else (if (monthSnapshot != null && monthSnapshot.foodAllowance > 0.0) monthSnapshot.foodAllowance else profile.foodAllowance)
            effHouseRent + effMedical + effConveyance + effFood
        } else {
            val auto = LaborLawCalculator.calculateAutoAllowances(baseBasic)
            effHouseRent = auto.houseRent
            effMedical = auto.medicalAllowance
            effConveyance = auto.conveyanceAllowance
            effFood = auto.foodAllowance
            effHouseRent + effMedical + effConveyance + effFood
        }

        val grossSalary = if (isRunningMonth) {
            if (baseBasic > 0.0) baseBasic + allowances else 0.0
        } else if (monthSnapshot != null && monthSnapshot.grossSalary > 0.0) {
            monthSnapshot.grossSalary
        } else if (baseBasic > 0.0) {
            baseBasic + allowances
        } else {
            0.0
        }

        val effUserName = if (isRunningMonth) profile.userName else (if (monthSnapshot != null && monthSnapshot.userName.isNotBlank()) monthSnapshot.userName else profile.userName)
        val effCompanyName = if (isRunningMonth) profile.companyName else (if (monthSnapshot != null && monthSnapshot.companyName.isNotBlank()) monthSnapshot.companyName else profile.companyName)
        val effDesignation = if (isRunningMonth) profile.designation else (if (monthSnapshot != null && monthSnapshot.designation.isNotBlank()) monthSnapshot.designation else profile.designation)
        val effWorkerId = if (isRunningMonth) profile.workerId else (if (monthSnapshot != null && monthSnapshot.workerId.isNotBlank()) monthSnapshot.workerId else profile.workerId)
        val effGrade = if (isRunningMonth) profile.grade else (if (monthSnapshot != null && monthSnapshot.grade.isNotBlank()) monthSnapshot.grade else profile.grade)

        val allowedAbsents = if (isRunningMonth) profile.maxAbsentForBonus else (if (monthSnapshot != null) monthSnapshot.maxAbsentForBonus else profile.maxAbsentForBonus)
        val allowedLeaves = if (isRunningMonth) profile.maxLeaveForBonus else (if (monthSnapshot != null) monthSnapshot.maxLeaveForBonus else profile.maxLeaveForBonus)
        val bonusRate = if (isRunningMonth) profile.attendanceBonus else (if (monthSnapshot != null && monthSnapshot.attendanceBonus > 0.0) monthSnapshot.attendanceBonus else profile.attendanceBonus)
        val isAbsentPenalty = absentDays > allowedAbsents
        val isLeavePenalty = allowedLeaves != null && leaveDays > allowedLeaves
        val isBonusCancelledDueToAbsent = (baseBasic > 0.0 && bonusRate > 0.0) && isAbsentPenalty
        val isBonusCancelledDueToLeave = (baseBasic > 0.0 && bonusRate > 0.0) && isLeavePenalty
        val attendanceBonus = if (baseBasic > 0.0 && !isAbsentPenalty && !isLeavePenalty) bonusRate else 0.0
        
        // Labor law absent deduction: calculated from month's basic salary (Basic / 30)
        val dailyAbsentRate = if (baseBasic > 0.0) baseBasic / 30.0 else 0.0
        val absentDeduction = if (dailyAbsentRate > 0.0 && absentDays > 0) {
            dailyAbsentRate * absentDays
        } else {
            0.0
        }

        val advanceDeduction = deductionList.filter { it.type == "ADVANCE" }.sumOf { it.amount }
        val aponBazarDeduction = deductionList.filter { it.type == "APON_BAZAR" }.sumOf { it.amount }

        val totalEstimatedEarnings = (grossSalary - absentDeduction).coerceAtLeast(0.0) +
            totalOtAmount + nightBillTotal + tiffinBillTotal + totalPieceAmount + attendanceBonus -
            advanceDeduction - aponBazarDeduction

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
            basicSalary = baseBasic,
            allowances = allowances,
            grossSalary = grossSalary,
            attendanceBonus = attendanceBonus,
            dailyAbsentRate = dailyAbsentRate,
            absentDeduction = absentDeduction,
            advanceDeduction = advanceDeduction,
            aponBazarDeduction = aponBazarDeduction,
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
            absentDays = absentDays,
            isBonusCancelledDueToLeave = isBonusCancelledDueToLeave,
            isBonusCancelledDueToAbsent = isBonusCancelledDueToAbsent,
            maxAllowedLeaveForBonus = allowedLeaves,
            effectiveNightRate = effectiveNightRate,
            effectiveTiffinRate = effectiveTiffinRate,
            userName = effUserName,
            companyName = effCompanyName,
            designation = effDesignation,
            workerId = effWorkerId,
            grade = effGrade,
            houseRent = effHouseRent,
            medicalAllowance = effMedical,
            conveyanceAllowance = effConveyance,
            foodAllowance = effFood
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlySummaryState()
    )

    fun setYearMonth(yearMonth: String) {
        _selectedYearMonth.value = yearMonth
    }

    fun addDeduction(type: String, date: String, amount: Double, description: String = "") {
        viewModelScope.launch {
            val ym = if (date.length >= 7) date.substring(0, 7) else _selectedYearMonth.value
            val record = com.example.data.model.DeductionRecord(
                type = type,
                date = date,
                yearMonth = ym,
                amount = amount,
                description = description.trim()
            )
            repository.saveDeduction(record)
        }
    }

    fun deleteDeduction(id: Long) {
        viewModelScope.launch {
            repository.deleteDeduction(id)
        }
    }

    fun moveDeductionToTrash(record: com.example.data.model.DeductionRecord) {
        viewModelScope.launch {
            repository.moveDeductionToTrash(record)
        }
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

    fun acceptPrivacyPolicy() {
        viewModelScope.launch {
            val current = userProfile.value
            val now = System.currentTimeMillis()
            val updated = current.copy(
                hasAcceptedPrivacyPolicy = true,
                privacyPolicyAcceptedAt = now
            )
            repository.saveProfile(updated)
        }
    }

    fun updateProfile(updated: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(updated)
            val currentCalendarYm = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            // Only update selected month if it is current or future; never overwrite past months
            val ym = if (_selectedYearMonth.value >= currentCalendarYm) _selectedYearMonth.value else currentCalendarYm
            val otRate = LaborLawCalculator.getHourlyOtRate(updated)
            val allowances = if (updated.isCustomSalary) {
                updated.houseRent + updated.medicalAllowance + updated.conveyanceAllowance + updated.foodAllowance
            } else {
                val auto = LaborLawCalculator.calculateAutoAllowances(updated.basicSalary)
                auto.houseRent + auto.medicalAllowance + auto.conveyanceAllowance + auto.foodAllowance
            }
            val gross = if (updated.basicSalary > 0.0) updated.basicSalary + allowances else 0.0
            val snapshot = com.example.data.model.MonthlySalarySettings(
                yearMonth = ym,
                basicSalary = updated.basicSalary,
                houseRent = updated.houseRent,
                medicalAllowance = updated.medicalAllowance,
                conveyanceAllowance = updated.conveyanceAllowance,
                foodAllowance = updated.foodAllowance,
                grossSalary = gross,
                hourlyOtRate = otRate,
                attendanceBonus = updated.attendanceBonus,
                maxAbsentForBonus = updated.maxAbsentForBonus,
                maxLeaveForBonus = updated.maxLeaveForBonus,
                nightBillRate = updated.nightBillRate,
                tiffinBillRate = updated.tiffinBillRate,
                isCustomSalary = updated.isCustomSalary,
                userName = updated.userName,
                companyName = updated.companyName,
                designation = updated.designation,
                workerId = updated.workerId,
                grade = updated.grade
            )
            repository.saveMonthlySalarySettings(snapshot)
            // Synchronize running month records with the new rate and amount in Room and Firestore
            repository.updateRunningMonthOvertimeRates(currentCalendarYm, otRate)
            if (ym > currentCalendarYm) {
                repository.updateRunningMonthOvertimeRates(ym, otRate)
            }
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
            val currentCalendarYm = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val isRunningMonth = ym >= currentCalendarYm
            val profile = userProfile.value
            
            // For running month, always use the current active profile rate.
            // For past month, prioritize that past month's saved snapshot rate.
            val existingMonthSettings = repository.getMonthlySalarySettingsOnce(ym)
            val effectiveRate = if (isRunningMonth) {
                LaborLawCalculator.getHourlyOtRate(profile)
            } else if (existingMonthSettings != null && existingMonthSettings.hourlyOtRate > 0.0) {
                existingMonthSettings.hourlyOtRate
            } else {
                LaborLawCalculator.getHourlyOtRate(profile)
            }

            // Ensure a snapshot for this month exists so future salary changes don't alter it
            if (existingMonthSettings == null && profile.basicSalary > 0.0) {
                val allowances = if (profile.isCustomSalary) {
                    profile.houseRent + profile.medicalAllowance + profile.conveyanceAllowance + profile.foodAllowance
                } else {
                    val auto = LaborLawCalculator.calculateAutoAllowances(profile.basicSalary)
                    auto.houseRent + auto.medicalAllowance + auto.conveyanceAllowance + auto.foodAllowance
                }
                val gross = profile.basicSalary + allowances
                val snapshot = com.example.data.model.MonthlySalarySettings(
                    yearMonth = ym,
                    basicSalary = profile.basicSalary,
                    houseRent = profile.houseRent,
                    medicalAllowance = profile.medicalAllowance,
                    conveyanceAllowance = profile.conveyanceAllowance,
                    foodAllowance = profile.foodAllowance,
                    grossSalary = gross,
                    hourlyOtRate = effectiveRate,
                    attendanceBonus = profile.attendanceBonus,
                    maxAbsentForBonus = profile.maxAbsentForBonus,
                    maxLeaveForBonus = profile.maxLeaveForBonus,
                    nightBillRate = profile.nightBillRate,
                    tiffinBillRate = profile.tiffinBillRate,
                    isCustomSalary = profile.isCustomSalary,
                    userName = profile.userName,
                    companyName = profile.companyName,
                    designation = profile.designation,
                    workerId = profile.workerId,
                    grade = profile.grade
                )
                repository.saveMonthlySalarySettings(snapshot)
            } else if (existingMonthSettings != null && existingMonthSettings.userName.isBlank() && profile.userName.isNotBlank()) {
                val backfilled = existingMonthSettings.copy(
                    userName = profile.userName,
                    companyName = profile.companyName,
                    designation = profile.designation,
                    workerId = profile.workerId,
                    grade = profile.grade
                )
                repository.saveMonthlySalarySettings(backfilled)
            }

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
                hourlyRateSnapshot = effectiveRate,
                calculatedAmount = otHours * effectiveRate
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

    fun moveOtToTrash(record: OvertimeRecord) {
        viewModelScope.launch {
            repository.moveOvertimeToTrash(record)
        }
    }

    fun movePieceToTrash(record: PieceRateRecord) {
        viewModelScope.launch {
            repository.movePieceRateToTrash(record)
        }
    }

    fun restoreTrashItem(item: TrashItem) {
        viewModelScope.launch {
            repository.restoreTrashItem(item)
        }
    }

    fun deleteTrashItemPermanently(item: TrashItem) {
        viewModelScope.launch {
            repository.deleteTrashItemPermanently(item)
        }
    }

    fun clearAllTrash() {
        viewModelScope.launch {
            repository.clearAllTrash()
        }
    }

    fun purgeExpiredTrash() {
        viewModelScope.launch {
            repository.purgeExpiredTrash()
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
                    val alreadyConsented = repository.checkUserConsentInCloud(user.uid)
                    if (alreadyConsented) {
                        repository.markConsentGivenLocally()
                        completePostSignIn(user)
                    } else {
                        _pendingConsentUser.value = user
                    }
                }
            },
            onError = onError
        )
    }

    private suspend fun completePostSignIn(user: FirebaseUser) {
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

    fun confirmPrivacyConsent(user: FirebaseUser) {
        viewModelScope.launch {
            repository.saveConsentToCloud(user.uid)
            _pendingConsentUser.value = null
            completePostSignIn(user)
        }
    }

    fun declinePrivacyConsent() {
        viewModelScope.launch {
            _pendingConsentUser.value = null
            com.example.data.auth.AuthManager.signOut(getApplication())
        }
    }

    fun signOut() {
        viewModelScope.launch {
            com.example.data.auth.AuthManager.signOut(getApplication())
            repository.clearAllLocalData()
        }
    }

    fun deleteAllData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val isCloud = currentUser.value != null
                val success = repository.deleteAllUserData(isCloud)
                if (isCloud && success) {
                    com.example.data.auth.AuthManager.signOut(getApplication())
                }
                val currentCalendarYm = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
                _selectedYearMonth.value = currentCalendarYm
                onComplete(success)
            } catch (e: Exception) {
                android.util.Log.e("OvertimeViewModel", "Failed to delete all data: ${e.message}", e)
                onComplete(false)
            }
        }
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
    val dailyAbsentRate: Double = 0.0,
    val absentDeduction: Double = 0.0,
    val advanceDeduction: Double = 0.0,
    val aponBazarDeduction: Double = 0.0,
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
    val absentDays: Int = 0,
    val isBonusCancelledDueToLeave: Boolean = false,
    val isBonusCancelledDueToAbsent: Boolean = false,
    val maxAllowedLeaveForBonus: Int? = null,
    val effectiveNightRate: Double = 0.0,
    val effectiveTiffinRate: Double = 0.0,
    val userName: String = "",
    val companyName: String = "",
    val designation: String = "",
    val workerId: String = "",
    val grade: String = "",
    val houseRent: Double = 0.0,
    val medicalAllowance: Double = 0.0,
    val conveyanceAllowance: Double = 0.0,
    val foodAllowance: Double = 0.0
)
