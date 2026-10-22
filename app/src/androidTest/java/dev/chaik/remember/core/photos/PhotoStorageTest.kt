package dev.chaik.remember.core.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chaik.remember.core.database.ReMemberDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration

@RunWith(AndroidJUnit4::class)
class PhotoStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: ReMemberDatabase
    private lateinit var storage: PhotoStorage
    private val created = mutableListOf<String>()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, ReMemberDatabase::class.java).build()
        storage = PhotoStorage(context, db.photoDao())
    }

    @After
    fun tearDown() {
        created.forEach {
            storage.file(it).delete()
            storage.thumbnailFile(it).delete()
        }
        db.close()
    }

    /** «Снимок с камеры» 4000×3000 во временном файле. */
    private fun sourcePhoto(): Uri {
        val bitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 120, 40)) }
        val file = File(context.cacheDir, "source.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun importScalesDownAndMakesThumbnail() = runTest {
        val photo = storage.import(sourcePhoto()).also { created += it.id }

        assertEquals(2560, photo.width)
        assertEquals(1920, photo.height)
        assertTrue(storage.file(photo.id).exists())

        val thumb = BitmapFactory.decodeFile(storage.thumbnailFile(photo.id).path)
        assertEquals(480, thumb.width)
        assertEquals(360, thumb.height)
    }

    @Test
    fun cleanupRemovesUnreferencedPhotos() = runTest {
        val photo = storage.import(sourcePhoto()).also { created += it.id }

        // По умолчанию свежие фото не трогаются — их могли только что вставить в редактор
        assertEquals(0, storage.cleanupUnreferenced())
        assertTrue(storage.file(photo.id).exists())

        assertEquals(1, storage.cleanupUnreferenced(olderThan = Duration.ZERO))
        assertFalse(storage.file(photo.id).exists())
        assertFalse(storage.thumbnailFile(photo.id).exists())
    }
}
