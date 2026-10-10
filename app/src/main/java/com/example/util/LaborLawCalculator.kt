package com.example.util

import com.example.data.model.UserProfile
import java.text.DecimalFormat
import java.util.Locale

object LaborLawCalculator {

    const val STANDARD_MONTHLY_HOURS = 208.0 // 26 days * 8 hours

    /**
     * Calculate hourly overtime rate according to Bangladesh Labor Act 2006 (Section 108)
     * Regular hourly rate = Basic / 208
     * Overtime hourly rate = (Basic / 208) * 2 = Basic / 104
     */
    fun getHourlyOtRate(profile: UserProfile): Double {
        return if (profile.isCustomSalary && profile.customOtRate > 0.0) {
            profile.customOtRate
        } else {
            (profile.basicSalary / STANDARD_MONTHLY_HOURS) * 2.0
        }
    }

    /**
     * Standard RMG minimum wage gazette breakdown from Basic Salary
     */
    fun calculateAutoAllowances(basicSalary: Double): AutoAllowanceResult {
        val houseRent = basicSalary * 0.50
        val medical = 750.0 // Standard 750.0 Tk as per BD RMG Labor Gazette
        val conveyance = if (basicSalary > 0.0) 450.0 else 0.0
        val food = if (basicSalary > 0.0) 1250.0 else 0.0
        val gross = if (basicSalary > 0.0) (basicSalary + houseRent + medical + conveyance + food) else 0.0
        val otRate = if (basicSalary > 0.0) ((basicSalary / STANDARD_MONTHLY_HOURS) * 2.0) else 0.0

        return AutoAllowanceResult(
            basicSalary = basicSalary,
            houseRent = houseRent,
            medicalAllowance = medical,
            conveyanceAllowance = conveyance,
            foodAllowance = food,
            grossSalary = gross,
            otHourlyRate = otRate
        )
    }

    /**
     * Standard RMG minimum wage gazette breakdown from Gross Salary:
     * Fixed Allowances = Medical (750) + Conveyance (450) + Food (1250) = 2450
     * Gross = Basic + (Basic * 0.50) + 2450 = 1.5 * Basic + 2450
     * Basic = (Gross - 2450) / 1.5
     */
    fun calculateFromGrossSalary(grossSalary: Double): AutoAllowanceResult {
        if (grossSalary <= 0.0) {
            return AutoAllowanceResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        val fixedAllowances = 2450.0 // 750 + 450 + 1250
        val basic = if (grossSalary > fixedAllowances) {
            (grossSalary - fixedAllowances) / 1.5
        } else {
            grossSalary / 1.5
        }
        val houseRent = basic * 0.50
        val medical = 750.0
        val conveyance = 450.0
        val food = 1250.0
        val otRate = (basic / STANDARD_MONTHLY_HOURS) * 2.0

        return AutoAllowanceResult(
            basicSalary = basic,
            houseRent = houseRent,
            medicalAllowance = medical,
            conveyanceAllowance = conveyance,
            foodAllowance = food,
            grossSalary = grossSalary,
            otHourlyRate = otRate
        )
    }

    /**
     * Calculate OT amount for given hours and rate
     */
    fun calculateOtAmount(hours: Double, hourlyRate: Double): Double {
        return hours * hourlyRate
    }

    /**
     * Format number to Bengali or English string with currency symbol
     */
    fun formatCurrency(amount: Double, isBengali: Boolean = true): String {
        val df = DecimalFormat("#,##0.00")
        val formatted = df.format(amount)
        val withSymbol = "৳ $formatted"
        return if (isBengali) toBengaliDigits(withSymbol) else withSymbol
    }

    fun formatNumber(number: Double, isBengali: Boolean = true, decimalPlaces: Int = 1): String {
        val pattern = if (decimalPlaces == 0) "#,##0" else "#,##0.${"#".repeat(decimalPlaces)}"
        val df = DecimalFormat(pattern)
        val formatted = df.format(number)
        return if (isBengali) toBengaliDigits(formatted) else formatted
    }

    fun toBengaliDigits(input: String): String {
        val banglaDigits = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(banglaDigits[ch] ?: ch)
        }
        return sb.toString()
    }
}

data class AutoAllowanceResult(
    val basicSalary: Double,
    val houseRent: Double,
    val medicalAllowance: Double,
    val conveyanceAllowance: Double,
    val foodAllowance: Double,
    val grossSalary: Double,
    val otHourlyRate: Double
)
