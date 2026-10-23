package dev.chaik.remember.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.database.entity.PhotoEntity
import dev.chaik.remember.core.model.DayEntry
import dev.chaik.remember.core.model.Event
import dev.chaik.remember.core.model.EventColor
import dev.chaik.remember.core.model.MoneyCategory
import dev.chaik.remember.core.model.MoneyTransaction
import dev.chaik.remember.core.model.MoneyType
import dev.chaik.remember.core.model.Note
import dev.chaik.remember.core.model.NoteFolder
import dev.chaik.remember.core.model.SleepEntry
import dev.chaik.remember.core.model.Subtask
import dev.chaik.remember.core.model.Task
import dev.chaik.remember.core.model.Repeat
import dev.chaik.remember.core.photos.PhotoRefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Логика репозиториев на базе в памяти (без шифрования — оно проверяется отдельно). */
@RunWith(AndroidJUnit4::class)
class RepositoriesTest {
    private lateinit var db: ReMemberDatabase
    private val today = LocalDate.of(2026, 10, 23)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ReMemberDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun dayEntryWithTagsUpdatesInPlace() = runTest {
        val days = DayRepository(db)
        val tags = TagRepository(db)
        val gym = tags.getOrCreate("Зал")

        days.save(DayEntry(date = today, text = "Первая версия", tags = listOf(gym)))
        val created = days.observe(today).first()!!

        days.save(created.copy(text = "Исправил", tags = listOf(gym, tags.getOrCreate("работа"))))
        val updated = days.observe(today).first()!!

        assertEquals("Исправил", updated.text)
        assertEquals(setOf("Зал", "работа"), updated.tags.map { it.name }.toSet())
        assertEquals(created.createdAt, updated.createdAt) // дата создания не меняется при правке
    }

    @Test
    fun tagNamesAreCaseInsensitive() = runTest {
        val tags = TagRepository(db)
        assertEquals(tags.getOrCreate("Работа").id, tags.getOrCreate("работа").id)
        assertEquals(1, tags.observeAll().first().size)
    }

    @Test
    fun unusedTagsAreRemoved() = runTest {
        val days = DayRepository(db)
        val tags = TagRepository(db)
        days.save(DayEntry(date = today, tags = listOf(tags.getOrCreate("старый"))))
        days.save(DayEntry(date = today, tags = emptyList()))

        tags.deleteUnused()

        assertTrue(tags.observeAll().first().isEmpty())
    }

    @Test
    fun sleepIsStoredPerDay() = runTest {
        val sleep = SleepRepository(db)
        sleep.save(SleepEntry(date = today, quality = 3, dream = "Летал над городом"))
        assertEquals("Летал над городом", sleep.observe(today).first()?.dream)
        assertNull(sleep.observe(today.minusDays(1)).first())
    }

    @Test
    fun moneyTotalsForPeriod() = runTest {
        val money = MoneyRepository(db)
        val food = money.saveCategory(MoneyCategory(name = "Еда", type = MoneyType.EXPENSE))
        money.save(MoneyTransaction(date = today, type = MoneyType.EXPENSE, amountMinor = 45_050, categoryId = food))
        money.save(MoneyTransaction(date = today.minusDays(3), type = MoneyType.EXPENSE, amountMinor = 10_000))
        money.save(MoneyTransaction(date = today, type = MoneyType.INCOME, amountMinor = 1_000_000))
        money.save(MoneyTransaction(date = today.minusMonths(2), type = MoneyType.EXPENSE, amountMinor = 99_999))

        val totals = money.observeTotals(today.withDayOfMonth(1), today).first()

        assertEquals(55_050, totals.expenseMinor)
        assertEquals(1_000_000, totals.incomeMinor)
        assertEquals(944_950, totals.balanceMinor)
        assertEquals(2, money.observeForDate(today).first().size)
    }

    @Test
    fun deletingCategoryKeepsTransactions() = runTest {
        val money = MoneyRepository(db)
        val taxi = money.saveCategory(MoneyCategory(name = "Такси", type = MoneyType.EXPENSE))
        money.save(MoneyTransaction(date = today, type = MoneyType.EXPENSE, amountMinor = 500, categoryId = taxi))

        db.openHelper.writableDatabase.execSQL("DELETE FROM money_category WHERE id = $taxi")

        assertNull(money.observeForDate(today).first().single().categoryId)
    }

    @Test
    fun notesPinnedFirstAndFolderDeletionKeepsNotes() = runTest {
        val notes = NoteRepository(db)
        val folder = notes.saveFolder(NoteFolder(name = "Идеи"))
        notes.save(Note(title = "Обычная", folderId = folder))
        val pinned = notes.save(Note(title = "Важная", pinned = true))
        notes.save(Note(title = "Свежая"))

        assertEquals(pinned, notes.observeAll().first().first().id)

        notes.deleteFolder(folder)

        // Все три заметки на месте, «Обычная» теперь тоже без папки
        assertEquals(3, notes.observeInFolder(null).first().size)
    }

    @Test
    fun notesListCarriesTagsAndQuickActionsKeepUpdatedAt() = runTest {
        val notes = NoteRepository(db)
        val tags = TagRepository(db)
        val folder = notes.saveFolder(NoteFolder(name = "Идеи"))
        val read = tags.getOrCreate("читать")
        val id = notes.save(Note(title = "Книги", tags = listOf(read, tags.getOrCreate("дом"))))
        notes.save(Note(title = "Без тегов"))
        val updatedAt = notes.get(id)!!.updatedAt

        notes.setPinned(id, true)
        notes.moveToFolder(id, folder)

        val book = notes.observeSummaries().first().first { it.id == id }
        assertEquals(setOf("читать", "дом"), book.tags.map { it.name }.toSet())
        assertTrue(book.pinned)
        assertEquals(folder, book.folderId)
        // Закрепление и перенос — не «правка» заметки: дата изменения та же
        assertEquals(updatedAt, book.updatedAt)
        assertTrue(notes.observeSummaries().first().first { it.id != id }.tags.isEmpty())
    }

    @Test
    fun noteSummaryHasShortPreviewAndCover() = runTest {
        val notes = NoteRepository(db)
        val photo = "3f2b8c1e-0d4a-4b6f-9e21-7c5a0b9d1e42"
        val long = "очень длинный текст ".repeat(100)
        notes.save(Note(title = "С фото", text = "Начало **жирным**\n![](photo:$photo?w=50)\n$long"))
        notes.save(Note(title = "Без фото", text = "просто текст"))

        val list = notes.observeSummaries().first()
        val withPhoto = list.first { it.title == "С фото" }
        // Обложка нашлась прямо в базе, превью — без разметки и не длиннее лимита
        assertEquals(photo, withPhoto.coverPhotoId)
        assertTrue(withPhoto.preview.startsWith("Начало жирным"))
        assertTrue(withPhoto.preview.length <= 151)
        assertNull(list.first { it.title == "Без фото" }.coverPhotoId)
    }

    @Test
    fun repeatingTaskMovesToNextDateAndResetsSubtasks() = runTest {
        val tasks = TaskRepository(db)
        val id = tasks.save(
            Task(
                title = "Зал",
                dueDate = today,
                repeat = Repeat.Daily,
                subtasks = listOf(Subtask(title = "Разминка", done = true), Subtask(title = "Спина")),
            ),
        )
        tasks.setDone(tasks.get(id)!!, done = true, today = today)

        val moved = tasks.get(id)!!
        assertEquals(today.plusDays(1), moved.dueDate)
        assertTrue(!moved.done)
        assertTrue(moved.subtasks.none { it.done })
        assertEquals(listOf("Разминка", "Спина"), moved.subtasks.map { it.title })

        // Обычная задача — просто выполнена, и её можно вернуть
        val once = tasks.save(Task(title = "Купить хлеб"))
        tasks.setDone(tasks.get(once)!!, done = true, today = today)
        assertTrue(tasks.get(once)!!.done)
        tasks.deleteDone()
        assertNull(tasks.get(once))
    }

    @Test
    fun eventsAreStoredAsRulesAndBirthdaysAreYearly() = runTest {
        val events = EventRepository(db)
        val id = events.save(
            Event(
                title = "Иван Петров",
                date = LocalDate.of(2026, 3, 10),
                color = EventColor.PURPLE,
                repeat = Repeat.None,
                remindMinutes = 1440,
                isBirthday = true,
                birthYear = 1995,
            ),
        )
        val saved = events.get(id)!!
        // У дня рождения повтор всегда ежегодный, даже если пришёл другой
        assertEquals(Repeat.Yearly, saved.repeat)
        assertEquals(EventColor.PURPLE, saved.color)
        assertEquals(1440, saved.remindMinutes)
        assertEquals(1995, saved.birthYear)

        // Обычное событие: год рождения не хранится
        val other = events.save(Event(title = "Подписка", date = today, repeat = Repeat.Monthly, birthYear = 2000))
        assertNull(events.get(other)!!.birthYear)
        assertEquals(2, events.observeAll().first().size)

        events.delete(id)
        assertNull(events.get(id))
    }

    @Test
    fun photoRefsFollowText() = runTest {
        val days = DayRepository(db)
        val notes = NoteRepository(db)
        val photoDao = db.photoDao()
        val old = Instant.now().minus(2, ChronoUnit.DAYS)
        val kept = "3f2b8c1e-0d4a-4b6f-9e21-7c5a0b9d1e42"
        val dropped = "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d"
        listOf(kept, dropped).forEach { photoDao.insert(PhotoEntity(it, 100, 100, 1000, old)) }

        days.save(DayEntry(date = today, text = "![](${PhotoRefs.uri(kept)}) ![](${PhotoRefs.uri(dropped)})"))
        val noteId = notes.save(Note(text = "![](${PhotoRefs.uri(kept)})"))
        assertTrue(photoDao.unreferencedBefore(Instant.now()).isEmpty())

        // Убрали фото из записи дня — на него больше никто не ссылается
        days.save(DayEntry(date = today, text = "![](${PhotoRefs.uri(kept)})"))
        assertEquals(listOf(dropped), photoDao.unreferencedBefore(Instant.now()).map { it.id })

        // kept ещё в заметке — после удаления дня оно не должно стать «ничьим»
        days.delete(today)
        assertEquals(listOf(dropped), photoDao.unreferencedBefore(Instant.now()).map { it.id })

        notes.delete(noteId)
        assertEquals(setOf(kept, dropped), photoDao.unreferencedBefore(Instant.now()).map { it.id }.toSet())
    }
}
