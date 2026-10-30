package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.OvertimeRecord
import com.example.data.model.PieceRateRecord
import com.example.data.model.TrashItem
import com.example.data.model.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resumeWith(Result.success(result)) }
        addOnFailureListener { exception -> cont.resumeWith(Result.failure(exception)) }
        addOnCanceledListener { cont.cancel() }
    }

class OvertimeRepository(private val database: AppDatabase, private val context: android.content.Context? = null) {

    private val overtimeDao = database.overtimeDao()
    private val pieceRateDao = database.pieceRateDao()
    private val userProfileDao = database.userProfileDao()
    private val monthlySalaryDao = database.monthlySalaryDao()
    private val trashDao = database.trashDao()
    private val deductionDao = database.deductionDao()

    val allTrashItems: Flow<List<TrashItem>> = trashDao.getAllTrashItems()

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val syncScope = CoroutineScope(Dispatchers.IO)

    init {
        syncScope.launch {
            try {
                overtimeDao.cleanupDuplicateDates()
                purgeExpiredTrash()
                val current = userProfileDao.getUserProfileOnce()
                if (current != null) {
                    val isDummyProfile = current.userName == "মোঃ শ্রমিক ভাই" ||
                                          current.companyName == "আমান টেক্সটাইল লিমিটেড" ||
                                          current.workerId == "EMP-8492"

                    if (isDummyProfile) {
                        val cleaned = current.copy(
                            userName = "",
                            companyName = "",
                            designation = "",
                            workerId = "",
                            grade = "",
                            basicSalary = 0.0,
                            houseRent = 0.0,
                            medicalAllowance = 750.0,
                            conveyanceAllowance = 0.0,
                            foodAllowance = 0.0,
                            attendanceBonus = 0.0,
                            nightBillRate = 0.0,
                            tiffinBillRate = 0.0,
                            monthlyOtTargetHours = 0.0,
                            supportWhatsapp = ""
                        )
                        userProfileDao.insertOrUpdateProfile(cleaned)
                    } else if (current.medicalAllowance == 1250.0) {
                        userProfileDao.insertOrUpdateProfile(current.copy(medicalAllowance = 750.0))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()

    suspend fun getProfileOnce(): UserProfile {
        return userProfileDao.getUserProfileOnce() ?: UserProfile()
    }

    suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile)
        // Automatic real-time background sync to Firebase
        syncScope.launch {
            autoSyncProfileToCloud(profile)
        }
    }

    fun getMonthlySalarySettings(yearMonth: String): Flow<com.example.data.model.MonthlySalarySettings?> {
        return monthlySalaryDao.getSettingsForMonth(yearMonth)
    }

    suspend fun getMonthlySalarySettingsOnce(yearMonth: String): com.example.data.model.MonthlySalarySettings? {
        return monthlySalaryDao.getSettingsForMonthOnce(yearMonth)
    }

    suspend fun saveMonthlySalarySettings(settings: com.example.data.model.MonthlySalarySettings) {
        monthlySalaryDao.insertOrUpdate(settings)
        syncScope.launch {
            autoSyncMonthlySalaryToCloud(settings)
        }
    }

    // Deductions (Advance & Apon Bazar)
    fun getDeductionsForMonth(yearMonth: String): Flow<List<com.example.data.model.DeductionRecord>> {
        return deductionDao.getDeductionsForMonth(yearMonth)
    }

    suspend fun saveDeduction(record: com.example.data.model.DeductionRecord): Long {
        val id = deductionDao.insertOrUpdate(record)
        val saved = record.copy(id = id)
        syncScope.launch {
            autoSyncDeductionToCloud(saved)
        }
        return id
    }

    suspend fun deleteDeduction(id: Long) {
        deductionDao.deleteById(id)
        syncScope.launch {
            autoDeleteDeductionFromCloud(id)
        }
    }

    // Overtime
    fun getOvertimeRecords(yearMonth: String): Flow<List<OvertimeRecord>> {
        return overtimeDao.getRecordsByMonth(yearMonth)
    }

    fun getAllOvertimeRecords(): Flow<List<OvertimeRecord>> {
        return overtimeDao.getAllRecords()
    }

    suspend fun insertOrUpdateOvertime(record: OvertimeRecord): Long {
        val existing = overtimeDao.getRecordByDate(record.date)
        val targetId = if (record.id != 0L) {
            record.id
        } else if (existing != null) {
            existing.id
        } else {
            0L
        }
        val recordToSave = record.copy(id = targetId)
        val savedId = overtimeDao.insertRecord(recordToSave)
        val finalRecord = recordToSave.copy(id = savedId)

        // Automatic real-time background sync to Firebase
        syncScope.launch {
            autoSyncOvertimeToCloud(finalRecord)
        }
        return savedId
    }

    suspend fun getOvertimeByDate(date: String): OvertimeRecord? {
        return overtimeDao.getRecordByDate(date)
    }

    suspend fun deleteOvertime(id: Long) {
        val existing = overtimeDao.getRecordById(id) ?: return
        overtimeDao.deleteRecordById(id)
        // Direct reliable deletion from Firebase Cloud
        try {
            autoDeleteOvertimeFromCloud(existing.date)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Error deleting OT from cloud: ${e.message}")
        }
    }

    // Piece Rate
    fun getPieceRateRecords(yearMonth: String): Flow<List<PieceRateRecord>> {
        return pieceRateDao.getPieceRecordsByMonth(yearMonth)
    }

    suspend fun insertPieceRate(record: PieceRateRecord): Long {
        val savedId = pieceRateDao.insertPieceRecord(record)
        val finalRecord = record.copy(id = savedId)
        // Automatic real-time background sync to Firebase
        syncScope.launch {
            autoSyncPieceRateToCloud(finalRecord)
        }
        return savedId
    }

    suspend fun deletePieceRate(id: Long) {
        pieceRateDao.deletePieceRecordById(id)
        // Automatic real-time background deletion from Firebase
        syncScope.launch {
            autoDeletePieceRateFromCloud(id)
        }
    }

    suspend fun cleanupDuplicateDates() {
        try {
            overtimeDao.cleanupDuplicateDates()
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Error cleaning duplicate dates: ${e.message}")
        }
    }

    suspend fun clearAllLocalData() {
        try {
            overtimeDao.deleteAllRecords()
            pieceRateDao.deleteAllPieceRecords()
            monthlySalaryDao.deleteAll()
            deductionDao.deleteAll()
            trashDao.clearAllTrash()
            scheduleTrashAutoDelete()

            val current = userProfileDao.getUserProfileOnce()
            val cleanProfile = UserProfile(
                id = 1,
                userName = "",
                companyName = "",
                designation = "",
                workerId = "",
                grade = "",
                isCustomSalary = false,
                basicSalary = 0.0,
                houseRent = 0.0,
                medicalAllowance = 0.0,
                conveyanceAllowance = 0.0,
                foodAllowance = 0.0,
                attendanceBonus = 0.0,
                maxAbsentForBonus = 0,
                maxLeaveForBonus = null,
                nightBillRate = 0.0,
                tiffinBillRate = 0.0,
                customOtRate = 0.0,
                monthlyOtTargetHours = 0.0,
                language = current?.language ?: "bn",
                supportWhatsapp = "",
                hasAcceptedPrivacyPolicy = false,
                privacyPolicyAcceptedAt = 0L,
                lastCloudSyncTime = 0L,
                cloudSyncStatus = if ((current?.language ?: "bn") == "bn") "লগইন নেই (লোকাল)" else "Not logged in (Local)"
            )
            userProfileDao.insertOrUpdateProfile(cleanProfile)
        } catch (e: Exception) {
            Log.e("OvertimeRepository", "Error clearing local data", e)
        }
    }

    suspend fun checkUserConsentInCloud(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val snap = firestore.collection("users").document(uid).get().awaitResult()
            if (snap.exists()) {
                val data = snap.data
                val hasConsent = data?.get("hasAcceptedPrivacyPolicy") as? Boolean ?: false
                val consentAudit = data?.get("consentAudit") as? Map<*, *>
                val auditConsent = consentAudit?.get("isConsentGiven") as? Boolean ?: false
                hasConsent || auditConsent
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Error checking user consent in cloud: ${e.message}")
            false
        }
    }

    suspend fun saveConsentToCloud(uid: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        try {
            val consentMap = hashMapOf<String, Any>(
                "hasAcceptedPrivacyPolicy" to true,
                "privacyPolicyAcceptedAt" to now,
                "consentAudit" to hashMapOf<String, Any>(
                    "isConsentGiven" to true,
                    "consentTimestamp" to now,
                    "policyVersion" to "1.0.10",
                    "consentDeclaration" to "User agreed to Terms and Privacy Policy before storing salary and overtime data in Firestore."
                )
            )
            firestore.collection("users").document(uid).set(consentMap, SetOptions.merge()).awaitResult()
            markConsentGivenLocally(now)
            Log.d("OvertimeRepository", "Privacy consent recorded successfully in Firestore for uid: $uid")
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Error saving consent to cloud: ${e.message}")
            markConsentGivenLocally(now)
        }
    }

    suspend fun markConsentGivenLocally(timestamp: Long = System.currentTimeMillis()) {
        val current = userProfileDao.getUserProfileOnce() ?: UserProfile()
        userProfileDao.insertOrUpdateProfile(
            current.copy(
                hasAcceptedPrivacyPolicy = true,
                privacyPolicyAcceptedAt = timestamp
            )
        )
    }

    suspend fun deleteAllUserData(isCloudSignedIn: Boolean) = withContext(Dispatchers.IO) {
        if (isCloudSignedIn) {
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                val userDocRef = firestore.collection("users").document(uid)

                // STEP 1 (PROFILE WIPE WITH CONSENT PRESERVED IN CLOUD):
                // Reset and wipe all personal records (factory name, title, id, grade, salary, bonus, bills, etc.)
                // BUT preserve the Privacy Policy Consent record in Firestore so the user is never asked again.
                try {
                    val currentLocal = userProfileDao.getUserProfileOnce()
                    val consentTime = if ((currentLocal?.privacyPolicyAcceptedAt ?: 0L) > 0L) {
                        currentLocal!!.privacyPolicyAcceptedAt
                    } else {
                        System.currentTimeMillis()
                    }

                    val emptyProfileWithConsent = hashMapOf<String, Any>(
                        "userName" to "",
                        "companyName" to "",
                        "designation" to "",
                        "workerId" to "",
                        "grade" to "",
                        "isCustomSalary" to false,
                        "basicSalary" to 0.0,
                        "houseRent" to 0.0,
                        "medicalAllowance" to 0.0,
                        "conveyanceAllowance" to 0.0,
                        "foodAllowance" to 0.0,
                        "attendanceBonus" to 0.0,
                        "maxAbsentForBonus" to 0,
                        "nightBillRate" to 0.0,
                        "tiffinBillRate" to 0.0,
                        "customOtRate" to 0.0,
                        "monthlyOtTargetHours" to 0.0,
                        "language" to "bn",
                        "supportWhatsapp" to "",
                        "lastSyncTime" to 0L,
                        // CONSENT IS PERMANENTLY PRESERVED IN FIRESTORE PER USER REQUIREMENT:
                        "hasAcceptedPrivacyPolicy" to true,
                        "privacyPolicyAcceptedAt" to consentTime,
                        "consentAudit" to hashMapOf<String, Any>(
                            "isConsentGiven" to true,
                            "consentTimestamp" to consentTime,
                            "policyVersion" to "1.0.10",
                            "consentDeclaration" to "User agreed to Terms and Privacy Policy before storing salary and overtime data in Firestore."
                        )
                    )
                    userDocRef.set(emptyProfileWithConsent).awaitResult()
                    Log.d("OvertimeRepository", "Root profile wiped while preserving consent in Firestore")
                } catch (e: Exception) {
                    Log.w("OvertimeRepository", "Failed wiping profile fields in root user doc: ${e.message}")
                }

                // STEP 2: Delete subcollections using batched writes for maximum reliability
                // 2a. overtime_records
                try {
                    val otSnap = userDocRef.collection("overtime_records").get().awaitResult()
                    if (!otSnap.isEmpty) {
                        val batch = firestore.batch()
                        var count = 0
                        for (doc in otSnap.documents) {
                            batch.delete(doc.reference)
                            count++
                            if (count % 450 == 0) {
                                batch.commit().awaitResult()
                            }
                        }
                        batch.commit().awaitResult()
                    }
                } catch (e: Exception) {
                    Log.w("OvertimeRepository", "Failed deleting cloud overtime_records: ${e.message}")
                }

                // 2b. piece_rate_records
                try {
                    val prSnap = userDocRef.collection("piece_rate_records").get().awaitResult()
                    if (!prSnap.isEmpty) {
                        val batch = firestore.batch()
                        var count = 0
                        for (doc in prSnap.documents) {
                            batch.delete(doc.reference)
                            count++
                            if (count % 450 == 0) {
                                batch.commit().awaitResult()
                            }
                        }
                        batch.commit().awaitResult()
                    }
                } catch (e: Exception) {
                    Log.w("OvertimeRepository", "Failed deleting cloud piece_rate_records: ${e.message}")
                }

                // 2c. monthly_salary_settings
                try {
                    val msSnap = userDocRef.collection("monthly_salary_settings").get().awaitResult()
                    if (!msSnap.isEmpty) {
                        val batch = firestore.batch()
                        var count = 0
                        for (doc in msSnap.documents) {
                            batch.delete(doc.reference)
                            count++
                            if (count % 450 == 0) {
                                batch.commit().awaitResult()
                            }
                        }
                        batch.commit().awaitResult()
                    }
                } catch (e: Exception) {
                    Log.w("OvertimeRepository", "Failed deleting cloud monthly_salary_settings: ${e.message}")
                }

                // 2d. deduction_records
                try {
                    val dedSnap = userDocRef.collection("deduction_records").get().awaitResult()
                    if (!dedSnap.isEmpty) {
                        val batch = firestore.batch()
                        var count = 0
                        for (doc in dedSnap.documents) {
                            batch.delete(doc.reference)
                            count++
                            if (count % 450 == 0) {
                                batch.commit().awaitResult()
                            }
                        }
                        batch.commit().awaitResult()
                    }
                } catch (e: Exception) {
                    Log.w("OvertimeRepository", "Failed deleting cloud deduction_records: ${e.message}")
                }

                // 2e. trash_records
                try {
                    val trSnap = userDocRef.collection("trash_records").get().awaitResult()
                    if (!trSnap.isEmpty) {
                        val batch = firestore.batch()
                        var count = 0
                        for (doc in trSnap.documents) {
                            batch.delete(doc.reference)
                            count++
                            if (count % 450 == 0) {
                                batch.commit().awaitResult()
                            }
                        }
                        batch.commit().awaitResult()
                    }
                } catch (e: Exception) {
                    Log.w("OvertimeRepository", "Failed deleting cloud trash_records: ${e.message}")
                }
            }
        }

        // Always clear all local data in Room database
        clearAllLocalData()
    }

    // --- Automatic Firebase Sync Engine ---

    private fun getDocId(profile: UserProfile? = null): String? {
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) return uid
        return null
    }

    private suspend fun autoSyncProfileToCloud(profile: UserProfile) {
        val docId = getDocId(profile) ?: return

        // Safety guard: If profile has never been synced (fresh install) and all critical fields are empty/zero,
        // do NOT overwrite existing cloud profile!
        val isDefaultProfile = profile.lastCloudSyncTime == 0L &&
                profile.basicSalary == 0.0 &&
                profile.userName.isBlank() &&
                profile.workerId.isBlank() &&
                profile.companyName.isBlank()
        if (isDefaultProfile) {
            Log.d("OvertimeRepository", "Skipping auto-push for default empty profile")
            return
        }

        try {
            val userMap = profileToMap(profile)
            firestore.collection("users").document(docId).set(userMap, SetOptions.merge()).awaitResult()
            val now = System.currentTimeMillis()
            userProfileDao.insertOrUpdateProfile(profile.copy(
                lastCloudSyncTime = now,
                cloudSyncStatus = "স্বয়ংক্রিয় ব্যাকআপ সক্রিয় (Synced)"
            ))
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto sync profile failed: ${e.message}")
        }
    }

    private suspend fun autoSyncOvertimeToCloud(record: OvertimeRecord) {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            val map = overtimeToMap(record)
            firestore.collection("users").document(docId)
                .collection("overtime_records").document(record.date)
                .set(map, SetOptions.merge()).awaitResult()
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto sync OT record failed: ${e.message}")
        }
    }

    private suspend fun autoDeleteOvertimeFromCloud(date: String) {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            firestore.collection("users").document(docId)
                .collection("overtime_records").document(date)
                .delete().awaitResult()
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto delete OT record failed: ${e.message}")
        }
    }

    private suspend fun autoSyncPieceRateToCloud(record: PieceRateRecord) {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            val map = pieceRateToMap(record)
            firestore.collection("users").document(docId)
                .collection("piece_rate_records").document(record.id.toString())
                .set(map, SetOptions.merge()).awaitResult()
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto sync piece rate failed: ${e.message}")
        }
    }

    private suspend fun autoDeletePieceRateFromCloud(id: Long) {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            firestore.collection("users").document(docId)
                .collection("piece_rate_records").document(id.toString())
                .delete().awaitResult()
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto delete piece rate failed: ${e.message}")
        }
    }

    private suspend fun markProfileSynced(currentProfile: UserProfile) {
        val now = System.currentTimeMillis()
        val latest = userProfileDao.getUserProfileOnce() ?: currentProfile
        userProfileDao.insertOrUpdateProfile(latest.copy(
            lastCloudSyncTime = now,
            cloudSyncStatus = "স্বয়ংক্রিয় ব্যাকআপ সক্রিয় (Synced)"
        ))
    }

    private suspend fun autoSyncMonthlySalaryToCloud(settings: com.example.data.model.MonthlySalarySettings) {
        val docId = getDocId() ?: return
        try {
            val map = monthlySalaryToMap(settings)
            firestore.collection("users").document(docId)
                .collection("monthly_salary_settings").document(settings.yearMonth)
                .set(map, SetOptions.merge()).awaitResult()
            Log.d("OvertimeRepository", "Auto sync monthly salary settings success: ${settings.yearMonth}")
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto sync monthly salary settings failed: ${e.message}")
        }
    }

    private suspend fun autoSyncDeductionToCloud(record: com.example.data.model.DeductionRecord) {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            val map = deductionToMap(record)
            firestore.collection("users").document(docId)
                .collection("deduction_records").document(record.id.toString())
                .set(map, SetOptions.merge()).awaitResult()
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto sync deduction failed: ${e.message}")
        }
    }

    private suspend fun autoDeleteDeductionFromCloud(id: Long) {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            firestore.collection("users").document(docId)
                .collection("deduction_records").document(id.toString())
                .delete().awaitResult()
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto delete deduction failed: ${e.message}")
        }
    }

    private suspend fun autoSyncTrashToCloud(item: TrashItem) {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            val map = trashToMap(item)
            firestore.collection("users").document(docId)
                .collection("trash_records").document(item.id.toString())
                .set(map, SetOptions.merge()).awaitResult()
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto sync trash failed: ${e.message}")
        }
    }

    private suspend fun autoDeleteTrashFromCloud(id: Long) {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            firestore.collection("users").document(docId)
                .collection("trash_records").document(id.toString())
                .delete().awaitResult()
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto delete trash failed: ${e.message}")
        }
    }

    private suspend fun autoClearAllTrashFromCloud() {
        val currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return
        try {
            val snap = firestore.collection("users").document(docId)
                .collection("trash_records").get().awaitResult()
            if (!snap.isEmpty) {
                val batch = firestore.batch()
                for (doc in snap.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().awaitResult()
            }
            markProfileSynced(currentProfile)
        } catch (e: Exception) {
            Log.w("OvertimeRepository", "Auto clear all trash failed: ${e.message}")
        }
    }

    private fun getDoubleFromField(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun getBooleanFromField(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.toBoolean()
            else -> false
        }
    }

    // Full 2-Way Sync & Restore Engine:
    // Pushes local records to cloud and restores cloud records into Room DB
    suspend fun fullAutoSyncAndRestore(): Pair<Boolean, String> {
        var currentProfile = getProfileOnce()
        val docId = getDocId(currentProfile) ?: return Pair(false, "লগইন করা নেই")
        val now = System.currentTimeMillis()

        return try {
            val userDocRef = firestore.collection("users").document(docId)

            // 1. Sync Profile directly from server to avoid stale cached profiles
            val cloudProfileSnap = try {
                userDocRef.get(Source.SERVER).awaitResult()
            } catch (_: Exception) {
                userDocRef.get().awaitResult()
            }
            if (cloudProfileSnap.exists()) {
                val data = cloudProfileSnap.data
                if (data != null) {
                    val cloudBasic = getDoubleFromField(data["basicSalary"])
                    val cloudName = (data["userName"] as? String)?.trim() ?: ""
                    val cloudCompany = (data["companyName"] as? String)?.trim() ?: ""
                    val cloudDesignation = (data["designation"] as? String)?.trim() ?: ""
                    val cloudWorkerId = (data["workerId"] as? String)?.trim() ?: ""
                    val cloudGrade = (data["grade"] as? String)?.trim() ?: ""
                    val cloudHouseRent = getDoubleFromField(data["houseRent"])
                    val cloudMedical = getDoubleFromField(data["medicalAllowance"])
                    val cloudConveyance = getDoubleFromField(data["conveyanceAllowance"])
                    val cloudFood = getDoubleFromField(data["foodAllowance"])
                    val cloudAttendanceBonus = getDoubleFromField(data["attendanceBonus"])
                    val cloudNightBill = getDoubleFromField(data["nightBillRate"])
                    val cloudTiffinBill = getDoubleFromField(data["tiffinBillRate"])
                    val cloudCustomOt = getDoubleFromField(data["customOtRate"])
                    val cloudTargetHours = getDoubleFromField(data["monthlyOtTargetHours"])
                    val cloudIsCustom = getBooleanFromField(data["isCustomSalary"])
                    val cloudMaxAbsent = ((data["maxAbsentForBonus"] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
                    val cloudMaxLeave = if (data.containsKey("maxLeaveForBonus")) {
                        val num = (data["maxLeaveForBonus"] as? Number)?.toInt()
                        if (num == null || num < 0) null else num
                    } else null
                    val cloudWhatsapp = (data["supportWhatsapp"] as? String)?.trim() ?: ""
                    val cloudLang = (data["language"] as? String)?.trim() ?: "bn"
                    val cloudSyncTime = (data["lastSyncTime"] as? Number)?.toLong() ?: 0L

                    // Check if cloud document contains meaningful user profile data
                    val cloudHasData = cloudBasic > 0.0 ||
                            (cloudName.isNotBlank() && cloudName != "মোঃ শ্রমিক ভাই") ||
                            cloudCompany.isNotBlank() ||
                            cloudWorkerId.isNotBlank() ||
                            cloudDesignation.isNotBlank() ||
                            cloudNightBill > 0.0 ||
                            cloudTiffinBill > 0.0 ||
                            cloudAttendanceBonus > 0.0 ||
                            cloudTargetHours > 0.0

                    // Check if local is fresh install or default unconfigured state
                    val localIsDefaultOrBlank = currentProfile.lastCloudSyncTime == 0L ||
                            (currentProfile.basicSalary == 0.0 && currentProfile.companyName.isBlank() && currentProfile.workerId.isBlank())

                    // Restore from cloud when local is fresh/empty and cloud has data, or when cloud data has equal/newer timestamp
                    val shouldRestore = cloudHasData && (localIsDefaultOrBlank || cloudSyncTime >= currentProfile.lastCloudSyncTime || currentProfile.basicSalary == 0.0)

                    if (shouldRestore) {
                        val restoredProfile = currentProfile.copy(
                            userName = if (cloudName.isNotBlank()) cloudName else currentProfile.userName,
                            companyName = if (cloudCompany.isNotBlank()) cloudCompany else currentProfile.companyName,
                            designation = if (cloudDesignation.isNotBlank()) cloudDesignation else currentProfile.designation,
                            workerId = if (cloudWorkerId.isNotBlank()) cloudWorkerId else currentProfile.workerId,
                            grade = if (cloudGrade.isNotBlank()) cloudGrade else currentProfile.grade,
                            isCustomSalary = cloudIsCustom,
                            basicSalary = if (cloudBasic > 0.0) cloudBasic else currentProfile.basicSalary,
                            houseRent = if (cloudHouseRent > 0.0) cloudHouseRent else currentProfile.houseRent,
                            medicalAllowance = 750.0, // Fixed 750 as per BD RMG standard
                            conveyanceAllowance = if (cloudConveyance > 0.0) cloudConveyance else currentProfile.conveyanceAllowance,
                            foodAllowance = if (cloudFood > 0.0) cloudFood else currentProfile.foodAllowance,
                            attendanceBonus = if (cloudAttendanceBonus > 0.0) cloudAttendanceBonus else currentProfile.attendanceBonus,
                            maxAbsentForBonus = if (data.containsKey("maxAbsentForBonus")) cloudMaxAbsent else currentProfile.maxAbsentForBonus,
                            maxLeaveForBonus = if (data.containsKey("maxLeaveForBonus")) cloudMaxLeave else currentProfile.maxLeaveForBonus,
                            nightBillRate = if (cloudNightBill > 0.0) cloudNightBill else currentProfile.nightBillRate,
                            tiffinBillRate = if (cloudTiffinBill > 0.0) cloudTiffinBill else currentProfile.tiffinBillRate,
                            customOtRate = if (cloudCustomOt > 0.0) cloudCustomOt else currentProfile.customOtRate,
                            monthlyOtTargetHours = if (cloudTargetHours > 0.0) cloudTargetHours else currentProfile.monthlyOtTargetHours,
                            language = if (cloudLang.isNotBlank()) cloudLang else currentProfile.language,
                            supportWhatsapp = if (cloudWhatsapp.isNotBlank()) cloudWhatsapp else currentProfile.supportWhatsapp,
                            lastCloudSyncTime = if (cloudSyncTime > 0L) cloudSyncTime else now,
                            cloudSyncStatus = "স্বয়ংক্রিয় ব্যাকআপ সক্রিয় (Synced)"
                        )
                        userProfileDao.insertOrUpdateProfile(restoredProfile)
                        currentProfile = restoredProfile // CRITICAL: Update working reference so it is NOT overwritten!
                    } else if (!localIsDefaultOrBlank) {
                        // Local has real data that is newer than cloud; upload local profile
                        val toUpload = currentProfile.copy(
                            lastCloudSyncTime = now,
                            cloudSyncStatus = "স্বয়ংক্রিয় ব্যাকআপ সক্রিয় (Synced)"
                        )
                        userDocRef.set(profileToMap(toUpload), SetOptions.merge()).awaitResult()
                        currentProfile = toUpload
                        userProfileDao.insertOrUpdateProfile(currentProfile)
                    }
                }
            } else {
                // Cloud document does not exist yet; upload local profile if it has real user-entered data
                val localHasData = currentProfile.basicSalary > 0.0 ||
                        currentProfile.userName.isNotBlank() ||
                        currentProfile.companyName.isNotBlank() ||
                        currentProfile.workerId.isNotBlank()
                if (localHasData) {
                    val toUpload = currentProfile.copy(
                        lastCloudSyncTime = now,
                        cloudSyncStatus = "স্বয়ংক্রিয় ব্যাকআপ সক্রিয় (Synced)"
                    )
                    userDocRef.set(profileToMap(toUpload), SetOptions.merge()).awaitResult()
                    currentProfile = toUpload
                    userProfileDao.insertOrUpdateProfile(currentProfile)
                }
            }

            // 2. Upload local Overtime records to cloud
            val localOts = overtimeDao.getAllRecordsList()
            for (ot in localOts) {
                userDocRef.collection("overtime_records").document(ot.date)
                    .set(overtimeToMap(ot), SetOptions.merge()).awaitResult()
            }

            // 3. Download / Restore Overtime records from cloud
            val otQuerySnap = userDocRef.collection("overtime_records").get().awaitResult()
            for (doc in otQuerySnap.documents) {
                val data = doc.data ?: continue
                val remoteOt = mapToOvertime(data) ?: continue
                val existingLocal = overtimeDao.getRecordByDate(remoteOt.date)
                if (existingLocal == null) {
                    overtimeDao.insertRecord(remoteOt.copy(id = 0L))
                } else {
                    // Update existing record maintaining same local ID to avoid duplicates
                    overtimeDao.insertRecord(remoteOt.copy(id = existingLocal.id))
                }
            }
            // Auto cleanup duplicate dates if any previously existed
            overtimeDao.cleanupDuplicateDates()

            // 4. Upload local Piece Rate records to cloud
            val localPieces = pieceRateDao.getAllPieceRecordsList()
            for (pr in localPieces) {
                userDocRef.collection("piece_rate_records").document(pr.id.toString())
                    .set(pieceRateToMap(pr), SetOptions.merge()).awaitResult()
            }

            // 5. Download / Restore Piece Rate records from cloud
            val prQuerySnap = userDocRef.collection("piece_rate_records").get().awaitResult()
            for (doc in prQuerySnap.documents) {
                val data = doc.data ?: continue
                val remotePr = mapToPieceRate(data) ?: continue
                val existingLocal = pieceRateDao.getPieceRecordById(remotePr.id)
                if (existingLocal == null) {
                    pieceRateDao.insertPieceRecord(remotePr)
                }
            }

            // 6. Upload local Monthly Salary Settings to cloud
            val localMonthlySettings = monthlySalaryDao.getAllSettings()
            for (ms in localMonthlySettings) {
                userDocRef.collection("monthly_salary_settings").document(ms.yearMonth)
                    .set(monthlySalaryToMap(ms), SetOptions.merge()).awaitResult()
            }

            // 7. Download / Restore Monthly Salary Settings from cloud
            val msQuerySnap = userDocRef.collection("monthly_salary_settings").get().awaitResult()
            for (doc in msQuerySnap.documents) {
                val data = doc.data ?: continue
                val remoteMs = mapToMonthlySalary(data) ?: continue
                monthlySalaryDao.insertOrUpdate(remoteMs)
            }

            // 8. Upload local Deductions to cloud
            val localDeductions = deductionDao.getAllDeductions()
            for (ded in localDeductions) {
                userDocRef.collection("deduction_records").document(ded.id.toString())
                    .set(deductionToMap(ded), SetOptions.merge()).awaitResult()
            }

            // 9. Download / Restore Deductions from cloud
            val dedQuerySnap = userDocRef.collection("deduction_records").get().awaitResult()
            for (doc in dedQuerySnap.documents) {
                val data = doc.data ?: continue
                val remoteDed = mapToDeduction(data) ?: continue
                val targetId = if (remoteDed.id > 0L) remoteDed.id else (doc.id.toLongOrNull() ?: 0L)
                deductionDao.insertOrUpdate(remoteDed.copy(id = targetId))
            }

            // 10. Upload local Trash to cloud
            val localTrash = trashDao.getAllTrashItemsOnce()
            for (tr in localTrash) {
                userDocRef.collection("trash_records").document(tr.id.toString())
                    .set(trashToMap(tr), SetOptions.merge()).awaitResult()
            }

            // 11. Download / Restore Trash from cloud with 24-hour expiration filter
            val trashQuerySnap = userDocRef.collection("trash_records").get().awaitResult()
            val cutoff = now - com.example.receiver.TrashAutoDeleteScheduler.TWENTY_FOUR_HOURS_MILLIS
            val trashPurgeBatch = firestore.batch()
            var anyExpiredTrash = false

            for (doc in trashQuerySnap.documents) {
                val data = doc.data ?: continue
                val remoteTrash = mapToTrash(data) ?: continue
                if (remoteTrash.deletedAt <= cutoff) {
                    // Older than 24 hours: permanently delete from Cloud Firestore!
                    trashPurgeBatch.delete(doc.reference)
                    anyExpiredTrash = true
                } else {
                    // Still within 24 hours: restore to local Room database!
                    val targetId = if (remoteTrash.id > 0L) remoteTrash.id else (doc.id.toLongOrNull() ?: 0L)
                    trashDao.insertTrashItem(remoteTrash.copy(id = targetId))
                }
            }
            if (anyExpiredTrash) {
                try {
                    trashPurgeBatch.commit().awaitResult()
                } catch (e: Exception) {
                    Log.w("OvertimeRepository", "Purge expired trash batch failed: ${e.message}")
                }
            }
            scheduleTrashAutoDelete()

            // 8. Ensure profile in Room DB has latest sync timestamp and status using currentProfile
            val finalProfile = currentProfile.copy(
                lastCloudSyncTime = now,
                cloudSyncStatus = "স্বয়ংক্রিয় ব্যাকআপ সক্রিয় (Synced)"
            )
            userProfileDao.insertOrUpdateProfile(finalProfile)
            Pair(true, "স্বয়ংক্রিয় ক্লাউড সিঙ্ক সফল হয়েছে!")
        } catch (e: Exception) {
            Log.e("OvertimeRepository", "Full sync error", e)
            Pair(false, "ক্লাউড সংযোগ ত্রুটি: ${e.message}")
        }
    }

    // Called by ViewModel if needed
    suspend fun syncWithCloud(): Pair<Boolean, String> {
        return fullAutoSyncAndRestore()
    }

    private fun profileToMap(profile: UserProfile): Map<String, Any> {
        val map = hashMapOf<String, Any>(
            "userName" to profile.userName,
            "companyName" to profile.companyName,
            "designation" to profile.designation,
            "workerId" to profile.workerId,
            "grade" to profile.grade,
            "isCustomSalary" to profile.isCustomSalary,
            "basicSalary" to profile.basicSalary,
            "houseRent" to profile.houseRent,
            "medicalAllowance" to profile.medicalAllowance,
            "conveyanceAllowance" to profile.conveyanceAllowance,
            "foodAllowance" to profile.foodAllowance,
            "attendanceBonus" to profile.attendanceBonus,
            "maxAbsentForBonus" to profile.maxAbsentForBonus,
            "maxLeaveForBonus" to (profile.maxLeaveForBonus ?: -1),
            "nightBillRate" to profile.nightBillRate,
            "tiffinBillRate" to profile.tiffinBillRate,
            "customOtRate" to profile.customOtRate,
            "monthlyOtTargetHours" to profile.monthlyOtTargetHours,
            "language" to profile.language,
            "supportWhatsapp" to profile.supportWhatsapp,
            "hasAcceptedPrivacyPolicy" to profile.hasAcceptedPrivacyPolicy,
            "privacyPolicyAcceptedAt" to profile.privacyPolicyAcceptedAt,
            "consentAudit" to hashMapOf<String, Any>(
                "isConsentGiven" to profile.hasAcceptedPrivacyPolicy,
                "consentTimestamp" to (if (profile.privacyPolicyAcceptedAt > 0L) profile.privacyPolicyAcceptedAt else System.currentTimeMillis()),
                "policyVersion" to "1.0.10",
                "consentDeclaration" to "User agreed to Terms and Privacy Policy before storing salary and overtime data in Firestore."
            ),
            "lastSyncTime" to (if (profile.lastCloudSyncTime > 0) profile.lastCloudSyncTime else System.currentTimeMillis())
        )
        return map
    }

    private fun overtimeToMap(record: OvertimeRecord): Map<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to record.id,
            "date" to record.date,
            "yearMonth" to record.yearMonth,
            "otHours" to record.otHours,
            "hasNightBill" to record.hasNightBill,
            "hasTiffinBill" to record.hasTiffinBill,
            "isHoliday" to record.isHoliday,
            "shiftType" to record.shiftType,
            "note" to record.note,
            "hourlyRateSnapshot" to record.hourlyRateSnapshot,
            "calculatedAmount" to record.calculatedAmount,
            "createdAt" to record.createdAt,
            "updatedAt" to System.currentTimeMillis()
        )
        return map
    }

    private fun mapToOvertime(map: Map<String, Any?>): OvertimeRecord? {
        val date = map["date"] as? String ?: return null
        val yearMonth = map["yearMonth"] as? String ?: if (date.length >= 7) date.substring(0, 7) else ""
        return OvertimeRecord(
            id = (map["id"] as? Number)?.toLong() ?: 0L,
            date = date,
            yearMonth = yearMonth,
            otHours = (map["otHours"] as? Number)?.toDouble() ?: 0.0,
            hasNightBill = map["hasNightBill"] as? Boolean ?: false,
            hasTiffinBill = map["hasTiffinBill"] as? Boolean ?: false,
            isHoliday = map["isHoliday"] as? Boolean ?: false,
            shiftType = map["shiftType"] as? String ?: "REGULAR",
            note = map["note"] as? String ?: "",
            hourlyRateSnapshot = (map["hourlyRateSnapshot"] as? Number)?.toDouble() ?: 0.0,
            calculatedAmount = (map["calculatedAmount"] as? Number)?.toDouble() ?: 0.0,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun pieceRateToMap(record: PieceRateRecord): Map<String, Any> {
        val map = hashMapOf<String, Any>(
            "id" to record.id,
            "date" to record.date,
            "yearMonth" to record.yearMonth,
            "itemName" to record.itemName,
            "pieceCount" to record.pieceCount,
            "ratePerPiece" to record.ratePerPiece,
            "totalAmount" to record.totalAmount,
            "note" to record.note,
            "createdAt" to record.createdAt,
            "updatedAt" to System.currentTimeMillis()
        )
        return map
    }

    private fun mapToPieceRate(map: Map<String, Any?>): PieceRateRecord? {
        val date = map["date"] as? String ?: return null
        val yearMonth = map["yearMonth"] as? String ?: if (date.length >= 7) date.substring(0, 7) else ""
        val itemName = map["itemName"] as? String ?: ""
        return PieceRateRecord(
            id = (map["id"] as? Number)?.toLong() ?: 0L,
            date = date,
            yearMonth = yearMonth,
            itemName = itemName,
            pieceCount = (map["pieceCount"] as? Number)?.toInt() ?: 0,
            ratePerPiece = (map["ratePerPiece"] as? Number)?.toDouble() ?: 0.0,
            totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            note = map["note"] as? String ?: "",
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun monthlySalaryToMap(settings: com.example.data.model.MonthlySalarySettings): Map<String, Any> {
        return hashMapOf(
            "yearMonth" to settings.yearMonth,
            "basicSalary" to settings.basicSalary,
            "houseRent" to settings.houseRent,
            "medicalAllowance" to settings.medicalAllowance,
            "conveyanceAllowance" to settings.conveyanceAllowance,
            "foodAllowance" to settings.foodAllowance,
            "grossSalary" to settings.grossSalary,
            "hourlyOtRate" to settings.hourlyOtRate,
            "attendanceBonus" to settings.attendanceBonus,
            "maxAbsentForBonus" to settings.maxAbsentForBonus,
            "maxLeaveForBonus" to (settings.maxLeaveForBonus ?: -1),
            "nightBillRate" to settings.nightBillRate,
            "tiffinBillRate" to settings.tiffinBillRate,
            "isCustomSalary" to settings.isCustomSalary,
            "updatedAt" to settings.updatedAt
        )
    }

    private fun mapToMonthlySalary(map: Map<String, Any?>): com.example.data.model.MonthlySalarySettings? {
        val yearMonth = map["yearMonth"] as? String ?: return null
        return com.example.data.model.MonthlySalarySettings(
            yearMonth = yearMonth,
            basicSalary = getDoubleFromField(map["basicSalary"]),
            houseRent = getDoubleFromField(map["houseRent"]),
            medicalAllowance = getDoubleFromField(map["medicalAllowance"]),
            conveyanceAllowance = getDoubleFromField(map["conveyanceAllowance"]),
            foodAllowance = getDoubleFromField(map["foodAllowance"]),
            grossSalary = getDoubleFromField(map["grossSalary"]),
            hourlyOtRate = getDoubleFromField(map["hourlyOtRate"]),
            attendanceBonus = getDoubleFromField(map["attendanceBonus"]),
            maxAbsentForBonus = ((map["maxAbsentForBonus"] as? Number)?.toInt() ?: 1).coerceAtLeast(0),
            maxLeaveForBonus = if (map.containsKey("maxLeaveForBonus")) {
                val num = (map["maxLeaveForBonus"] as? Number)?.toInt()
                if (num == null || num < 0) null else num
            } else null,
            nightBillRate = getDoubleFromField(map["nightBillRate"]),
            tiffinBillRate = getDoubleFromField(map["tiffinBillRate"]),
            isCustomSalary = getBooleanFromField(map["isCustomSalary"]),
            updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun deductionToMap(record: com.example.data.model.DeductionRecord): Map<String, Any> {
        return hashMapOf(
            "id" to record.id,
            "type" to record.type,
            "date" to record.date,
            "yearMonth" to record.yearMonth,
            "amount" to record.amount,
            "description" to record.description,
            "createdAt" to record.createdAt
        )
    }

    private fun mapToDeduction(map: Map<String, Any?>): com.example.data.model.DeductionRecord? {
        val type = map["type"] as? String ?: return null
        val date = map["date"] as? String ?: return null
        val ym = map["yearMonth"] as? String ?: (if (date.length >= 7) date.substring(0, 7) else return null)
        val amount = getDoubleFromField(map["amount"])
        val desc = map["description"] as? String ?: ""
        val id = (map["id"] as? Number)?.toLong() ?: 0L
        val createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        return com.example.data.model.DeductionRecord(
            id = id,
            type = type,
            date = date,
            yearMonth = ym,
            amount = amount,
            description = desc,
            createdAt = createdAt
        )
    }

    private fun trashToMap(item: TrashItem): Map<String, Any> {
        val expireAt = item.deletedAt + com.example.receiver.TrashAutoDeleteScheduler.TWENTY_FOUR_HOURS_MILLIS
        return hashMapOf(
            "id" to item.id,
            "itemType" to item.itemType,
            "originalId" to item.originalId,
            "title" to item.title,
            "subtitle" to item.subtitle,
            "date" to item.date,
            "jsonData" to item.jsonData,
            "deletedAt" to item.deletedAt,
            "expireAt" to expireAt
        )
    }

    private fun mapToTrash(map: Map<String, Any?>): TrashItem? {
        val itemType = map["itemType"] as? String ?: return null
        val title = map["title"] as? String ?: ""
        val subtitle = map["subtitle"] as? String ?: ""
        val date = map["date"] as? String ?: ""
        val jsonData = map["jsonData"] as? String ?: return null
        val id = (map["id"] as? Number)?.toLong() ?: 0L
        val originalId = (map["originalId"] as? Number)?.toLong() ?: 0L
        val deletedAt = (map["deletedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        return TrashItem(
            id = id,
            itemType = itemType,
            originalId = originalId,
            title = title,
            subtitle = subtitle,
            date = date,
            jsonData = jsonData,
            deletedAt = deletedAt
        )
    }

    private fun scheduleTrashAutoDelete() {
        try {
            val ctx = context ?: try { com.example.OvertimeApp.instance } catch (_: Exception) { null }
            ctx?.let {
                com.example.receiver.TrashAutoDeleteScheduler.scheduleNextAutoDelete(it)
            }
        } catch (_: Exception) {}
    }

    suspend fun moveOvertimeToTrash(record: OvertimeRecord) = withContext(Dispatchers.IO) {
        val trash = TrashItem.fromOvertimeRecord(record)
        val id = trashDao.insertTrashItem(trash)
        val savedTrash = trash.copy(id = id)
        syncScope.launch { autoSyncTrashToCloud(savedTrash) }
        deleteOvertime(record.id)
        scheduleTrashAutoDelete()
    }

    suspend fun movePieceRateToTrash(record: PieceRateRecord) = withContext(Dispatchers.IO) {
        val trash = TrashItem.fromPieceRateRecord(record)
        val id = trashDao.insertTrashItem(trash)
        val savedTrash = trash.copy(id = id)
        syncScope.launch { autoSyncTrashToCloud(savedTrash) }
        deletePieceRate(record.id)
        scheduleTrashAutoDelete()
    }

    suspend fun moveDeductionToTrash(record: com.example.data.model.DeductionRecord) = withContext(Dispatchers.IO) {
        val trash = TrashItem.fromDeductionRecord(record)
        val id = trashDao.insertTrashItem(trash)
        val savedTrash = trash.copy(id = id)
        syncScope.launch { autoSyncTrashToCloud(savedTrash) }
        deleteDeduction(record.id)
        scheduleTrashAutoDelete()
    }

    suspend fun restoreTrashItem(item: TrashItem) = withContext(Dispatchers.IO) {
        when (item.itemType) {
            "OT" -> {
                val record = item.toOvertimeRecord()
                if (record != null) {
                    insertOrUpdateOvertime(record)
                }
            }
            "PIECE" -> {
                val record = item.toPieceRateRecord()
                if (record != null) {
                    insertPieceRate(record)
                }
            }
            "ADVANCE", "APON_BAZAR", "DEDUCTION" -> {
                val record = item.toDeductionRecord()
                if (record != null) {
                    saveDeduction(record)
                }
            }
        }
        trashDao.deleteTrashItem(item)
        syncScope.launch { autoDeleteTrashFromCloud(item.id) }
        scheduleTrashAutoDelete()
    }

    suspend fun deleteTrashItemPermanently(item: TrashItem) = withContext(Dispatchers.IO) {
        trashDao.deleteTrashItem(item)
        syncScope.launch { autoDeleteTrashFromCloud(item.id) }
        scheduleTrashAutoDelete()
    }

    suspend fun clearAllTrash() = withContext(Dispatchers.IO) {
        trashDao.clearAllTrash()
        syncScope.launch { autoClearAllTrashFromCloud() }
        scheduleTrashAutoDelete()
    }

    suspend fun purgeExpiredTrash(): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - com.example.receiver.TrashAutoDeleteScheduler.TWENTY_FOUR_HOURS_MILLIS
        val purgedCount = trashDao.purgeExpiredTrash(cutoff)
        val docId = getDocId()
        if (docId != null) {
            try {
                val trSnap = firestore.collection("users").document(docId)
                    .collection("trash_records").get().awaitResult()
                if (!trSnap.isEmpty) {
                    val batch = firestore.batch()
                    var anyExpired = false
                    for (doc in trSnap.documents) {
                        val deletedAt = (doc.data?.get("deletedAt") as? Number)?.toLong() ?: 0L
                        if (deletedAt > 0L && deletedAt <= cutoff) {
                            batch.delete(doc.reference)
                            anyExpired = true
                        }
                    }
                    if (anyExpired) {
                        batch.commit().awaitResult()
                    }
                }
            } catch (e: Exception) {
                Log.w("OvertimeRepository", "Purge expired trash from cloud failed: ${e.message}")
            }
        }
        scheduleTrashAutoDelete()
        purgedCount
    }
}
