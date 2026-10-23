package dev.chaik.remember.feature.settings.data

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.chaik.remember.R
import dev.chaik.remember.app.AppContainer
import dev.chaik.remember.core.backup.NewerBackupException
import dev.chaik.remember.core.backup.NotBackupException
import dev.chaik.remember.core.backup.RestorePreview
import dev.chaik.remember.core.backup.StorageUsage
import dev.chaik.remember.core.backup.WrongPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Что сейчас происходит с копией. */
sealed interface BackupTask {
    data class Working(@param:StringRes val message: Int) : BackupTask
    data class Exported(val bytes: Long) : BackupTask
    data class Failed(@param:StringRes val message: Int) : BackupTask
    data class ReadyToRestore(val preview: RestorePreview) : BackupTask
}

/** Данные и резервные копии: место на телефоне, экспорт, восстановление. */
class DataViewModel(private val container: AppContainer) : ViewModel() {
    private val backup = container.backupManager

    var usage by mutableStateOf<StorageUsage?>(null)
        private set
    var task by mutableStateOf<BackupTask?>(null)
        private set

    /** Пароль копии между окном пароля и выбором файла. */
    private var pendingPassword: CharArray? = null

    init {
        refreshUsage()
    }

    fun refreshUsage() {
        viewModelScope.launch { usage = backup.usage() }
    }

    fun rememberPassword(password: CharArray) {
        pendingPassword = password
    }

    /** Выбор файла отменили — пароль больше не нужен. */
    fun forgetPassword() {
        pendingPassword?.fill(' ')
        pendingPassword = null
    }

    fun export(target: Uri) {
        val password = pendingPassword ?: return
        pendingPassword = null
        task = BackupTask.Working(R.string.data_exporting)
        // В фоне приложения: копия допишется, даже если страницу закрыли
        container.appScope.launch {
            task = try {
                BackupTask.Exported(backup.export(target, password))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Экспорт не удался", e)
                BackupTask.Failed(R.string.data_export_failed)
            } finally {
                password.fill(' ')
            }
        }
    }

    fun prepareRestore(source: Uri, password: CharArray) {
        task = BackupTask.Working(R.string.data_checking)
        viewModelScope.launch {
            task = try {
                BackupTask.ReadyToRestore(backup.prepareRestore(source, password))
            } catch (e: CancellationException) {
                throw e
            } catch (_: WrongPasswordException) {
                BackupTask.Failed(R.string.data_wrong_password)
            } catch (_: NotBackupException) {
                BackupTask.Failed(R.string.data_not_backup)
            } catch (_: NewerBackupException) {
                BackupTask.Failed(R.string.data_newer_backup)
            } catch (e: Exception) {
                Log.w(TAG, "Копия не читается", e)
                BackupTask.Failed(R.string.data_broken_backup)
            } finally {
                password.fill(' ')
            }
        }
    }

    /** Подтвердили замену: копия встанет на место при перезапуске. */
    fun confirmRestore() = backup.markRestoreReady()

    fun dismiss() {
        if (task is BackupTask.ReadyToRestore) backup.cancelRestore()
        task = null
    }

    private companion object {
        const val TAG = "Backup"
    }
}
