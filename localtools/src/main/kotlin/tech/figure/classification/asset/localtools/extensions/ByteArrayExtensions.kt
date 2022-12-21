package tech.figure.classification.asset.localtools.extensions

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

internal fun ByteArray.gzipAc(): ByteArray = ByteArrayOutputStream().use { byteStream ->
    GZIPOutputStream(byteStream).use { it.write(this, 0, this.size) }
    byteStream.toByteArray()
}
