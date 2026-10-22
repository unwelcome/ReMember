package dev.chaik.remember.core.database

import android.content.Context
import androidx.room.Room
import dev.chaik.remember.core.security.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {
    const val DATABASE_NAME = "remember.db"

    /** Зашифрованная база: файл на диске без пароля из Keystore не читается. */
    fun create(
        context: Context,
        keyManager: DatabaseKeyManager,
        name: String = DATABASE_NAME,
    ): ReMemberDatabase {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(context, ReMemberDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(keyManager.passphrase()))
            .build()
    }
}
