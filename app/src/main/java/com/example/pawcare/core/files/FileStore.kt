package com.example.pawcare.core.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class FileImportException : IOException()
data class ImportedFile(val path: String, val name: String, val mime: String, val size: Long)
@Singleton
class FileStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val root get() = File(context.filesDir, "media").also { check(it.exists() || it.mkdirs()) }
    fun file(path: String): File {
        require(!path.contains('/') && !path.contains('\\') && path != "." && path != "..")
        return File(root, path)
    }
    fun uri(path: String): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file(path))
    suspend fun cleanup(referenced: Set<String>, before: Long) = withContext(Dispatchers.IO) {
        root.listFiles()?.filter { it.name !in referenced && it.lastModified() < before }?.forEach { it.delete() }
    }
    suspend fun delete(path: String) = withContext(Dispatchers.IO) { file(path).delete(); Unit }
    suspend fun import(uri: Uri, photoOnly: Boolean = false): ImportedFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: throw FileImportException()
        if ((!mime.startsWith("image/") && mime != "application/pdf") || (photoOnly && !mime.startsWith("image/"))) throw FileImportException()
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "document"
        val raw = File(root, "${UUID.randomUUID()}.tmp")
        var destination: File? = null
        try {
            resolver.openInputStream(uri)?.use { input ->
                raw.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 20L * 1024 * 1024) throw FileImportException()
                        output.write(buffer, 0, count)
                    }
                    if (total == 0L) throw FileImportException()
                }
            } ?: throw FileImportException()
            if (mime == "application/pdf") {
                val header = raw.inputStream().use { input -> ByteArray(5).also { java.io.DataInputStream(input).readFully(it) }.toString(Charsets.US_ASCII) }
                if (header != "%PDF-") throw FileImportException()
                destination = File(root, "${UUID.randomUUID()}.pdf")
                check(raw.renameTo(destination))
                ImportedFile(destination.name, name, mime, destination.length())
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(raw.path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw FileImportException()
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
                val bitmap = BitmapFactory.decodeFile(raw.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: throw FileImportException()
                val exif = ExifInterface(raw)
                val matrix = Matrix().apply {
                    if (exif.isFlipped) postScale(-1f, 1f)
                    postRotate(exif.rotationDegrees.toFloat())
                }
                val normalized = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                try {
                    destination = File(root, "${UUID.randomUUID()}.jpg")
                    destination.outputStream().use { if (!normalized.compress(Bitmap.CompressFormat.JPEG, 85, it)) throw FileImportException() }
                } finally {
                    if (normalized !== bitmap) normalized.recycle()
                    bitmap.recycle()
                }
                ImportedFile(destination.name, name.substringBeforeLast('.') + ".jpg", "image/jpeg", destination.length())
            }
        } catch (error: Exception) {
            destination?.delete()
            throw error
        } finally { raw.delete() }
    }
}
