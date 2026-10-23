package dev.chaik.remember.core.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.chaik.remember.R
import java.io.File
import kotlin.math.roundToInt

/** Размеры фото на кнопках панели. */
private val WidthPresets = listOf(50, 75, 100)

private val GridSpacing = 4.dp
private val PhotoShape = RoundedCornerShape(16.dp)
private val CellShape = RoundedCornerShape(8.dp)

/**
 * Редактор «текст + фото». Сам не прокручивается — кладётся в прокручиваемый контейнер экрана.
 *
 * @param photoFile файл фото по id (полноразмерный — Coil сам уменьшит под размер на экране)
 * @param onOpenPhotos открыть фото на весь экран: все фото блока и с какого начать
 */
@Composable
fun RichEditor(
    state: EditorState,
    photoFile: (String) -> File,
    onOpenPhotos: (photoIds: List<String>, index: Int) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val colors = MaterialTheme.colorScheme
    val style = textStyle.copy(color = colors.onSurface)
    val highlighter = remember(colors) {
        MarkdownHighlighter(markerColor = colors.outline, accentColor = colors.primary)
    }

    // После вставки фото курсор переходит под него — но только если клавиатура уже была в работе
    LaunchedEffect(state.focusRequestKey) {
        val key = state.focusRequestKey ?: return@LaunchedEffect
        val block = state.blocks.firstOrNull { it.key == key } as? EditorState.TextBlock
        if (block != null && state.hasFocus) {
            withFrameNanos { } // дождаться, пока новый блок появится на экране
            block.focusRequester.requestFocus()
        }
        state.consumeFocusRequest()
    }

    // «Назад» сначала снимает выделение с фото, а не закрывает экран
    BackHandler(enabled = state.selectedPhotoKey != null) { state.selectPhoto(null) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.blocks.forEachIndexed { index, block ->
            // key — чтобы при вставке фото поля ввода не путали своё состояние (курсор, фокус)
            key(block.key) {
                when (block) {
                    is EditorState.TextBlock -> TextBlockField(
                        block = block,
                        state = state,
                        style = style,
                        highlighter = highlighter,
                        // Подсказка только в пустом документе
                        placeholder = placeholder.takeIf { index == 0 && state.isEmpty },
                    )
                    is EditorState.PhotoBlock -> PhotoBlockView(
                        block = block,
                        state = state,
                        photoFile = photoFile,
                        onOpen = { i -> onOpenPhotos(block.photoIds, i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TextBlockField(
    block: EditorState.TextBlock,
    state: EditorState,
    style: TextStyle,
    highlighter: MarkdownHighlighter,
    placeholder: String?,
) {
    BasicTextField(
        value = block.value,
        onValueChange = { state.onTextChange(block, it) },
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = highlighter,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(block.focusRequester)
            .onFocusChanged { state.onTextFocusChanged(block, it.isFocused) },
        decorationBox = { inner ->
            Box {
                if (placeholder != null && block.value.text.isEmpty()) {
                    Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.outline))
                }
                inner()
            }
        },
    )
}

/** Фото-блок: одно фото или сетка, с выравниванием, щипком и панелью действий. */
@Composable
private fun PhotoBlockView(
    block: EditorState.PhotoBlock,
    state: EditorState,
    photoFile: (String) -> File,
    onOpen: (index: Int) -> Unit,
) {
    val selected = state.selectedPhotoKey == block.key
    val colors = MaterialTheme.colorScheme

    // Ширина во время щипка — плавная; в документ записывается при отпускании
    var pinchWidth by remember { mutableFloatStateOf(-1f) }
    val animatedWidth by animateFloatAsState(block.widthPercent.toFloat(), label = "photoWidth")
    val width = if (pinchWidth > 0) pinchWidth else animatedWidth

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = block.align.toAlignment()) {
            Box(
                Modifier
                    .fillMaxWidth(width / 100f)
                    .then(if (selected) Modifier.border(3.dp, colors.primary, PhotoShape) else Modifier)
                    .padding(if (selected) 3.dp else 0.dp)
                    .pinchToResize(block, state) { pinchWidth = it },
            ) {
                if (block.isGrid) {
                    PhotoGrid(block, state, photoFile, onOpen)
                } else {
                    PhotoCell(
                        file = photoFile(block.photoIds.first()),
                        shape = PhotoShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(state.aspectOf(block.photoIds.first()))
                            .photoTaps(block, 0, state, onOpen),
                    )
                }
            }
        }

        AnimatedVisibility(selected) {
            PhotoActions(block, state, onOpen = { onOpen(state.selectedPhotoIndex) })
        }
    }
}

/** Сетка: 2 фото — в ряд, 3 — в ряд, 4 — квадрат 2×2, больше — по 3 в ряд. */
@Composable
private fun PhotoGrid(
    block: EditorState.PhotoBlock,
    state: EditorState,
    photoFile: (String) -> File,
    onOpen: (index: Int) -> Unit,
) {
    val ids = block.photoIds
    val columns = if (ids.size == 2 || ids.size == 4) 2 else 3
    val markSelected = state.selectedPhotoKey == block.key

    Column(verticalArrangement = Arrangement.spacedBy(GridSpacing)) {
        ids.chunked(columns).forEachIndexed { row, rowIds ->
            Row(horizontalArrangement = Arrangement.spacedBy(GridSpacing)) {
                rowIds.forEachIndexed { column, id ->
                    val index = row * columns + column
                    PhotoCell(
                        file = photoFile(id),
                        shape = CellShape,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .then(
                                // В выделенной сетке отмечаем фото, к которому относятся «корзина» и «на весь экран»
                                if (markSelected && state.selectedPhotoIndex == index) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, CellShape)
                                } else {
                                    Modifier
                                },
                            )
                            .photoTaps(block, index, state, onOpen),
                    )
                }
                // Неполный последний ряд: пустые места, чтобы ячейки были того же размера
                repeat(columns - rowIds.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PhotoCell(file: File, shape: RoundedCornerShape, modifier: Modifier) {
    Box(modifier.clip(shape)) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * Тап — открыть/закрыть панель (повторный тап по тому же фото закрывает),
 * двойной тап — на весь экран, долгое нажатие — открыть панель.
 */
@Composable
private fun Modifier.photoTaps(
    block: EditorState.PhotoBlock,
    index: Int,
    state: EditorState,
    onOpen: (Int) -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(block.key, index) {
        detectTapGestures(
            // Состояние читается в момент тапа, а не при создании обработчика —
            // иначе повторный тап «не видел» бы, что панель уже открыта
            onTap = { state.togglePhoto(block.key, index) },
            onDoubleTap = { onOpen(index) },
            onLongPress = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                state.selectPhoto(block.key, index)
            },
        )
    }
}

/** Щипок двумя пальцами меняет размер блока; одним пальцем — обычная прокрутка. */
private fun Modifier.pinchToResize(
    block: EditorState.PhotoBlock,
    state: EditorState,
    onPinch: (Float) -> Unit,
): Modifier = pointerInput(block.key) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var current = block.widthPercent.toFloat()
        var pinched = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } >= 2) {
                current = (current * event.calculateZoom())
                    .coerceIn(DocumentBlock.MIN_WIDTH.toFloat(), DocumentBlock.FULL_WIDTH.toFloat())
                pinched = true
                onPinch(current)
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (pinched) {
            // Округляем до 5 %, чтобы размеры не были «кривыми»
            state.setPhotoWidth(block.key, (current / 5).roundToInt() * 5)
            onPinch(-1f)
        }
    }
}

@Composable
private fun PhotoActions(
    block: EditorState.PhotoBlock,
    state: EditorState,
    onOpen: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        // Несколько строк: в одну на узком экране всё не помещается
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SingleChoiceSegmentedButtonRow {
                WidthPresets.forEachIndexed { i, preset ->
                    SegmentedButton(
                        selected = block.widthPercent == preset,
                        onClick = { state.setPhotoWidth(block.key, preset) },
                        shape = SegmentedButtonDefaults.itemShape(i, WidthPresets.size),
                        icon = {},
                        label = { Text("$preset%") },
                    )
                }
            }
            SingleChoiceSegmentedButtonRow {
                AlignOptions.forEachIndexed { i, (align, icon, label) ->
                    SegmentedButton(
                        selected = block.align == align,
                        onClick = { state.setPhotoAlign(block.key, align) },
                        shape = SegmentedButtonDefaults.itemShape(i, AlignOptions.size),
                        icon = {},
                        label = { Icon(icon, stringResource(label)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { state.movePhoto(block.key, up = true) },
                    enabled = state.canMovePhoto(block.key, up = true),
                ) {
                    Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.editor_photo_up))
                }
                IconButton(
                    onClick = { state.movePhoto(block.key, up = false) },
                    enabled = state.canMovePhoto(block.key, up = false),
                ) {
                    Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.editor_photo_down))
                }
                IconButton(onClick = onOpen) {
                    Icon(Icons.Outlined.Fullscreen, stringResource(R.string.editor_photo_open))
                }
                FilledTonalIconButton(onClick = { state.removePhotoAt(block.key, state.selectedPhotoIndex) }) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.editor_photo_delete))
                }
            }
        }
    }
}

private data class AlignOption(val align: PhotoAlign, val icon: ImageVector, val label: Int)

private val AlignOptions = listOf(
    AlignOption(PhotoAlign.Start, Icons.AutoMirrored.Outlined.FormatAlignLeft, R.string.editor_align_left),
    AlignOption(PhotoAlign.Center, Icons.Outlined.FormatAlignCenter, R.string.editor_align_center),
    AlignOption(PhotoAlign.End, Icons.AutoMirrored.Outlined.FormatAlignRight, R.string.editor_align_right),
)

private fun PhotoAlign.toAlignment(): Alignment = when (this) {
    PhotoAlign.Start -> Alignment.TopStart
    PhotoAlign.Center -> Alignment.TopCenter
    PhotoAlign.End -> Alignment.TopEnd
}
