package dev.chaik.remember

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.chaik.remember.app.ReMemberApp
import dev.chaik.remember.feature.lock.LockActivity

class MainActivity : ComponentActivity() {
    private val lock get() = appContainer.appLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Без полупрозрачной подложки под кнопками навигации — контент виден до самого низа
        window.isNavigationBarContrastEnforced = false
        setContent {
            ReMemberApp(lock)
        }
    }

    override fun onStart() {
        super.onStart()
        // Вернулись из фона: если прошло больше таймаута — снова замок
        lock.onForeground()
        if (lock.locked.value) LockActivity.show(this)
    }
}
