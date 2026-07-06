package elieoko.app.mcoresystem.domain.util

import elieoko.app.mcoresystem.data.preferences.ExchangeRateRepository
import java.text.NumberFormat
import java.util.Locale

object CurrencyConverter {

    fun toCDF(amount: Double, currencyCode: String, rate: Double): Double {
        return when (currencyCode.uppercase()) {
            ExchangeRateRepository.CURRENCY_USD_CODE -> amount * rate
            else -> amount
        }
    }

    fun toUSD(amount: Double, currencyCode: String, rate: Double): Double {
        return when (currencyCode.uppercase()) {
            ExchangeRateRepository.CURRENCY_CDF_CODE -> if (rate > 0) amount / rate else 0.0
            else -> amount
        }
    }

    fun formatCDF(amount: Double): String {
        return "${formatNumber(amount)} FC"
    }

    fun formatUSD(amount: Double): String {
        return "$${formatNumber(amount)}"
    }

    fun conversionLabel(amount: Double, currencyCode: String, rate: Double): String {
        return when (currencyCode.uppercase()) {
            ExchangeRateRepository.CURRENCY_USD_CODE -> "≈ ${formatCDF(toCDF(amount, currencyCode, rate))}"
            ExchangeRateRepository.CURRENCY_CDF_CODE -> "≈ ${formatUSD(toUSD(amount, currencyCode, rate))}"
            else -> ""
        }
    }

    private fun formatNumber(value: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        return formatter.format(value)
    }
}
