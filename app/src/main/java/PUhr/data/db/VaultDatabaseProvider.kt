package PUhr.data.db

import PUhr.core.crypto.SecureWipe
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultDatabaseProvider @Inject constructor(
    private val context: Context,
) {

    @Volatile
    private var database: VaultDatabase? = null

    private var passphraseRef: ByteArray? = null

    @Synchronized
    fun open(dbk: ByteArray): VaultDatabase {
        database?.let { return it }

        val passphrase = dbk.copyOf()
        passphraseRef = passphrase

        val factory = SupportFactory(passphrase, PRAGMA_HOOK, true)

        val db = Room.databaseBuilder(context, VaultDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()

        database = db
        return db
    }

    @Synchronized
    fun getDatabase(): VaultDatabase? = database

    @Synchronized
    fun close() {
        database?.close()
        database = null

        passphraseRef?.fill(0)
        passphraseRef = null

        runBlocking {
            SecureWipe.wipeFile(File(context.filesDir, "$DB_NAME-wal").toPath())
            SecureWipe.wipeFile(File(context.filesDir, "$DB_NAME-shm").toPath())
        }
    }

    companion object {
        private const val DB_NAME = "timely_cache.db"

        private val PRAGMA_HOOK = object : SQLiteDatabaseHook {
            override fun preKey(database: net.sqlcipher.database.SQLiteDatabase) {
                database.execSQL("PRAGMA cipher_memory_security = ON")
                database.execSQL("PRAGMA secure_delete = ON")
            }

            override fun postKey(database: net.sqlcipher.database.SQLiteDatabase) {
                database.execSQL("PRAGMA cipher_memory_security = ON")
                database.execSQL("PRAGMA secure_delete = ON")
            }
        }
    }
}
