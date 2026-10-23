package dev.chaik.remember.core.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.chaik.remember.core.model.Photo

/**
 * Состояние редактора: список блоков «текст, фото, текст, …», курсор и выделенное фото.
 *
 * Живёт во ViewModel экрана, поэтому переживает поворот экрана.
 * Всё, что меняет документ, идёт через методы этого класса —
 * так чередование «текст / фото» никогда не нарушается.
 */
@Stable
class EditorState(markdown: String = "") {

    sealed class Block {
        abstract val key: Long
    }

    class TextBlock internal constructor(override val key: Long, value: TextFieldValue) : Block() {
        var value by mutableStateOf(value)
            internal set
        val focusRequester = FocusRequester()
    }

    class PhotoBlock internal constructor(
        override val key: Long,
        val photoId: String,
        widthPercent: Int,
        aspectRatio: Float,
    ) : Block() {
        var widthPercent by mutableIntStateOf(widthPercent)
            internal set

        /** Ширина / высота. Пока размеры не загружены — 4:3, чтобы место под фото не прыгало сильно. */
        var aspectRatio by mutableFloatStateOf(aspectRatio)
            internal set
    }

    private var nextKey = 0L

    val blocks = mutableStateListOf<Block>()

    /** Текстовый блок, где последним стоял курсор; null — ещё нигде (вставка пойдёт в конец). */
    var activeTextKey by mutableStateOf<Long?>(null)
        private set

    /** Фото, у которого открыта панель действий. */
    var selectedPhotoKey by mutableStateOf<Long?>(null)
        private set

    init {
        load(markdown)
    }

    /** Текущий документ в виде Markdown. Чтение отслеживается Compose — подходит для snapshotFlow. */
    val markdown: String
        get() = EditorDocument.serialize(
            blocks.map {
                when (it) {
                    is TextBlock -> DocumentBlock.Text(it.value.text)
                    is PhotoBlock -> DocumentBlock.Photo(it.photoId, it.widthPercent)
                }
            },
        )

    val photoIds: List<String> get() = blocks.filterIsInstance<PhotoBlock>().map { it.photoId }

    val isEmpty: Boolean get() = blocks.all { it is TextBlock && it.value.text.isBlank() }

    /** Заменить документ целиком (при загрузке записи). */
    fun load(markdown: String) {
        blocks.clear()
        EditorDocument.parse(markdown).forEach { block ->
            blocks += when (block) {
                is DocumentBlock.Text -> textBlock(block.text)
                is DocumentBlock.Photo -> PhotoBlock(nextKey++, block.photoId, block.widthPercent, DEFAULT_ASPECT)
            }
        }
        activeTextKey = null
        selectedPhotoKey = null
    }

    /** Подставить реальные пропорции фото, когда они загрузились из базы. */
    fun applyPhotoSizes(photos: Map<String, Photo>) {
        blocks.filterIsInstance<PhotoBlock>().forEach { block ->
            photos[block.photoId]?.let { block.aspectRatio = it.aspect }
        }
    }

    // Текст

    /** Ввод в текстовом блоке. Enter в списке продолжает список. */
    fun onTextChange(block: TextBlock, value: TextFieldValue) {
        block.value = MarkdownFormatting.continueList(block.value, value)
    }

    /** Стоит ли сейчас курсор в каком-нибудь текстовом блоке. */
    var hasFocus by mutableStateOf(false)
        private set

    fun onTextFocusChanged(block: TextBlock, focused: Boolean) {
        if (focused) {
            activeTextKey = block.key
            selectedPhotoKey = null
            hasFocus = true
        } else if (activeTextKey == block.key) {
            hasFocus = false
        }
    }

    /** Применить действие панели форматирования к блоку с курсором. */
    fun format(transform: (TextFieldValue) -> TextFieldValue) {
        val block = activeTextBlock() ?: return
        block.value = transform(block.value)
    }

    /** Блок с курсором, а если его не было — последний текстовый. */
    fun activeTextBlock(): TextBlock? =
        blocks.firstOrNull { it.key == activeTextKey } as? TextBlock
            ?: blocks.lastOrNull() as? TextBlock

    // Фото

    /**
     * Вставить фото после строки с курсором (слово посередине не разрезается):
     * текст делится на «до» и «после», фото встают между ними,
     * курсор — в начало текста после них.
     */
    fun insertPhotos(photos: List<Photo>): TextBlock? {
        if (photos.isEmpty()) return null
        val target = activeTextBlock() ?: return null
        val index = blocks.indexOf(target)
        val value = target.value
        val cursor = value.selection.max.coerceIn(0, value.text.length)
        val cut = value.text.indexOf('\n', cursor).let { if (it < 0) value.text.length else it }
        val before = value.text.substring(0, cut).removeSuffix("\n")
        val after = value.text.substring(cut).removePrefix("\n")

        target.value = TextFieldValue(before, TextRange(before.length))
        val inserted = buildList {
            photos.forEachIndexed { i, photo ->
                add(PhotoBlock(nextKey++, photo.id, DocumentBlock.FULL_WIDTH, photo.aspect))
                add(textBlock(if (i == photos.lastIndex) after else ""))
            }
        }
        blocks.addAll(index + 1, inserted)
        val caret = inserted.last() as TextBlock
        caret.value = caret.value.copy(selection = TextRange(0))
        activeTextKey = caret.key
        focusRequestKey = caret.key
        return caret
    }

    /** Блок, в который нужно перевести фокус (выполняет RichEditor). */
    var focusRequestKey by mutableStateOf<Long?>(null)
        private set

    fun consumeFocusRequest() {
        focusRequestKey = null
    }

    fun selectPhoto(key: Long?) {
        selectedPhotoKey = key
    }

    fun setPhotoWidth(key: Long, percent: Int) {
        photo(key)?.widthPercent = percent.coerceIn(DocumentBlock.MIN_WIDTH, DocumentBlock.FULL_WIDTH)
    }

    /** Удалить фото; текст до и после него склеивается в один абзац-блок. */
    fun removePhoto(key: Long) {
        val index = blocks.indexOfFirst { it.key == key }
        if (index <= 0) return
        val before = blocks[index - 1] as TextBlock
        val after = blocks[index + 1] as TextBlock
        val caret = before.value.text.length
        before.value = TextFieldValue(EditorDocument.joinParagraphs(before.value.text, after.value.text), TextRange(caret))
        blocks.removeRange(index, index + 2)
        if (selectedPhotoKey == key) selectedPhotoKey = null
        if (activeTextKey == after.key) activeTextKey = before.key
    }

    fun canMovePhoto(key: Long, up: Boolean): Boolean {
        val index = blocks.indexOfFirst { it.key == key }
        if (index <= 0) return false
        val neighbour = blocks[if (up) index - 1 else index + 1] as TextBlock
        return neighbour.value.text.isNotEmpty() || blocks.getOrNull(if (up) index - 2 else index + 2) is PhotoBlock
    }

    /**
     * Сдвинуть фото на один абзац вверх или вниз.
     * Если соседний текст пуст и за ним другое фото — фото меняются местами.
     */
    fun movePhoto(key: Long, up: Boolean) {
        val index = blocks.indexOfFirst { it.key == key }
        if (index <= 0 || !canMovePhoto(key, up)) return
        val above = blocks[index - 1] as TextBlock
        val below = blocks[index + 1] as TextBlock

        if (up) {
            val text = above.value.text
            if (text.isEmpty()) return swapPhotos(index, index - 2)
            val split = text.lastIndexOf('\n')
            val paragraph = text.substring(split + 1)
            above.value = TextFieldValue(if (split < 0) "" else text.substring(0, split))
            below.value = TextFieldValue(EditorDocument.joinParagraphs(paragraph, below.value.text))
        } else {
            val text = below.value.text
            if (text.isEmpty()) return swapPhotos(index, index + 2)
            val split = text.indexOf('\n')
            val paragraph = if (split < 0) text else text.substring(0, split)
            below.value = TextFieldValue(if (split < 0) "" else text.substring(split + 1))
            above.value = TextFieldValue(EditorDocument.joinParagraphs(above.value.text, paragraph))
        }
    }

    private fun swapPhotos(a: Int, b: Int) {
        val first = blocks[a]
        blocks[a] = blocks[b]
        blocks[b] = first
    }

    private fun photo(key: Long) = blocks.firstOrNull { it.key == key } as? PhotoBlock

    private fun textBlock(text: String) = TextBlock(nextKey++, TextFieldValue(text, TextRange(text.length)))

    private companion object {
        const val DEFAULT_ASPECT = 4f / 3f
    }
}

private val Photo.aspect: Float get() = if (height == 0) 1f else width.toFloat() / height
