package dev.chaik.remember.core.model

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalTime

/**
 * Тренировка в зале. Номер не хранится — он считается по порядку дат ([number]),
 * поэтому импорт старых тренировок сам расставляет номера.
 */
data class Workout(
    val id: Long = 0,
    /** Сквозной номер по хронологии, начиная с 1. */
    val number: Int = 0,
    val date: LocalDate,
    val start: LocalTime? = null,
    /** null — ещё идёт или время конца не записано. */
    val end: LocalTime? = null,
    /** Вес тела до и после, кг; оба необязательные. */
    val weightBefore: Double? = null,
    val weightAfter: Double? = null,
    val note: String = "",
    /** Идёт прямо сейчас (начата кнопкой и ещё не закончена). */
    val active: Boolean = false,
    /** Перенесена из файла. */
    val imported: Boolean = false,
    val exercises: List<WorkoutExercise> = emptyList(),
)

/** Упражнение внутри тренировки — с подходами по порядку. */
data class WorkoutExercise(
    val id: Long = 0,
    val exercise: Exercise,
    val sets: List<WorkoutSet> = emptyList(),
)

/** Упражнение из справочника: название и как записываются его подходы. */
data class Exercise(
    val id: Long = 0,
    val name: String,
    val kind: ExerciseKind = ExerciseKind.WEIGHT_REPS,
)

/** Как записываются подходы: «40×12», «30» или «1:30». */
enum class ExerciseKind(val code: String) {
    WEIGHT_REPS("WR"),
    REPS("R"),
    TIME("T"),
    ;

    companion object {
        fun fromCode(code: String): ExerciseKind = entries.firstOrNull { it.code == code } ?: WEIGHT_REPS
    }
}

/** Подход. Заполнены те поля, что нужны виду упражнения; пустое — «не записано». */
data class WorkoutSet(
    val id: Long = 0,
    /** Вес снаряда, кг, можно с дробью (17,5). */
    val weight: Double? = null,
    val reps: Int? = null,
    val seconds: Int? = null,
) {
    /** Как подход выглядит в записи: «40 × 12», «30», «1:30». */
    fun format(kind: ExerciseKind): String = when (kind) {
        ExerciseKind.WEIGHT_REPS -> "${weight?.let(::formatKg) ?: "—"} × ${reps ?: "—"}"
        ExerciseKind.REPS -> reps?.toString() ?: "—"
        ExerciseKind.TIME -> seconds?.let(::formatDuration) ?: "—"
    }
}

/** Краткая строка для списка тренировок. */
data class WorkoutSummary(
    val id: Long,
    val number: Int,
    val date: LocalDate,
    val start: LocalTime?,
    val end: LocalTime?,
    val weightBefore: Double?,
    val weightAfter: Double?,
    val active: Boolean,
    val exerciseNames: List<String>,
)

/** «40», «17,5», «65,75». */
fun formatKg(value: Double): String = DecimalFormat("0.##").format(value)

/** «1:30», «12:05», для часа и больше — «1:02:03». */
fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** «1:30» или «90» → 90 секунд; «2:05:00» тоже понимается. null — не разобрать. */
fun parseDuration(text: String): Int? {
    val parts = text.trim().split(':')
    if (parts.isEmpty() || parts.size > 3 || parts.any { it.isEmpty() || !it.all(Char::isDigit) }) return null
    val numbers = parts.map { it.toInt() }
    return numbers.fold(0) { acc, n -> acc * 60 + n }.takeIf { it > 0 }
}

/** «17,5» и «17.5» → 17.5. */
fun parseKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }
