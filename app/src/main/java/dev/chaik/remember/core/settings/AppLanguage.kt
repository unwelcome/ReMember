package dev.chaik.remember.core.settings

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/**
 * Язык приложения. Хранит его сам Android (настройка «Язык приложения» в системе),
 * поэтому он действует сразу, ещё до запуска экранов. Список — в res/xml/locales_config.xml;
 * новый язык = папка res/values-xx со strings.xml и строка в этом списке.
 */
object AppLanguage {
    /** Языки, на которые переведено приложение (теги BCP 47). Пустой тег — как в системе. */
    val available = listOf("", "ru", "en")

    fun current(context: Context): String =
        context.getSystemService(LocaleManager::class.java).applicationLocales.get(0)?.language.orEmpty()

    fun set(context: Context, tag: String) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }
}
