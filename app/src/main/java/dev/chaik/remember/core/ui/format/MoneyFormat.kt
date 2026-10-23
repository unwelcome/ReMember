package dev.chaik.remember.core.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/** Суммы: копейки ↔ текст. Пока валюта одна — рубли. */
object MoneyFormat {
    private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("ru")).apply {
        groupingSeparator = ' ' // узкий неразрывный пробел: «1 234»
        decimalSeparator = ','
    }

    /** 123450 → «1 234,50 ₽», 45000 → «450 ₽». */
    fun format(amountMinor: Long, withSign: Boolean = false, sign: Char = '−'): String {
        val rubles = BigDecimal.valueOf(abs(amountMinor), 2)
        val pattern = if (amountMinor % 100 == 0L) "#,##0" else "#,##0.00"
        val number = DecimalFormat(pattern, symbols).format(rubles)
        return (if (withSign) "$sign" else "") + "$number ₽"
    }

    /** «450», «450,5», «1 234.50» → копейки; null — не число или не больше нуля. */
    fun parse(text: String): Long? {
        val normalized = text.trim().replace(" ", "").replace(" ", "").replace(" ", "").replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    }

    /** Копейки → текст для поля ввода: 45050 → «450,50». */
    fun toInput(amountMinor: Long): String =
        if (amountMinor % 100 == 0L) (amountMinor / 100).toString()
        else BigDecimal.valueOf(amountMinor, 2).toPlainString().replace('.', ',')
}
