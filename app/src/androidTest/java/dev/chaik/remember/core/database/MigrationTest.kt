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

    private companion object {
        const val DB = "migration-test.db"
    }
}
