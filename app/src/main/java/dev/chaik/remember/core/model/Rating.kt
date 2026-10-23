package dev.chaik.remember.core.model

/**
 * Оценка в половинках звезды: 1 = ½★, 19 = 9.5★, 20 = 10★.
 * Целое число в базе — без ошибок округления, как у дробных.
 *
 * Секрет: при 10 из 10 три тапа по последней звезде дают [SECRET] = 11 из 10.
 */
object Rating {
    const val DAY_STARS = 10
    const val SLEEP_STARS = 5

    /** 11 из 10 — в половинках. */
    const val SECRET = 22

    fun isSecret(halves: Int?): Boolean = halves == SECRET

    /** «9.5», «10», «11» — для подписи. */
    fun format(halves: Int): String =
        if (halves % 2 == 0) (halves / 2).toString() else "${halves / 2}.5"
}
