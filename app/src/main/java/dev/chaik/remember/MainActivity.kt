package dev.chaik.remember

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.chaik.remember.app.ReMemberApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Без полупрозрачной подложки под кнопками навигации — контент виден до самого низа
        window.isNavigationBarContrastEnforced = false
        setContent {
            ReMemberApp()
        }
    }
}
