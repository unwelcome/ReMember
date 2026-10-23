package dev.chaik.remember.core.data

import dev.chaik.remember.core.model.ExerciseKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class WorkoutImportTest {
    @Before
    fun russian() = Locale.setDefault(Locale.forLanguageTag("ru-RU"))

    /** Две тренировки из блокнота в формате импорта. */
    private val example = """
        {
          "version": 1,
          "workouts": [
            {
              "date": "2026-09-28", "start": "17:00", "end": "18:30", "weightBefore": 66.5, "weightAfter": null,
              "exercises": [
                { "name": "Жим лежа", "kind": "weight_reps", "sets": [ { "weight": 40, "reps": 12 }, { "weight": 50, "reps": 8 } ] },
                { "name": "Пресс", "kind": "reps", "sets": [ { "reps": 30 }, { "reps": 30 } ] },
                { "name": "Планка", "kind": "time", "sets": [ { "time": "1:30" } ] }
              ]
            },
            {
              "date": "2026-10-09", "start": "15:00", "weightBefore": 65.5, "note": "мб еще что то",
              "exercises": [
                { "name": "Хаммер", "sets": [ { "weight": 60, "reps": 15 }, { "weight": 65, "reps": 12 } ] },
                { "name": "Пресс", "sets": [ { "reps": 30 } ] },
                { "name": "Планка", "sets": [ { "time": "1:30" } ] }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `пример читается целиком`() {
        val workouts = WorkoutImport.parse(example)
        assertEquals(2, workouts.size)

        val first = workouts.first()
        assertEquals(LocalDate.of(2026, 9, 28), first.date)
        assertEquals(LocalTime.of(17, 0), first.start)
        assertEquals(66.5, first.weightBefore!!, 0.0)
        assertNull(first.weightAfter)
        assertTrue(first.imported)
        assertEquals(3, first.exercises.size)
        assertEquals("40 × 12", first.exercises[0].sets[0].format(first.exercises[0].exercise.kind))

        // Без «kind» вид угадывается по подходам
        val second = workouts[1]
        assertNull(second.end)
        assertEquals("мб еще что то", second.note)
        assertEquals(ExerciseKind.WEIGHT_REPS, second.exercises.first { it.exercise.name == "Хаммер" }.exercise.kind)
        assertEquals(ExerciseKind.REPS, second.exercises.first { it.exercise.name == "Пресс" }.exercise.kind)
        assertEquals(ExerciseKind.TIME, second.exercises.first { it.exercise.name == "Планка" }.exercise.kind)
        assertEquals(90, second.exercises.first { it.exercise.name == "Планка" }.sets.single().seconds)
    }

    @Test
    fun `по порядку дат, даже если в файле вперемешку`() {
        val json = """{"workouts": [{"date": "2026-10-20"}, {"date": "2026-09-28", "start": "9:00", "number": 49}]}"""
        assertEquals(listOf(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 20)), WorkoutImport.parse(json).map { it.date })
        assertEquals(LocalTime.of(9, 0), WorkoutImport.parse(json).first().start)
    }

    @Test
    fun `бег — скорость и время`() {
        val json = """{"workouts": [{"date": "2025-11-07", "exercises": [
            {"name": "Бег", "kind": "speed_time", "sets": [{"speed": 9.4, "time": "10:00"}]},
            {"name": "Бег трусцой", "sets": [{"speed": 8, "time": "5:00"}]}
        ]}]}"""
        val exercises = WorkoutImport.parse(json).single().exercises
        assertEquals("9,4 км/ч × 10:00", exercises[0].sets.single().format(ExerciseKind.SPEED_TIME, "км/ч"))
        assertEquals(ExerciseKind.SPEED_TIME, exercises[1].exercise.kind)
    }

    @Test
    fun `понятные ошибки`() {
        val badDate = assertThrows(WorkoutImportException::class.java) {
            WorkoutImport.parse("""{"workouts": [{"date": "28.09.2026"}]}""")
        }
        assertEquals(ImportProblem.InWorkout(1, "28.09.2026", ImportProblem.BadDate("28.09.2026")), badDate.problems.single())

        val badTime = assertThrows(WorkoutImportException::class.java) {
            WorkoutImport.parse("""{"workouts": [{"date": "2026-09-28", "exercises": [{"name": "Планка", "sets": [{"time": "полторы"}]}]}]}""")
        }
        assertTrue((badTime.problems.single() as ImportProblem.InWorkout).problem is ImportProblem.BadDuration)

        assertThrows(WorkoutImportException::class.java) { WorkoutImport.parse("просто текст") }
    }
}
