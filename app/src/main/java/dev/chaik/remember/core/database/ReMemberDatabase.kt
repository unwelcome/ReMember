package dev.chaik.remember.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.chaik.remember.core.database.dao.DayEntryDao
import dev.chaik.remember.core.database.dao.MoneyDao
import dev.chaik.remember.core.database.dao.NoteDao
import dev.chaik.remember.core.database.dao.PhotoDao
import dev.chaik.remember.core.database.dao.SleepDao
import dev.chaik.remember.core.database.dao.TagDao
import dev.chaik.remember.core.database.dao.TaskDao
import dev.chaik.remember.core.database.entity.DayEntryEntity
import dev.chaik.remember.core.database.entity.DayEntryTagEntity
import dev.chaik.remember.core.database.entity.MoneyCategoryEntity
import dev.chaik.remember.core.database.entity.MoneyTransactionEntity
import dev.chaik.remember.core.database.entity.NoteEntity
import dev.chaik.remember.core.database.entity.NoteFolderEntity
import dev.chaik.remember.core.database.entity.NoteTagEntity
import dev.chaik.remember.core.database.entity.PhotoEntity
import dev.chaik.remember.core.database.entity.PhotoRefEntity
import dev.chaik.remember.core.database.entity.SleepEntryEntity
import dev.chaik.remember.core.database.entity.SubtaskEntity
import dev.chaik.remember.core.database.entity.TagEntity
import dev.chaik.remember.core.database.entity.TaskEntity

/**
 * Вся база приложения.
 *
 * При изменении таблиц: поднять [version] и добавить миграцию
 * (`autoMigrations` — для простых случаев). Схемы лежат в app/schemas.
 */
@Database(
    version = 2,
    entities = [
        DayEntryEntity::class,
        SleepEntryEntity::class,
        MoneyCategoryEntity::class,
        MoneyTransactionEntity::class,
        NoteFolderEntity::class,
        NoteEntity::class,
        TagEntity::class,
        DayEntryTagEntity::class,
        NoteTagEntity::class,
        PhotoEntity::class,
        PhotoRefEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
    ],
    autoMigrations = [
        // 2: задачи и подзадачи — только новые таблицы
        AutoMigration(from = 1, to = 2),
    ],
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ReMemberDatabase : RoomDatabase() {
    abstract fun dayEntryDao(): DayEntryDao
    abstract fun sleepDao(): SleepDao
    abstract fun moneyDao(): MoneyDao
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun photoDao(): PhotoDao
    abstract fun taskDao(): TaskDao
}
