package dev.chaik.remember.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.chaik.remember.core.model.MoneyType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "money_category")
data class MoneyCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: MoneyType,
    /** Эмодзи или имя иконки. */
    val icon: String?,
    /** Цвет в формате ARGB. */
    val color: Long?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    val archived: Boolean,
)

@Entity(
    tableName = "money_transaction",
    foreignKeys = [
        ForeignKey(
            entity = MoneyCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("date"), Index("category_id")],
)
data class MoneyTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: MoneyType,
    /** Сумма в копейках (центах): без ошибок округления, как у Float/Double. */
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    /** Код валюты ISO 4217 — на случай, если валют станет несколько. */
    val currency: String,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    val comment: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
