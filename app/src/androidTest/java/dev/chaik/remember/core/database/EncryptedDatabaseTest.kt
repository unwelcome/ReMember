package dev.chaik.remember.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chaik.remember.core.data.DayRepository
import dev.chaik.remember.core.model.DayEntry
import dev.chaik.remember.core.security.DatabaseKeyManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.time.LocalDate

/**
 * Настоящая зашифрованная база на телефоне: пароль из Keystore,
 * данные переживают «перезапуск» (закрытие и новое открытие базы).
 * Работает с отдельными тестовыми ключом и файлом — настоящий дневник не трогается.
 */
@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    @After
    fun cleanUp() {
        context.deleteDatabase(DB_NAME)
        File(context.noBackupFilesDir, KEY_FILE).delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_ALIAS)
    }

    private fun keyManager() = DatabaseKeyManager(context, KEY_ALIAS, KEY_FILE)

    @Test
    fun passphraseIsStableAcrossInstances() {
        val first = keyManager().passphrase()
        val second = keyManager().passphrase()

        assertEquals(32, first.size)
        assertArrayEquals(first, second)
    }

    @Test
    fun dataSurvivesReopen() = runTest {
        val date = LocalDate.of(2026, 10, 23)

        DatabaseFactory.create(context, keyManager(), DB_NAME).let { db ->
            DayRepository(db).save(DayEntry(date = date, mood = 4, text = "Начал делать ReMember"))
            db.close()
        }

        // «Перезапуск»: новая база, новый менеджер ключей
        DatabaseFactory.create(context, keyManager(), DB_NAME).let { db ->
            val entry = DayRepository(db).observe(date).first()
            assertEquals("Начал делать ReMember", entry?.text)
            assertEquals(4, entry?.mood)
            db.close()
        }
    }

    @Test
    fun fileOnDiskIsEncrypted() = runTest {
        DatabaseFactory.create(context, keyManager(), DB_NAME).let { db ->
            DayRepository(db).save(DayEntry(date = LocalDate.now(), text = "секрет"))
            db.close()
        }

        val bytes = context.getDatabasePath(DB_NAME).readBytes()
        // У обычной SQLite файл начинается с «SQLite format 3», у зашифрованной — случайные байты
        assertFalse(String(bytes.copyOfRange(0, 15)).startsWith("SQLite format"))
        assertFalse(String(bytes, Charsets.UTF_8).contains("секрет"))
    }

    private companion object {
        const val DB_NAME = "test-encrypted.db"
        const val KEY_ALIAS = "test_database_key"
        const val KEY_FILE = "test-database.key"
    }
}
