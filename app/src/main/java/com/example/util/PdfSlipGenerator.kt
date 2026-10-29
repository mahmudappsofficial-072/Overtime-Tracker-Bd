package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.MonthlySummaryState
import java.io.File
import java.io.FileOutputStream

object PdfSlipGenerator {

    fun generateAndDownloadPdf(
        context: Context,
        profile: UserProfile,
        summary: MonthlySummaryState,
        selectedMonth: String,
        isBn: Boolean
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Background card
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRect(24f, 24f, pageWidth - 24f, pageHeight - 24f, paint)

        // Header Background
        paint.color = Color.rgb(5, 150, 105) // Brand Primary
        canvas.drawRect(24f, 24f, pageWidth - 24f, 110f, paint)

        // Company Name
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val company = if (profile.companyName.isNotBlank()) profile.companyName else "Garments & Factory Ltd."
        canvas.drawText(company, 44f, 60f, paint)

        // Title
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (isBn) "মাসিক বেতন ও ওভারটাইম স্লিপ • $selectedMonth" else "Monthly Salary & Overtime Pay Slip • $selectedMonth", 44f, 85f, paint)

        // Brand Logo on top-right of PDF Header
        try {
            val logoBmp = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.ic_app_logo)
            if (logoBmp != null) {
                val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logoBmp, 60, 60, true)
                canvas.drawBitmap(scaledLogo, pageWidth - 96f, 37f, paint)
            }
        } catch (e: Exception) {
            // Ignore logo draw error in PDF
        }

        var y = 140f

        // Employee Meta Box
        paint.color = Color.rgb(255, 255, 255)
        canvas.drawRect(44f, y - 16f, pageWidth - 44f, y + 60f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (isBn) "কর্মী নাম: ${profile.userName}" else "Worker Name: ${profile.userName}", 56f, y + 8f, paint)
        val formattedGrade = formatDisplayGrade(profile.grade, isBn)
        val designationLine = if (formattedGrade.isNotBlank()) {
            if (isBn) "পদবি: ${profile.designation} ($formattedGrade)" else "Designation: ${profile.designation} ($formattedGrade)"
        } else {
            if (isBn) "পদবি: ${profile.designation}" else "Designation: ${profile.designation}"
        }
        canvas.drawText(designationLine, 320f, y + 8f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (isBn) "কার্ড / আইডি নং: ${profile.workerId}" else "Card / ID No: ${profile.workerId}", 56f, y + 36f, paint)
        val totalDays = summary.regularDutyDays + summary.dayShiftDays + summary.nightShiftDays
        canvas.drawText(if (isBn) "মোট ডিউটি দিন: $totalDays দিন | নাইট: ${summary.nightShiftDays} দিন" else "Total Duty: $totalDays days | Night: ${summary.nightShiftDays} days", 320f, y + 36f, paint)

        y += 90f

        // Section: Attendance Summary
        paint.color = Color.rgb(5, 150, 105)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (isBn) "হাজিরা ও শিফট বিবরণ:" else "Attendance & Shift Summary:", 44f, y, paint)
        y += 20f

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val weeklyAndHolidays = summary.weeklyOffDays + summary.holidayDutyDays
        canvas.drawText(if (isBn) "সাপ্তাহিক ও সরকারি ছুটি: $weeklyAndHolidays দিন" else "Weekly Off & Holiday: $weeklyAndHolidays days", 56f, y, paint)
        canvas.drawText(if (isBn) "সাময়িক ছুটি: ${summary.leaveDays} দিন" else "Leave: ${summary.leaveDays} days", 320f, y, paint)
        y += 18f

        val bonusStatusText = if (summary.attendanceBonus > 0) {
            if (isBn) "(হাজিরা বোনাস অর্জিত)" else "(Attendance Bonus Earned)"
        } else if (summary.absentDays > 0) {
            if (isBn) "(হাজিরা বোনাস কর্তন)" else "(Attendance Bonus Deducted)"
        } else {
            ""
        }
        canvas.drawText(if (isBn) "অনুপস্থিত: ${summary.absentDays} দিন $bonusStatusText" else "Absent: ${summary.absentDays} days $bonusStatusText", 56f, y, paint)
        y += 20f

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(44f, y, pageWidth - 44f, y, paint)
        y += 24f

        // Section: Basic & Allowances
        paint.color = Color.rgb(5, 150, 105)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (isBn) "মূল বেতন ও নির্ধারিত ভাতাসমূহ:" else "Basic Salary & Fixed Allowances:", 44f, y, paint)
        y += 20f

        fun drawPdfRow(label: String, amount: Double, isBold: Boolean = false, isDeduction: Boolean = false) {
            paint.color = if (isDeduction) Color.rgb(220, 38, 38) else if (isBold) Color.rgb(15, 23, 42) else Color.rgb(71, 85, 105)
            paint.textSize = 11f
            paint.typeface = if (isBold || isDeduction) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(label, 56f, y, paint)
            val valStr = if (isDeduction) String.format("- Tk %.2f", Math.abs(amount)) else String.format("Tk %.2f", amount)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(valStr, pageWidth - 56f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            y += 18f
        }

        val autoAllowances = if (!profile.isCustomSalary && summary.basicSalary > 0.0) {
            LaborLawCalculator.calculateAutoAllowances(summary.basicSalary)
        } else null

        val effHouseRent = if (profile.isCustomSalary) profile.houseRent else (autoAllowances?.houseRent ?: profile.houseRent)
        val effMedical = if (profile.isCustomSalary) profile.medicalAllowance else (autoAllowances?.medicalAllowance ?: profile.medicalAllowance)
        val effConveyance = if (profile.isCustomSalary) profile.conveyanceAllowance else (autoAllowances?.conveyanceAllowance ?: profile.conveyanceAllowance)
        val effFood = if (profile.isCustomSalary) profile.foodAllowance else (autoAllowances?.foodAllowance ?: profile.foodAllowance)

        drawPdfRow(if (isBn) "মূল বেতন" else "Basic Salary", summary.basicSalary)
        drawPdfRow(if (isBn) "বাড়িভাড়া ভাতা (৫০%)" else "House Rent Allowance (50%)", effHouseRent)
        drawPdfRow(if (isBn) "চিকিৎসা ভাতা" else "Medical Allowance", effMedical)
        drawPdfRow(if (isBn) "যাতায়াত ভাতা" else "Conveyance Allowance", effConveyance)
        drawPdfRow(if (isBn) "খাদ্য ভাতা" else "Food Allowance", effFood)
        val attendanceLabel = if (summary.attendanceBonus > 0) {
            if (summary.absentDays > 0) {
                if (isBn) "হাজিরা বোনাস (${summary.absentDays} দিন অনুপস্থিতি অনুমোদিত)" else "Attendance Bonus (${summary.absentDays} absent days allowed)"
            } else {
                if (isBn) "হাজিরা বোনাস (নিয়মিত উপস্থিতি)" else "Attendance Bonus (Regular Attendance)"
            }
        } else if (summary.absentDays > 0) {
            if (isBn) "হাজিরা বোনাস (${summary.absentDays} দিন অনুপস্থিতির জন্য বাতিল)" else "Attendance Bonus (Cancelled for ${summary.absentDays} absent days)"
        } else {
            if (isBn) "হাজিরা বোনাস" else "Attendance Bonus"
        }
        drawPdfRow(
            attendanceLabel,
            summary.attendanceBonus
        )

        y += 6f
        canvas.drawLine(44f, y, pageWidth - 44f, y, paint)
        y += 20f

        // Section: Overtime & Extra
        paint.color = Color.rgb(5, 150, 105)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (isBn) "অতিরিক্ত আয়:" else "Overtime & Extra Earnings:", 44f, y, paint)
        y += 20f

        drawPdfRow(if (isBn) "ওভারটাইম আয় (${String.format("%.1f", summary.totalOtHours)} ঘণ্টা @ Tk ${String.format("%.2f", summary.hourlyOtRate)}/ঘণ্টা)" else "Overtime Income (${String.format("%.1f", summary.totalOtHours)} hrs @ Tk ${String.format("%.2f", summary.hourlyOtRate)}/hr)", summary.totalOtAmount, isBold = true)
        if (summary.nightBillTotal > 0) {
            drawPdfRow(if (isBn) "নাইট বিল (${summary.nightBillCount}টি @ Tk ${String.format("%.0f", profile.nightBillRate)})" else "Night Bill (${summary.nightBillCount} nights @ Tk ${String.format("%.0f", profile.nightBillRate)})", summary.nightBillTotal)
        }
        if (summary.tiffinBillTotal > 0) {
            drawPdfRow(if (isBn) "টিফিন বিল (${summary.tiffinBillCount}টি @ Tk ${String.format("%.0f", profile.tiffinBillRate)})" else "Tiffin Bill (${summary.tiffinBillCount} days @ Tk ${String.format("%.0f", profile.tiffinBillRate)})", summary.tiffinBillTotal)
        }
        drawPdfRow(if (isBn) "পিস রেট আয়" else "Piece Rate Earnings", summary.totalPieceAmount, isBold = summary.totalPieceAmount > 0)

        // Section: Deductions (if any deduction exists)
        val hasDeductions = summary.absentDeduction > 0 || summary.advanceDeduction > 0 || summary.aponBazarDeduction > 0
        if (hasDeductions) {
            y += 6f
            canvas.drawLine(44f, y, pageWidth - 44f, y, paint)
            y += 20f

            paint.color = Color.rgb(220, 38, 38)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(if (isBn) "কর্তন:" else "Deductions:", 44f, y, paint)
            y += 20f

            if (summary.absentDeduction > 0) {
                drawPdfRow(
                    if (isBn) "অনুপস্থিতি কর্তন (${summary.absentDays} দিন @ Tk ${String.format("%.2f", summary.dailyAbsentRate)}/দিন)"
                    else "Absent Deduction (${summary.absentDays} days @ Tk ${String.format("%.2f", summary.dailyAbsentRate)}/day)",
                    summary.absentDeduction,
                    isDeduction = true
                )
            }
            if (summary.advanceDeduction > 0) {
                drawPdfRow(
                    if (isBn) "অগ্রিম কর্তন" else "Advance Deduction",
                    summary.advanceDeduction,
                    isDeduction = true
                )
            }
            if (summary.aponBazarDeduction > 0) {
                drawPdfRow(
                    if (isBn) "আপন বাজার কর্তন" else "Apon Bazar Deduction",
                    summary.aponBazarDeduction,
                    isDeduction = true
                )
            }
        }

        y += 10f

        // Net Payable Box
        paint.color = Color.rgb(236, 253, 245)
        canvas.drawRect(44f, y, pageWidth - 44f, y + 46f, paint)
        paint.color = Color.rgb(5, 150, 105)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(44f, y, pageWidth - 44f, y + 46f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(5, 150, 105)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (isBn) "মোট প্রদেয় বেতন:" else "Net Payable Salary:", 56f, y + 28f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 16f
        canvas.drawText(String.format("Tk %.2f", summary.totalEstimatedEarnings), pageWidth - 56f, y + 28f, paint)
        paint.textAlign = Paint.Align.LEFT

        y += 70f

        // Signatures area
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawLine(56f, y + 40f, 180f, y + 40f, paint)
        canvas.drawText(if (isBn) "শ্রমিকের স্বাক্ষর" else "Worker's Signature", 76f, y + 54f, paint)

        canvas.drawLine(pageWidth - 180f, y + 40f, pageWidth - 56f, y + 40f, paint)
        canvas.drawText(if (isBn) "অ্যাকাউন্টস / কারখানা কর্তৃপক্ষ" else "Accounts / Factory Authority", pageWidth - 170f, y + 54f, paint)

        // Footer note
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isBn) "ওভারটাইম ট্র্যাকার বিডি • বাংলাদেশ শ্রম আইন অনুযায়ী প্রস্তুতকৃত" else "Generated via Overtime Tracker BD • Prepared under Bangladesh Labor Act", pageWidth / 2f, pageHeight - 36f, paint)

        pdfDocument.finishPage(page)

        // Save to Phone Downloads
        val cleanWorkerId = if (profile.workerId.isNotBlank()) profile.workerId.replace("/", "_") else "Worker"
        val fileName = "Salary_Slip_${cleanWorkerId}_${selectedMonth}.pdf"
        var savedUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        pdfDocument.writeTo(outStream)
                    }
                    savedUri = uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outStream ->
                    pdfDocument.writeTo(outStream)
                }
                savedUri = Uri.fromFile(file)
            }

            Toast.makeText(context, if (isBn) "পিডিএফ ডাউনলোড হয়েছে: $fileName" else "PDF downloaded: $fileName", Toast.LENGTH_LONG).show()

            // Open intent
            savedUri?.let { uri ->
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(openIntent, if (isBn) "PDF ওপেন করুন" else "Open PDF")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(chooser)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Toast.makeText(context, if (isBn) "পিডিএফ সংরক্ষণে সমস্যা: ${e.localizedMessage}" else "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }

        return savedUri
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
}
