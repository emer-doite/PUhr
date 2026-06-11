package PUhr.core.crypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class SecureWipeTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun fileIsDeletedAfterWipe() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        Files.write(file.toPath(), "sensitive data".toByteArray())

        SecureWipe.wipeFile(file.toPath())

        assertFalse(Files.exists(file.toPath()))
    }

    @Test
    fun nonExistentFile_doesNotThrow() = runBlocking {
        val path = Path.of(tempFolder.root.absolutePath, "nonexistent.bin")
        assertFalse(Files.exists(path))

        SecureWipe.wipeFile(path)
    }
}
