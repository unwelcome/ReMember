package dev.chaik.remember.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Миграции базы на настоящем телефоне: данные старой версии переживают обновление.
 * Схемы версий берутся из app/schemas.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ReMemberDatabase::class.java)

    @Test
    fun from1To2KeepsDiaryAndAddsTasks() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO day_entry (date, mood, text, created_at, updated_at) VALUES (20384, 22, 'запись дня', 0, 0)",
            )
            db.execSQL("INSERT INTO note (title, text, folder_id, pinned, created_at, updated_at) VALUES ('Заметка', '', NULL, 1, 0, 0)")
        }

        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT text, mood FROM day_entry").use { cursor ->
                cursor.moveToFirst()
                assertEquals("запись дня", cursor.getString(0))
                assertEquals(22, cursor.getInt(1))
            }
            db.query("SELECT COUNT(*) FROM note").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            // Новые таблицы на месте и работают
            db.execSQL(
                "INSERT INTO task (title, notes, due_date, due_time, priority, repeat, done, done_at, created_at, updated_at) " +
                    "VALUES ('Задача', '', NULL, NULL, 0, '', 0, NULL, 0, 0)",
            )
            db.execSQL("INSERT INTO subtask (task_id, title, done, sort_order) VALUES (1, 'Шаг', 0, 0)")
        }
    }

    @Test
    fun from4To5KeepsSetsAndAddsSpeed() {
        helper.createDatabase(DB, 4).use { db ->
            db.execSQL("INSERT INTO exercise (name, name_key, kind, created_at) VALUES ('Жим лежа', 'жим лежа', 'WR', 0)")
            db.execSQL(
                "INSERT INTO workout (date, start_time, end_time, weight_before, weight_after, note, active, imported, " +
                    "created_at, updated_at) VALUES (20359, 61200, NULL, NULL, NULL, '', 0, 0, 0, 0)",
            )
            db.execSQL("INSERT INTO workout_exercise (workout_id, exercise_id, sort_order) VALUES (1, 1, 0)")
            db.execSQL("INSERT INTO workout_set (workout_exercise_id, sort_order, weight, reps, seconds) VALUES (1, 0, 40.0, 12, NULL)")
        }

        helper.runMigrationsAndValidate(DB, 5, true).use { db ->
            db.query("SELECT weight, reps, speed FROM workout_set").use { cursor ->
                cursor.moveToFirst()
                assertEquals(40.0, cursor.getDouble(0), 0.0)
                assertEquals(12, cursor.getInt(1))
                assertTrue(cursor.isNull(2))
            }
            db.execSQL("INSERT INTO workout_set (workout_exercise_id, sort_order, weight, reps, seconds, speed) VALUES (1, 1, NULL, NULL, 600, 9.4)")
        }
    }

    @Test
    fun from3To4KeepsEventsAndAddsGym() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO event (title, description, date, time, color, repeat, remind_minutes, is_birthday, birth_year, " +
                    "created_at, updated_at) VALUES ('Иван', '', 20522, 28800, 'RED', 'Y', NULL, 1, 1995, 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(DB, 4, true).use { db ->
            db.query("SELECT title, birth_year FROM event").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Иван", cursor.getString(0))
                assertEquals(1995, cursor.getInt(1))
            }
            db.execSQL("INSERT INTO exercise (name, name_key, kind, created_at) VALUES ('Жим лежа', 'жим лежа', 'WR', 0)")
            db.execSQL(
                "INSERT INTO workout (date, start_time, end_time, weight_before, weight_after, note, active, imported, " +
                    "created_at, updated_at) VALUES (20359, 61200, 66600, 66.5, NULL, '', 0, 1, 0, 0)",
            )
            db.execSQL("INSERT INTO workout_exercise (workout_id, exercise_id, sort_order) VALUES (1, 1, 0)")
            db.execSQL("INSERT INTO workout_set (workout_exercise_id, sort_order, weight, reps, seconds) VALUES (1, 0, 40.0, 12, NULL)")
        }
    }

    @Test
    fun from2To3KeepsTasksAndAddsEvents() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO task (title, notes, due_date, due_time, priority, repeat, done, done_at, created_at, updated_at) " +
                    "VALUES ('Задача', '', NULL, NULL, 2, 'D', 0, NULL, 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(DB, 3, true).use { db ->
            db.query("SELECT title, priority, repeat FROM task").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Задача", cursor.getString(0))
                assertEquals(2, cursor.getInt(1))
                assertEquals("D", cursor.getString(2))
            }
            db.execSQL(
                "INSERT INTO event (title, description, date, time, color, repeat, remind_minutes, is_birthday, birth_year, " +
                    "created_at, updated_at) VALUES ('Иван', '', 20522, 28800, 'RED', 'Y', 1440, 1, 1995, 0, 0)",
            )
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
