package dev.chaik.remember.core.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    private class MemoryStore : LockStore {
        override var pinHash: String? = null
        override var failedAttempts = 0
        override var lockoutUntil = 0L
        override var backgroundSince = 0L
    }

    private var now = 1_000_000L
    private val store = MemoryStore()
    private val lock = AppLock(store, clock = { now }, enabled = true)

    @Test
    fun `хэш PIN проверяется и не совпадает с другим PIN`() {
        val hash = PinHasher.hash("1111")
        assertTrue(PinHasher.verify("1111", hash))
        assertFalse(PinHasher.verify("1112", hash))
        // Соль случайная: один PIN — разные хэши
        assertNotEquals(hash, PinHasher.hash("1111"))
        assertFalse(hash.contains("1111"))
    }

    @Test
    fun `старый хэш пересчитывается после верного PIN`() = runTest {
        store.pinHash = PinHasher.hash(AppLock.DEFAULT_PIN, iterations = 1_000)
        assertTrue(PinHasher.needsRehash(store.pinHash!!))

        assertEquals(PinResult.Unlocked, lock.unlockWithPin(AppLock.DEFAULT_PIN))

        assertFalse(PinHasher.needsRehash(store.pinHash!!))
        assertTrue(PinHasher.verify(AppLock.DEFAULT_PIN, store.pinHash!!))
    }

    @Test
    fun `выключенная блокировка не запирает ни при запуске, ни после фона`() {
        val off = AppLock(MemoryStore(), clock = { now }, enabled = false)
        assertFalse(off.locked.value)
        off.onBackground()
        now += AppLock.DEFAULT_TIMEOUT_MILLIS * 2
        off.onForeground()
        assertFalse(off.locked.value)
    }

    @Test
    fun `при запуске заблокировано, PIN по умолчанию открывает`() = runTest {
        assertTrue(lock.locked.value)
        assertEquals(PinResult.Unlocked, lock.unlockWithPin(AppLock.DEFAULT_PIN))
        assertFalse(lock.locked.value)
    }

    @Test
    fun `неверный PIN — попытки, потом пауза`() = runTest {
        repeat(AppLock.MAX_ATTEMPTS - 1) { i ->
            assertEquals(PinResult.Wrong(AppLock.MAX_ATTEMPTS - 1 - i), lock.unlockWithPin("0000"))
        }
        val lockedOut = lock.unlockWithPin("0000")
        assertTrue(lockedOut is PinResult.LockedOut)
        // Во время паузы не открывает даже верный PIN
        assertTrue(lock.unlockWithPin(AppLock.DEFAULT_PIN) is PinResult.LockedOut)

        now += AppLock.LOCKOUT_MILLIS
        assertEquals(PinResult.Unlocked, lock.unlockWithPin(AppLock.DEFAULT_PIN))
    }

    @Test
    fun `блокировка после долгого фона, но не после короткого`() = runTest {
        lock.unlockWithBiometric()

        lock.onBackground()
        now += AppLock.DEFAULT_TIMEOUT_MILLIS - 1
        lock.onForeground()
        assertFalse(lock.locked.value)

        lock.onBackground()
        now += AppLock.DEFAULT_TIMEOUT_MILLIS
        lock.onForeground()
        assertTrue(lock.locked.value)
    }
}
