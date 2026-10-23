package dev.chaik.remember.core.health

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ActivityMathTest {
    private val day = Instant.parse("2026-10-23T00:00:00Z")
    private val end = day.plusSeconds(86_400)
    private fun at(minute: Long): Instant = day.plusSeconds(minute * 60)
    private fun span(from: Long, to: Long) = TimeSpan(at(from), at(to))

    @Test
    fun `бодрая ходьба считается, прогулка по дому — нет`() {
        val steps = listOf(
            StepsSpan(span(600, 610), 1_000), // 100 шагов в минуту — активно
            StepsSpan(span(700, 710), 200), // 20 в минуту — нет
        )
        assertEquals(10, ActivityMath.activeMinutes(steps, emptyList(), day, end))
    }

    @Test
    fun `тренировка и ходьба в одно время не считаются дважды`() {
        val steps = listOf(StepsSpan(span(600, 630), 3_000))
        val gym = listOf(span(620, 680))
        assertEquals(80, ActivityMath.activeMinutes(steps, gym, day, end))
    }

    @Test
    fun `только внутри суток`() {
        val lateWalk = listOf(span(-30, 30))
        assertEquals(30, ActivityMath.activeMinutes(emptyList(), lateWalk, day, end))
    }

    @Test
    fun `короткие куски шагов — по темпу в секундах`() {
        // 15 шагов за 9 секунд — 100 в минуту
        val short = listOf(StepsSpan(TimeSpan(at(600), at(600).plusSeconds(9)), 15))
        assertEquals(1, ActivityMath.activeMinutes(short, emptyList(), day, end))
    }

    @Test
    fun `калории по шагам и тренировке`() {
        // 5000 шагов при 66 кг — около 169 ккал
        assertEquals(168.7, ActivityMath.estimateCalories(5000, 0, 66.0), 0.1)
        // Час тренировки без ходьбы — ещё 264
        assertEquals(432.7, ActivityMath.estimateCalories(5000, 60, 66.0), 0.1)
        val time = ActivityMath.activeTime(listOf(StepsSpan(span(600, 620), 2_000)), listOf(span(610, 670)), day, end)
        assertEquals(70, time.total)
        assertEquals(50, time.workoutOnly)
    }
}
