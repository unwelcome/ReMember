package dev.chaik.remember.core.data

import dev.chaik.remember.core.model.Exercise
import dev.chaik.remember.core.model.ExerciseKind
import dev.chaik.remember.core.model.Workout
import dev.chaik.remember.core.model.WorkoutExercise
import dev.chaik.remember.core.model.WorkoutSet
import dev.chaik.remember.core.model.parseDuration
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/*
 * Файл импорта тренировок (версия 1):
 * {
 *   "version": 1,
 *   "workouts": [
 *     {
 *       "date": "2026-09-28", "start": "17:00", "end": "18:30",
 *       "weightBefore": 66.5, "weightAfter": null, "note": "",
 *       "exercises": [
 *         { "name": "Жим лежа", "kind": "weight_reps", "sets": [ { "weight": 40, "reps": 12 } ] },
 *         { "name": "Пресс", "kind": "reps", "sets": [ { "reps": 30 } ] },
 *         { "name": "Планка", "kind": "time", "sets": [ { "time": "1:30" } ] },
 *         { "name": "Бег", "kind": "speed_time", "sets": [ { "speed": 9.4, "time": "10:00" } ] }
 *       ]
 *     }
 *   ]
 * }
 * Обязательны только date и name. kind можно не писать — он угадается по подходам.
 * Незнакомые поля (например, номер тренировки) пропускаются: номера считаются по датам.
 */

@Serializable
private data class WorkoutFileJson(val version: Int = 1, val workouts: List<WorkoutJson> = emptyList())

@Serializable
private data class WorkoutJson(
    val date: String,
    val start: String? = null,
    val end: String? = null,
    val weightBefore: Double? = null,
    val weightAfter: Double? = null,
    val note: String = "",
    val exercises: List<ExerciseJson> = emptyList(),
)

@Serializable
private data class ExerciseJson(val name: String, val kind: String? = null, val sets: List<SetJson> = emptyList())

@Serializable
private data class SetJson(val weight: Double? = null, val reps: Int? = null, val time: String? = null, val speed: Double? = null)

/** Файл не читается; [message] — что именно не так, по-русски. */
class WorkoutImportException(message: String) : Exception(message)

object WorkoutImport {
    private val json = Json { ignoreUnknownKeys = true }

    /** Тренировки из файла — по хронологии, ещё не сохранённые (id = 0, imported = true). */
    fun parse(text: String): List<Workout> {
        val file = try {
            json.decodeFromString<WorkoutFileJson>(text)
        } catch (e: SerializationException) {
            throw WorkoutImportException("Это не файл тренировок: ${e.message?.lineSequence()?.firstOrNull().orEmpty()}")
        } catch (e: IllegalArgumentException) {
            throw WorkoutImportException("Это не файл тренировок: ${e.message?.lineSequence()?.firstOrNull().orEmpty()}")
        }
        if (file.version != 1) throw WorkoutImportException("Неизвестная версия файла: ${file.version}")

        val errors = mutableListOf<String>()
        val workouts = file.workouts.mapIndexedNotNull { i, w ->
            try {
                w.toModel()
            } catch (e: WorkoutImportException) {
                errors += "Тренировка ${i + 1} (${w.date}): ${e.message}"
                null
            }
        }
        if (errors.isNotEmpty()) {
            throw WorkoutImportException(errors.take(MAX_ERRORS).joinToString("\n") + if (errors.size > MAX_ERRORS) "\n…и ещё ${errors.size - MAX_ERRORS}" else "")
        }
        return workouts.sortedWith(compareBy<Workout> { it.date }.thenBy { it.start ?: LocalTime.MIN })
    }

    private fun WorkoutJson.toModel(): Workout = Workout(
        date = try {
            LocalDate.parse(date.trim())
        } catch (_: DateTimeParseException) {
            throw WorkoutImportException("дата «$date» — нужен вид ГГГГ-ММ-ДД")
        },
        start = start?.let { time(it, "начала") },
        end = end?.let { time(it, "конца") },
        weightBefore = weightBefore,
        weightAfter = weightAfter,
        note = note.trim(),
        imported = true,
        exercises = exercises.map { it.toModel() },
    )

    private fun ExerciseJson.toModel(): WorkoutExercise {
        if (name.isBlank()) throw WorkoutImportException("упражнение без названия")
        val sets = sets.map { set ->
            WorkoutSet(
                weight = set.weight,
                reps = set.reps,
                seconds = set.time?.let { parseDuration(it) ?: throw WorkoutImportException("«$name»: время «$it» — нужен вид м:сс") },
                speed = set.speed,
            )
        }
        val kind = when (kind?.trim()?.lowercase()) {
            null, "" -> guessKind(sets)
            "weight_reps" -> ExerciseKind.WEIGHT_REPS
            "reps" -> ExerciseKind.REPS
            "time" -> ExerciseKind.TIME
            "speed_time" -> ExerciseKind.SPEED_TIME
            else -> throw WorkoutImportException("«$name»: вид «$kind» — бывает weight_reps, reps, time или speed_time")
        }
        return WorkoutExercise(exercise = Exercise(name = name.trim(), kind = kind), sets = sets)
    }

    private fun guessKind(sets: List<WorkoutSet>): ExerciseKind = when {
        sets.any { it.speed != null } -> ExerciseKind.SPEED_TIME
        sets.any { it.weight != null } -> ExerciseKind.WEIGHT_REPS
        sets.any { it.seconds != null } -> ExerciseKind.TIME
        else -> ExerciseKind.REPS
    }

    private fun time(value: String, what: String): LocalTime = try {
        LocalTime.parse(value.trim().padStart(5, '0'))
    } catch (_: DateTimeParseException) {
        throw WorkoutImportException("время $what «$value» — нужен вид ЧЧ:ММ")
    }

    private const val MAX_ERRORS = 5
}
