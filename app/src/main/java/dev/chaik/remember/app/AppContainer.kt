package dev.chaik.remember.app

import android.content.Context
import dev.chaik.remember.core.data.DayRepository
import dev.chaik.remember.core.data.MoneyRepository
import dev.chaik.remember.core.data.NoteRepository
import dev.chaik.remember.core.data.SleepRepository
import dev.chaik.remember.core.data.TagRepository
import dev.chaik.remember.core.database.DatabaseFactory
import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.photos.PhotoStorage
import dev.chaik.remember.core.security.DatabaseKeyManager

/**
 * Все общие объекты приложения в одном месте (ручной DI).
 * Создаются лениво — при первом обращении, а не при запуске.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: ReMemberDatabase by lazy {
        DatabaseFactory.create(appContext, DatabaseKeyManager(appContext))
    }

    val photoStorage by lazy { PhotoStorage(appContext, database.photoDao()) }

    val dayRepository by lazy { DayRepository(database) }
    val sleepRepository by lazy { SleepRepository(database) }
    val moneyRepository by lazy { MoneyRepository(database) }
    val noteRepository by lazy { NoteRepository(database) }
    val tagRepository by lazy { TagRepository(database) }
}
