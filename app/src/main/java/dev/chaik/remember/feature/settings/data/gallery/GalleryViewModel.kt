package dev.chaik.remember.feature.settings.data.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.chaik.remember.app.AppContainer
import dev.chaik.remember.core.photos.LibraryPhoto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Галерея всех фото: список и действия с фото. */
class GalleryViewModel(container: AppContainer) : ViewModel() {
    private val library = container.photoLibrary

    /** null — ещё грузится. */
    val photos: StateFlow<List<LibraryPhoto>?> =
        library.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(photoId: String) {
        viewModelScope.launch { library.delete(photoId) }
    }

    fun exportToGallery(photoId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(library.exportToGallery(photoId)) }
    }
}
