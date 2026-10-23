package dev.chaik.remember.core.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chaik.remember.core.settings.LocalAppSettings
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Пункт кругового меню мастеркнопки. [enabled] = false — «текущий», тёмный и неактивный. */
data class MasterButtonItem<T>(
    val key: T,
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
)

// Геометрия веера — кольцо вокруг кнопки (радиусы от центра мастеркнопки)
private val InnerRadius = 64.dp
private val OuterRadius = 150.dp
private val ItemSize = 47.dp

/** Насколько сектор под пальцем «вырастает» наружу. */
private val HoverGrow = 12.dp
private val DividerWidth = 2.dp

/** Цвета веера: очень тёмный синий, сектор под пальцем — чуть светлее, разделители — синие. */
private val FanColor = Color(0xFF0A1330)
private val FanHoverColor = Color(0xFF1B2B5E)
private val DividerColor = Color(0xFF2E4596)

/** Пункты стоят на 180°…90°; сектора выступают на полшага за края — как на макете. */
private const val FIRST_DEGREES = 180.0
private const val LAST_DEGREES = 90.0

private const val OPEN_MILLIS = 240
private const val STAGGER_MILLIS = 50

/** Нажатие короче этого и без сдвига — «тап»: меню остаётся открытым и ждёт выбора. */
private const val TAP_MILLIS = 250L

/**
 * Мастеркнопка — квадратная кнопка в правом нижнем углу с круговым меню.
 *
 * Меню — кольцо-веер вокруг кнопки от 180° до 90°, разделённое на сектора, по пункту в каждом.
 * Появляется сразу при касании кнопки:
 * - коротко нажал и отпустил — меню остаётся, выбираешь тапом по пункту
 *   (тап мимо, по кнопке или «назад» — закрыть);
 * - ведёшь палец к сектору — он подрастает, отпускаешь — пункт выбран; отпустил мимо — закрылось.
 *
 * Пункты [items] раскладываются по дуге по порядку: первый — слева (180°), последний — сверху (90°).
 * Вызывать внутри Box на весь экран: компонент сам ставит кнопку в угол и рисует меню поверх.
 */
@Composable
fun <T> BoxScope.MasterButton(
    icon: ImageVector,
    contentDescription: String,
    items: List<MasterButtonItem<T>>,
    onSelect: (T) -> Unit,
    visible: Boolean,
    padding: PaddingValues,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Режим левши: кнопка слева, веер раскрывается вправо-вверх (углы зеркалятся: x → 180° − x)
    val mirrored = LocalAppSettings.current.leftHanded
    fun deg(value: Double) = if (mirrored) 180.0 - value else value
    val select by rememberUpdatedState(onSelect)
    val currentItems by rememberUpdatedState(items)

    var open by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf<Int?>(null) }
    // Центр кнопки в координатах контейнера — вокруг него строится меню
    var center by remember { mutableStateOf(Offset.Zero) }
    var buttonTopLeft by remember { mutableStateOf(Offset.Zero) }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(open) {
        progress.animateTo(if (open) 1f else 0f, tween(OPEN_MILLIS, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(visible) { if (!visible) open = false }
    BackHandler(enabled = open) { open = false }

    fun close() {
        open = false
        hovered = null
    }

    fun choose(index: Int) {
        val item = currentItems.getOrNull(index) ?: return
        close()
        if (item.enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            select(item.key)
        }
    }

    val geometry = FanGeometry(currentItems.size)

    /** Какой сектор под пальцем: по углу, если палец вышел за кнопку и не ушёл слишком далеко. */
    fun itemAt(point: Offset): Int? {
        val dx = point.x - center.x
        val dy = center.y - point.y
        val distance = sqrt(dx * dx + dy * dy)
        val min = with(density) { (InnerRadius * 0.6f).toPx() }
        val max = with(density) { (OuterRadius + 40.dp).toPx() }
        if (distance !in min..max) return null
        var degrees = deg(atan2(dy, dx) * 180 / PI)
        if (degrees < -90) degrees += 360
        if (degrees > 270) degrees -= 360
        return geometry.indexAt(degrees)
    }

    // Меню поверх содержимого: только пока открыто или анимируется
    if (progress.value > 0f || open) {
        val ringColor = FanColor.copy(alpha = 0.94f)
        val hoverColor = FanHoverColor.copy(alpha = 0.97f)
        val dividerColor = DividerColor
        val growth = currentItems.indices.map { index ->
            animateFloatAsState(
                targetValue = if (hovered == index && currentItems[index].enabled) 1f else 0f,
                animationSpec = spring(),
                label = "sectorGrow",
            ).value
        }

        Box(
            Modifier
                .matchParentSize()
                // Тап мимо пунктов закрывает меню; фон не затемняем
                .pointerInput(Unit) { detectTapGestures { close() } },
        ) {
            Canvas(Modifier.matchParentSize()) {
                val scale = progress.value
                val inner = InnerRadius.toPx() * scale
                val outer = OuterRadius.toPx() * scale
                currentItems.indices.forEach { index ->
                    val grow = HoverGrow.toPx() * growth[index]
                    drawSector(
                        center = center,
                        inner = (inner - grow / 3).coerceAtLeast(0f),
                        outer = outer + grow,
                        fromDegrees = deg(geometry.sectorStart(index)),
                        toDegrees = deg(geometry.sectorEnd(index)),
                        color = if (growth[index] > 0f) lerp(ringColor, hoverColor, growth[index]) else ringColor,
                    )
                }
                // Разделители — на всех границах секторов, включая края веера
                (0..currentItems.size).forEach { boundary ->
                    val angle = deg(geometry.boundary(boundary)) * PI / 180
                    drawLine(
                        color = dividerColor,
                        start = center + polar(inner, angle),
                        end = center + polar(outer, angle),
                        strokeWidth = DividerWidth.toPx(),
                        cap = StrokeCap.Butt,
                    )
                }
            }
            currentItems.forEachIndexed { index, item ->
                FanItem(
                    item = item,
                    hovered = hovered == index,
                    // Пункты появляются по очереди: у каждого своя задержка
                    progress = itemProgress(progress.value, index),
                    position = {
                        val radius = with(density) { ((InnerRadius + OuterRadius) / 2).toPx() } * progress.value
                        center + polar(radius, deg(geometry.itemDegrees(index)) * PI / 180)
                    },
                    onClick = { choose(index) },
                )
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = Modifier
            .align(if (mirrored) Alignment.BottomStart else Alignment.BottomEnd)
            .padding(padding)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInParent()
                buttonTopLeft = bounds.topLeft
                center = bounds.center
            },
    ) {
        // Не FloatingActionButton: у него свой обработчик нажатий, а нам нужны и тап, и слайд
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(56.dp)
                // Пока веер открыт, кнопка слегка увеличена
                .graphicsLayer {
                    val grow = 1f + 0.12f * progress.value
                    scaleX = grow
                    scaleY = grow
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val wasOpen = open
                        // Меню появляется сразу — без ожидания долгого нажатия
                        if (!wasOpen) {
                            open = true
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                        val slop = viewConfiguration.touchSlop
                        var moved = false
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if ((change.position - down.position).getDistance() > slop) moved = true
                            val under = if (moved) itemAt(buttonTopLeft + change.position) else null
                            if (under != hovered) {
                                hovered = under
                                if (under != null && currentItems[under].enabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            }
                            if (!change.pressed) {
                                val chosen = hovered
                                val isTap = !moved && change.uptimeMillis - down.uptimeMillis < TAP_MILLIS
                                when {
                                    chosen != null && currentItems[chosen].enabled -> choose(chosen)
                                    // Тап по закрытой кнопке — меню остаётся и ждёт выбора тапом
                                    isTap && !wasOpen -> hovered = null
                                    // Тап по открытой кнопке или отпустили мимо после слайда — закрыть
                                    else -> close()
                                }
                                break
                            }
                        }
                    }
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Crossfade(targetState = icon, label = "masterIcon") {
                    Icon(it, contentDescription)
                }
            }
        }
    }
}

/**
 * Углы веера (в градусах, 0° — вправо, 90° — вверх): пункты на дуге 180°…90°,
 * сектора шириной в шаг между пунктами, крайние выступают за 180° и 90° на полшага.
 */
private class FanGeometry(private val count: Int) {
    private val step = if (count <= 1) 90.0 else (FIRST_DEGREES - LAST_DEGREES) / (count - 1)

    fun itemDegrees(index: Int): Double =
        if (count <= 1) (FIRST_DEGREES + LAST_DEGREES) / 2 else FIRST_DEGREES - index * step

    /** Граница перед сектором [index] (0) … после последнего ([count]). */
    fun boundary(index: Int): Double = itemDegrees(0) + step / 2 - index * step

    fun sectorStart(index: Int): Double = boundary(index)

    fun sectorEnd(index: Int): Double = boundary(index + 1)

    /** Сектор по углу; немного прощаем промах за краями веера. */
    fun indexAt(degrees: Double): Int? {
        val position = (boundary(0) - degrees) / step
        if (position < -0.25 || position > count + 0.25) return null
        return position.toInt().coerceIn(0, count - 1)
    }
}

/** Кольцевой сектор между углами (математическими, против часовой) [fromDegrees] → [toDegrees]. */
private fun DrawScope.drawSector(
    center: Offset,
    inner: Float,
    outer: Float,
    fromDegrees: Double,
    toDegrees: Double,
    color: Color,
) {
    // В Canvas углы идут по часовой, поэтому знак меняется
    val start = (-fromDegrees).toFloat()
    val sweep = (fromDegrees - toDegrees).toFloat()
    val path = Path().apply {
        arcTo(Rect(center, outer), start, sweep, forceMoveTo = true)
        arcTo(Rect(center, inner), start + sweep, -sweep, forceMoveTo = false)
        close()
    }
    drawPath(path, color)
}

private fun polar(radius: Float, radians: Double) =
    Offset((radius * cos(radians)).toFloat(), -(radius * sin(radians)).toFloat())

private fun lerp(from: Color, to: Color, fraction: Float) =
    androidx.compose.ui.graphics.lerp(from, to, fraction)

/** Прогресс появления пункта с задержкой «по очереди». */
private fun itemProgress(total: Float, index: Int): Float {
    val delay = index * STAGGER_MILLIS / OPEN_MILLIS.toFloat()
    return ((total - delay) / (1f - delay)).coerceIn(0f, 1f)
}

@Composable
private fun <T> FanItem(
    item: MasterButtonItem<T>,
    hovered: Boolean,
    progress: Float,
    position: () -> Offset,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (hovered && item.enabled) 1.15f else 1f,
        animationSpec = spring(),
        label = "hover",
    )
    var width by remember { mutableIntStateOf(0) }
    val contentColor = if (item.enabled) colors.onPrimaryContainer else colors.onSurface.copy(alpha = 0.38f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onSizeChanged { width = it.width }
            // Центр кружка — ровно в точке на дуге, подпись — под ним
            .offset {
                val point = position()
                val half = ItemSize.roundToPx() / 2
                IntOffset(point.x.roundToInt() - width / 2, point.y.roundToInt() - half)
            }
            .graphicsLayer {
                alpha = progress
                scaleX = scale * (0.5f + 0.5f * progress)
                scaleY = scale * (0.5f + 0.5f * progress)
                transformOrigin = TransformOrigin(0.5f, 0.3f)
            },
    ) {
        Surface(
            onClick = onClick,
            enabled = item.enabled,
            shape = CircleShape,
            color = if (item.enabled) colors.primaryContainer else colors.surfaceContainerLowest,
            contentColor = contentColor,
            modifier = Modifier.size(ItemSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null)
            }
        }
        Text(
            text = item.label,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = if (item.enabled) Color.White else Color.White.copy(alpha = 0.45f),
            maxLines = 1,
            // Подпись вплотную к кружку
            modifier = Modifier.offset(y = (-1).dp),
        )
    }
}
