# ReMember

Личный дневник для Android: записи о том, как прошёл день, с фото, сон, траты, заметки, задачи, календарь событий, зал и статистика.

Делается только для себя — без Google Play и серверов, все данные хранятся локально на телефоне в зашифрованном виде.

## Стек

- Kotlin, Jetpack Compose (Material 3)
- Room + SQLCipher, DataStore
- minSdk 33 (Android 13), targetSdk 37

## Сборка и запуск

Нужны Android Studio и телефон с включённой отладкой по USB или Wi-Fi.

Из Android Studio — кнопка **Run**. Из консоли:

```bash
./gradlew installDebug
```

## Документы

- [PLAN.md](PLAN.md) — поэтапный план работ и технические решения.
