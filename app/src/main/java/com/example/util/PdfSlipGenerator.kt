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
        canvas.drawText("মাসিক বেতন ও ওভারটাইম স্লিপ (Salary & Overtime Pay Slip) • $selectedMonth", 44f, 85f, paint)

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
        canvas.drawText("কর্মী নাম: ${profile.userName}", 56f, y + 8f, paint)
        canvas.drawText("পদবি: ${profile.designation} (${profile.grade})", 320f, y + 8f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("কার্ড / আইডি নং: ${profile.workerId}", 56f, y + 36f, paint)
        val totalDays = summary.regularDutyDays + summary.dayShiftDays + summary.nightShiftDays + summary.holidayDutyDays
        canvas.drawText("মোট ডিউটি দিন: $totalDays দিন | নাইট: ${summary.nightShiftDays} দিন", 320f, y + 36f, paint)

        y += 90f

        // Section: Attendance Summary
        paint.color = Color.rgb(5, 150, 105)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("হাজিরা ও শিফট বিবরণ (Attendance):", 44f, y, paint)
        y += 20f

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("ছুটি / সাপ্তাহিক ছুটি: ${summary.weeklyOffDays + summary.holidayDutyDays + summary.leaveDays} দিন", 56f, y, paint)
        val bonusStatusText = if (summary.attendanceBonus > 0) {
            "(হাজিরা বোনাস অর্জিত)"
        } else if (summary.absentDays > 0) {
            "(হাজিরা বোনাস কর্তন)"
        } else {
            ""
        }
        canvas.drawText("অনুপস্থিত: ${summary.absentDays} দিন $bonusStatusText", 320f, y, paint)
        y += 28f

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(44f, y, pageWidth - 44f, y, paint)
        y += 24f

        // Section: Basic & Allowances
        paint.color = Color.rgb(5, 150, 105)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("মূল বেতন ও নির্ধারিত ভাতাসমূহ (Earnings Breakdown):", 44f, y, paint)
        y += 20f

        fun drawPdfRow(label: String, amount: Double, isBold: Boolean = false) {
            paint.color = if (isBold) Color.rgb(15, 23, 42) else Color.rgb(71, 85, 105)
            paint.textSize = 11f
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(label, 56f, y, paint)
            val valStr = String.format("Tk %.2f", amount)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(valStr, pageWidth - 56f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            y += 18f
        }

        drawPdfRow("মূল বেতন (Basic Salary)", summary.basicSalary)
        drawPdfRow("বাড়িভাড়া ভাতা (House Rent - 50%)", profile.houseRent)
        drawPdfRow("চিকিৎসা ভাতা (Medical Allowance)", profile.medicalAllowance)
        drawPdfRow("যাতায়াত ভাতা (Conveyance)", profile.conveyanceAllowance)
        drawPdfRow("খাদ্য ভাতা (Food Allowance)", profile.foodAllowance)
        val attendanceLabel = if (summary.attendanceBonus > 0) {
            if (summary.absentDays > 0) "হাজিরা বোনাস (${summary.absentDays} দিন অনুপস্থিতি অনুমোদিত)" else "হাজিরা বোনাস (নিয়মিত উপস্থিতি)"
        } else if (summary.absentDays > 0) {
            "হাজিরা বোনাস (${summary.absentDays} দিন অনুপস্থিতির জন্য বাতিল)"
        } else {
            "হাজিরা বোনাস"
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
        canvas.drawText("অতিরিক্ত আয় (Overtime & Extra Earnings):", 44f, y, paint)
        y += 20f

        drawPdfRow("ওভারটাইম আয় (${String.format("%.1f", summary.totalOtHours)} ঘণ্টা @ Tk ${String.format("%.2f", summary.hourlyOtRate)}/ঘণ্টা)", summary.totalOtAmount, isBold = true)
        if (summary.nightBillTotal > 0) {
            drawPdfRow("নাইট বিল (${summary.nightBillCount}টি @ Tk ${String.format("%.0f", profile.nightBillRate)})", summary.nightBillTotal)
        }
        if (summary.tiffinBillTotal > 0) {
            drawPdfRow("টিফিন বিল (${summary.tiffinBillCount}টি @ Tk ${String.format("%.0f", profile.tiffinBillRate)})", summary.tiffinBillTotal)
        }
        if (summary.totalPieceAmount > 0) {
            drawPdfRow("পিস রেট আয় (Piece Rate Earnings)", summary.totalPieceAmount)
        }
        if (summary.absentDeduction > 0) {
            paint.color = Color.rgb(220, 38, 38)
            drawPdfRow("অনুপস্থিতি কর্তন (${summary.absentDays} দিন @ Tk ${String.format("%.2f", summary.dailyAbsentRate)}/দিন)", -summary.absentDeduction, isBold = true)
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
        canvas.drawText("মোট প্রদেয় বেতন (Net Payable):", 56f, y + 28f, paint)

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
        canvas.drawText("শ্রমিকের স্বাক্ষর", 76f, y + 54f, paint)

        canvas.drawLine(pageWidth - 180f, y + 40f, pageWidth - 56f, y + 40f, paint)
        canvas.drawText("অ্যাকাউন্টস / কারখানা কর্তৃপক্ষ", pageWidth - 170f, y + 54f, paint)

        // Footer note
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generated via Overtime Tracker BD • বাংলাদেশ শ্রম আইন অনুযায়ী প্রস্তুতকৃত", pageWidth / 2f, pageHeight - 36f, paint)

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

            Toast.makeText(context, "পিডিএফ ডাউনলোড হয়েছে: $fileName", Toast.LENGTH_LONG).show()

            // Open intent
            savedUri?.let { uri ->
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(openIntent, "PDF ওপেন করুন")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(chooser)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Toast.makeText(context, "পিডিএফ সংরক্ষণে সমস্যা: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }

        return savedUri
    }
}
