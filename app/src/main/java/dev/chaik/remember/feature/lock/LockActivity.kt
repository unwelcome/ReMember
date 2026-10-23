package dev.chaik.remember.feature.lock

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.fragment.app.FragmentActivity
import dev.chaik.remember.R
import dev.chaik.remember.appContainer
import dev.chaik.remember.core.security.BiometricUnlock
import dev.chaik.remember.core.ui.theme.ReMemberTheme

/**
 * Экран блокировки — отдельным окном поверх приложения. Так он закрывает и открытые
 * диалоги (просмотр фото, календарь), а под ним сохраняется всё, что было открыто.
 */
class LockActivity : FragmentActivity() {
    private val lock get() = appContainer.appLock
    private var biometricShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Уже разблокировано (например, вернулись к старой копии экрана) — сразу закрываемся
        if (!lock.locked.value) return finishQuietly()

        val canUseBiometric = lock.biometricEnabled && BiometricUnlock.isAvailable(this)
        setContent {
            ReMemberTheme {
                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                    // «Назад» на экране блокировки — свернуть приложение, а не пройти мимо замка
                    BackHandler { moveTaskToBack(true) }
                    LockScreen(
                        lock = lock,
                        onUnlocked = ::finishQuietly,
                        onBiometric = if (canUseBiometric) ::showBiometric else null,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Окно отпечатка — сразу при появлении замка, один раз; повторно — кнопкой с отпечатком
        if (!biometricShown && lock.locked.value && lock.biometricEnabled && BiometricUnlock.isAvailable(this)) {
            biometricShown = true
            showBiometric()
        }
    }

    private fun showBiometric() {
        BiometricUnlock.prompt(
            activity = this,
            title = getString(R.string.lock_biometric_title),
            negativeButton = getString(R.string.lock_biometric_use_pin),
            onSuccess = {
                lock.unlockWithBiometric()
                finishQuietly()
            },
        )
    }

    private fun finishQuietly() {
        finish()
        // Замок плавно исчезает, открывая приложение под ним
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    companion object {
        /** Показать замок поверх [activity] без анимации появления — чтобы содержимое не мелькнуло. */
        fun show(activity: Activity) {
            val intent = Intent(activity, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            activity.startActivity(intent, ActivityOptions.makeCustomAnimation(activity, 0, 0).toBundle())
        }
    }
}
