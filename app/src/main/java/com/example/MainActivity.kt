package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.OvertimeRecord
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OvertimeViewModel
import com.example.util.LanguageManager

class MainActivity : ComponentActivity() {

    private val openQuickOtTrigger = androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra("OPEN_QUICK_OT", false) == true) {
            openQuickOtTrigger.value = true
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OvertimeApp(
                    openQuickOtTrigger = openQuickOtTrigger.value,
                    onResetQuickOtTrigger = { openQuickOtTrigger.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_QUICK_OT", false)) {
            openQuickOtTrigger.value = true
        }
    }
}

@Composable
fun OvertimeApp(
    viewModel: OvertimeViewModel = viewModel(),
    openQuickOtTrigger: Boolean = false,
    onResetQuickOtTrigger: () -> Unit = {}
) {
    var currentTab by remember { mutableIntStateOf(0) } // 0: Home, 1: OT List, 2: Piece & Slip, 3: Profile

    // Back handling: Return to Home if on secondary tab
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    val selectedMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val otRecords by viewModel.monthlyOtRecords.collectAsStateWithLifecycle()
    val pieceRecords by viewModel.monthlyPieceRecords.collectAsStateWithLifecycle()
    val summary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    val lang = profile.language

    // Notification permission launcher for Android 13+ (API 33+)
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            com.example.receiver.ReminderScheduler.scheduleDailyReminders(context)
        }
    }

    LaunchedEffect(Unit) {
        com.example.receiver.ReminderScheduler.scheduleDailyReminders(context)
        try {
            com.example.receiver.MyFirebaseMessagingService.createFcmChannel(context)
        } catch (_: Exception) {}

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Dialog States
    var showQuickOtDialog by remember { mutableStateOf(false) }
    var hasCheckedMissingOtThisSession by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showMissingOtDialog by remember { mutableStateOf(false) }
    var missingOtDates by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(openQuickOtTrigger) {
        if (openQuickOtTrigger) {
            currentTab = 0
            showQuickOtDialog = true
            hasCheckedMissingOtThisSession = true
            onResetQuickOtTrigger()
        }
    }

    LaunchedEffect(otRecords, openQuickOtTrigger) {
        if (!hasCheckedMissingOtThisSession && !openQuickOtTrigger) {
            kotlinx.coroutines.delay(350)
            val missing = MissingOtDetector.getMissingDatesForCurrentMonth(otRecords)
            if (missing.isNotEmpty()) {
                missingOtDates = missing
                showMissingOtDialog = true
            }
            hasCheckedMissingOtThisSession = true
        }
    }
    var recordToEdit by remember { mutableStateOf<OvertimeRecord?>(null) }
    var showPieceDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showSignInConsentDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                // Tab 0: Home
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = LanguageManager.get("tab_home", lang)
                        )
                    },
                    label = {
                        Text(
                            text = LanguageManager.get("tab_home", lang),
                            fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_home_item")
                )

                // Tab 1: OT List
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            if (currentTab == 1) Icons.Filled.AccessTimeFilled else Icons.Outlined.AccessTime,
                            contentDescription = LanguageManager.get("tab_ot_list", lang)
                        )
                    },
                    label = {
                        Text(
                            text = LanguageManager.get("tab_ot_list", lang),
                            fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_ot_list_item")
                )

                // Tab 2: Piece Rate & Slip
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            if (currentTab == 2) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = LanguageManager.get("tab_piece_rate", lang)
                        )
                    },
                    label = {
                        Text(
                            text = LanguageManager.get("tab_piece_rate", lang),
                            fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_piece_item")
                )

                // Tab 3: Profile & Settings
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = {
                        Icon(
                            if (currentTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = LanguageManager.get("tab_profile", lang)
                        )
                    },
                    label = {
                        Text(
                            text = LanguageManager.get("tab_profile", lang),
                            fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_profile_item")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    selectedMonth = selectedMonth,
                    summary = summary,
                    profile = profile,
                    recentRecords = otRecords,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onQuickAddOtClick = {
                        recordToEdit = null
                        showQuickOtDialog = true
                    },
                    onEditTargetClick = { showTargetDialog = true },
                    onRecordClick = { rec ->
                        recordToEdit = rec
                        showQuickOtDialog = true
                    },
                    onViewAllClick = { currentTab = 1 }
                )

                1 -> OtListScreen(
                    selectedMonth = selectedMonth,
                    records = otRecords,
                    hourlyRate = summary.hourlyOtRate,
                    dailyAbsentRate = summary.dailyAbsentRate,
                    attendanceBonus = summary.attendanceBonus,
                    lang = lang,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onAddClick = {
                        recordToEdit = null
                        showQuickOtDialog = true
                    },
                    onEditClick = { rec ->
                        recordToEdit = rec
                        showQuickOtDialog = true
                    },
                    onDeleteClick = { id ->
                        otRecords.find { it.id == id }?.let { rec ->
                            viewModel.moveOtToTrash(rec)
                        } ?: viewModel.deleteOvertime(id)
                    }
                )

                2 -> PieceRateScreen(
                    selectedMonth = selectedMonth,
                    pieceRecords = pieceRecords,
                    profile = profile,
                    summary = summary,
                    lang = lang,
                    onAddPieceClick = { showPieceDialog = true },
                    onDeletePieceClick = { id ->
                        pieceRecords.find { it.id == id }?.let { rec ->
                            viewModel.movePieceToTrash(rec)
                        } ?: viewModel.deletePieceRate(id)
                    }
                )

                3 -> ProfileScreen(
                    profile = profile,
                    lang = lang,
                    currentUser = currentUser,
                    isAuthLoading = isAuthLoading,
                    onSignInWithGoogle = {
                        activity?.let { act ->
                            if (!profile.hasAcceptedPrivacyPolicy) {
                                showSignInConsentDialog = true
                            } else {
                                viewModel.signInWithGoogle(act) { err ->
                                    android.widget.Toast.makeText(act, err, android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    onSignOut = { viewModel.signOut() },
                    onSaveProfile = { updated -> viewModel.updateProfile(updated) },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onSyncCloud = { callback -> viewModel.syncCloudNow(callback) },
                    trashItems = trashItems,
                    onRestoreTrash = { viewModel.restoreTrashItem(it) },
                    onDeleteTrashPermanently = { viewModel.deleteTrashItemPermanently(it) },
                    onClearAllTrash = { viewModel.clearAllTrash() },
                    onDeleteAllData = {
                        viewModel.deleteAllData { success ->
                            val msg = if (success) {
                                if (lang == "bn") "সকল ডেটা সফলভাবে মুছে ফেলা হয়েছে" else "All data deleted successfully"
                            } else {
                                if (lang == "bn") "ডেটা মুছতে সমস্যা হয়েছে" else "Failed to delete data"
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }

    // Quick/Edit OT Dialog
    if (showQuickOtDialog) {
        QuickOtEntryDialog(
            initialRecord = recordToEdit,
            existingRecords = otRecords,
            hourlyRate = summary.hourlyOtRate,
            profile = profile,
            lang = lang,
            onDismiss = {
                showQuickOtDialog = false
                recordToEdit = null
            },
            onNavigateToProfile = {
                showQuickOtDialog = false
                recordToEdit = null
                currentTab = 3
            },
            onSave = { date, hours, nightBill, tiffinBill, isHoliday, shiftType, note ->
                viewModel.addOrUpdateOvertime(
                    id = recordToEdit?.id ?: 0,
                    date = date,
                    otHours = hours,
                    hasNightBill = nightBill,
                    hasTiffinBill = tiffinBill,
                    isHoliday = isHoliday,
                    shiftType = shiftType,
                    note = note
                )
                showQuickOtDialog = false
                recordToEdit = null
            }
        )
    }

    // Add Piece Work Dialog
    if (showPieceDialog) {
        AddPieceRateDialog(
            lang = lang,
            onDismiss = { showPieceDialog = false },
            onSave = { item, count, rate, date, note ->
                viewModel.addPieceRate(
                    itemName = item,
                    pieceCount = count,
                    ratePerPiece = rate,
                    date = date,
                    note = note
                )
                showPieceDialog = false
            }
        )
    }

    // Monthly Target Dialog
    if (showTargetDialog) {
        TargetGoalDialog(
            currentTarget = profile.monthlyOtTargetHours,
            lang = lang,
            onDismiss = { showTargetDialog = false },
            onSave = { newTarget ->
                viewModel.updateTargetHours(newTarget)
                showTargetDialog = false
            }
        )
    }

    // Missing Overtime Entry Reminder Dialog
    if (showMissingOtDialog && !showQuickOtDialog) {
        MissingOtReminderDialog(
            missingDates = missingOtDates,
            lang = lang,
            onDismiss = { showMissingOtDialog = false },
            onAddOtForDate = { targetDate ->
                showMissingOtDialog = false
                recordToEdit = OvertimeRecord(
                    date = targetDate,
                    yearMonth = if (targetDate.length >= 7) targetDate.substring(0, 7) else "",
                    otHours = 0.0
                )
                showQuickOtDialog = true
            }
        )
    }

    // Google Sign-In Privacy Consent Dialog (shown only when user clicks Google Sign-In and has not consented yet)
    if (showSignInConsentDialog) {
        InitialPrivacyConsentDialog(
            lang = lang,
            titleText = if (lang == "bn") "গোপনীয়তা নীতিমালায় সম্মতি" else "Privacy Policy Consent",
            confirmButtonText = if (lang == "bn") "সম্মত আছি ও সাইন ইন" else "Agree & Sign In",
            dismissButtonText = if (lang == "bn") "বাতিল / অফলাইনে ব্যবহার" else "Cancel / Use Offline",
            onAgree = {
                showSignInConsentDialog = false
                viewModel.acceptPrivacyPolicy()
                activity?.let { act ->
                    viewModel.signInWithGoogle(act) { err ->
                        android.widget.Toast.makeText(act, err, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDecline = {
                showSignInConsentDialog = false
            },
            onViewFullPolicy = {
                val privacyUrl = "https://mahmudappsofficial-072.github.io/overtime-tracker-privacy-policy/"
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(privacyUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback handled safely
                }
            }
        )
    }
}
