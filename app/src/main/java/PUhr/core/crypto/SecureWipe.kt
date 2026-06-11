package PUhr.core.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.SecureRandom

object SecureWipe {

    private const val PASSES = 3
    private val random = SecureRandom()

    suspend fun wipeFile(path: Path) {
        withContext(Dispatchers.IO) {
            if (Files.notExists(path)) return@withContext

            try {
                val size = Files.size(path)
                if (size == 0L) {
                    Files.deleteIfExists(path)
                    return@withContext
                }

                val opts = setOf(StandardOpenOption.WRITE, StandardOpenOption.READ)
                FileChannel.open(path, opts).use { channel ->
                    val bufferSize = 4096L.coerceAtMost(size).toInt().coerceAtLeast(1)
                    val buffer = ByteBuffer.allocate(bufferSize)

                    repeat(PASSES) {
                        channel.position(0)
                        var written = 0L
                        while (written < size) {
                            val chunk = bufferSize.coerceAtMost((size - written).toInt())
                            if (chunk != buffer.capacity()) {
                                buffer.limit(chunk)
                            } else {
                                buffer.limit(buffer.capacity())
                            }
                            val data = ByteArray(chunk)
                            random.nextBytes(data)
                            buffer.clear()
                            buffer.put(data)
                            buffer.flip()
                            written += channel.write(buffer)
                        }
                        channel.force(true)
                    }
                }

                Files.deleteIfExists(path)
            } catch (_: FileNotFoundException) {
            } catch (_: IOException) {
            }
        }
    }
}
