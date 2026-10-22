package dev.chaik.remember

import android.app.Application
import android.content.Context
import dev.chaik.remember.app.AppContainer

class ReMemberApplication : Application() {
    val container by lazy { AppContainer(this) }
}

/** Контейнер зависимостей из любого места, где есть Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as ReMemberApplication).container
