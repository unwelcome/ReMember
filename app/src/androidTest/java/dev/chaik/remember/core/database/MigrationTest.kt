package dev.chaik.remember.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
