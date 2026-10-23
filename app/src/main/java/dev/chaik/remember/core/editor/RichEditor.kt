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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * Редактор «текст + фото». Сам не прокручивается — кладётся в прокручиваемый контейнер экрана.
 *
 * @param photoFile файл фото по id (полноразмерный — Coil сам уменьшит под ширину экрана)
 * @param onOpenPhoto открыть фото на весь экран
 */
@Composable
fun RichEditor(
    state: EditorState,
    photoFile: (String) -> File,
    onOpenPhoto: (String) -> Unit,
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
            androidx.compose.runtime.key(block.key) {
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
                        file = photoFile(block.photoId),
                        onOpen = { onOpenPhoto(block.photoId) },
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

@Composable
private fun PhotoBlockView(
    block: EditorState.PhotoBlock,
    state: EditorState,
    file: File,
    onOpen: () -> Unit,
) {
    val selected = state.selectedPhotoKey == block.key
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme

    // Ширина во время щипка — плавная; в документ записывается при отпускании
    var pinchWidth by remember { mutableFloatStateOf(-1f) }
    val width = if (pinchWidth > 0) pinchWidth else block.widthPercent.toFloat()
    val animatedWidth by animateFloatAsState(width, label = "photoWidth")

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth((if (pinchWidth > 0) width else animatedWidth) / 100f)
                .aspectRatio(block.aspectRatio)
                .clip(PhotoShape)
                .then(if (selected) Modifier.border(3.dp, colors.primary, PhotoShape) else Modifier)
                .pointerInput(block.key) {
                    detectTapGestures(
                        onTap = { state.selectPhoto(if (selected) null else block.key) },
                        onDoubleTap = { onOpen() },
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            state.selectPhoto(block.key)
                        },
                    )
                }
                .pointerInput(block.key) {
                    // Щипок двумя пальцами меняет размер; одним пальцем — обычная прокрутка
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var current = block.widthPercent.toFloat()
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } >= 2) {
                                current = (current * event.calculateZoom())
                                    .coerceIn(DocumentBlock.MIN_WIDTH.toFloat(), DocumentBlock.FULL_WIDTH.toFloat())
                                pinchWidth = current
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        if (pinchWidth > 0) {
                            // Округляем до 5 %, чтобы размеры не были «кривыми»
                            state.setPhotoWidth(block.key, (current / 5).roundToInt() * 5)
                            pinchWidth = -1f
                        }
                    }
                },
        ) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        AnimatedVisibility(selected) {
            PhotoActions(block, state, onOpen)
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
        // Две строки: в одну на узком экране всё не помещается
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                FilledTonalIconButton(onClick = { state.removePhoto(block.key) }) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.editor_photo_delete))
                }
            }
        }
    }
}

private val PhotoShape = RoundedCornerShape(16.dp)
