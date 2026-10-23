package dev.chaik.remember.core.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Событие календаря. Подписки, годовщины и прочее — это обычные события с повтором;
 * отдельный вид только у дня рождения: [title] — имя человека, повтор всегда ежегодный.
 */
data class Event(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    /** День события; у повторяющегося — первый раз. У дня рождения важны только день и месяц. */
    val date: LocalDate,
    val time: LocalTime = DEFAULT_TIME,
    val color: EventColor = EventColor.DEFAULT,
    val repeat: Repeat = Repeat.None,
    /** За сколько минут предупредить: null — не предупреждать, 0 — в момент события. */
    val remindMinutes: Int? = null,
    val isBirthday: Boolean = false,
    /** Год рождения — чтобы показать, сколько исполняется. */
    val birthYear: Int? = null,
) {
    /** Приходится ли событие на [day]. День рождения — каждый год, в том числе в годы до [date]. */
    fun occursOn(day: LocalDate): Boolean {
        if (!isBirthday) return repeat.occursOn(date, day)
        if (birthYear != null && day.year < birthYear) return false
        return Repeat.Yearly.occursOn(date.withYear(day.year.coerceAtMost(date.year)), day)
    }

    /** Сколько исполняется в [day] (для дня рождения с известным годом). */
    fun ageOn(day: LocalDate): Int? = birthYear?.takeIf { isBirthday }?.let { day.year - it }

    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(8, 0)
    }
}

/**
 * Готовые цвета событий. В базе — имя константы, поэтому порядок и состав можно менять,
 * не ломая сохранённые события (незнакомое имя читается как [DEFAULT]).
 */
enum class EventColor(val argb: Long) {
    RED(0xFFE5484D),
    ORANGE(0xFFF76B15),
    YELLOW(0xFFFFC53D),
    GREEN(0xFF30A46C),
    TEAL(0xFF12A594),
    SKY(0xFF0EA5E9),
    BLUE(0xFF3E63DD),
    PURPLE(0xFF8E4EC6),
    PINK(0xFFD6409F),
    GRAY(0xFF8B8D98),
    ;

    companion object {
        val DEFAULT = BLUE
        val BIRTHDAY = RED

        fun fromName(name: String): EventColor = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
