package PUhr.data.db

import PUhr.data.db.dao.VaultFileDao
import PUhr.data.db.entity.TagsConverter
import PUhr.data.db.entity.VaultFileEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [VaultFileEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(TagsConverter::class)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun vaultFileDao(): VaultFileDao
}
