package dev.chaik.remember.core.data

import dev.chaik.remember.core.database.ReMemberDatabase
import dev.chaik.remember.core.model.DEFAULT_CURRENCY
import dev.chaik.remember.core.model.MoneyCategory
import dev.chaik.remember.core.model.MoneyTotals
import dev.chaik.remember.core.model.MoneyTransaction
import dev.chaik.remember.core.model.MoneyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/** Траты, доходы и их категории. */
class MoneyRepository(db: ReMemberDatabase) {
    private val dao = db.moneyDao()

    /** При первом запуске — создать стандартные категории. Повторный вызов ничего не делает. */
    suspend fun ensureDefaultCategories() {
        if (dao.categoryCount() > 0) return
        DefaultCategories.forEachIndexed { index, category ->
            dao.insertCategory(category.copy(sortOrder = index).toEntity())
        }
    }

    fun observeCategories(type: MoneyType): Flow<List<MoneyCategory>> =
        dao.observeCategories(type).map { list -> list.map { it.toModel() } }

    /** Создаёт категорию (id = 0) или обновляет существующую. Возвращает id. */
    suspend fun saveCategory(category: MoneyCategory): Long =
        if (category.id == 0L) {
            dao.insertCategory(category.toEntity())
        } else {
            dao.updateCategory(category.toEntity())
            category.id
        }

    fun observeForDate(date: LocalDate): Flow<List<MoneyTransaction>> =
        dao.observeForDate(date).map { list -> list.map { it.toModel() } }

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<MoneyTransaction>> =
        dao.observeRange(from, to).map { list -> list.map { it.toModel() } }

    fun observeTotals(from: LocalDate, to: LocalDate, currency: String = DEFAULT_CURRENCY): Flow<MoneyTotals> =
        dao.observeTotals(from, to, currency).map { MoneyTotals(it.expenseMinor, it.incomeMinor) }

    /** Создаёт операцию (id = 0) или обновляет существующую. Возвращает id. */
    suspend fun save(transaction: MoneyTransaction): Long =
        if (transaction.id == 0L) {
            dao.insert(transaction.toEntity(createdAt = Instant.now()))
        } else {
            dao.update(transaction.toEntity(createdAt = transaction.createdAt ?: Instant.now()))
            transaction.id
        }

    suspend fun delete(id: Long) = dao.delete(id)
}

/** Категории при первом запуске; «Другое» — последней. */
private val DefaultCategories = listOf(
    MoneyCategory(name = "Еда", type = MoneyType.EXPENSE, icon = "🍔"),
    MoneyCategory(name = "Одежда", type = MoneyType.EXPENSE, icon = "👕"),
    MoneyCategory(name = "Игры", type = MoneyType.EXPENSE, icon = "🎮"),
    MoneyCategory(name = "Транспорт", type = MoneyType.EXPENSE, icon = "🚌"),
    MoneyCategory(name = "Кино", type = MoneyType.EXPENSE, icon = "🎬"),
    MoneyCategory(name = "Другое", type = MoneyType.EXPENSE, icon = "📦"),
    MoneyCategory(name = "Зарплата", type = MoneyType.INCOME, icon = "💼"),
    MoneyCategory(name = "Стипендия", type = MoneyType.INCOME, icon = "🎓"),
    MoneyCategory(name = "Перевод", type = MoneyType.INCOME, icon = "💸"),
    MoneyCategory(name = "Кешбэк", type = MoneyType.INCOME, icon = "🪙"),
    MoneyCategory(name = "Другое", type = MoneyType.INCOME, icon = "📦"),
)
