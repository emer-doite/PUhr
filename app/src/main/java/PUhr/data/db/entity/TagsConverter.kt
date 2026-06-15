package PUhr.data.db.entity

import androidx.room.TypeConverter

class TagsConverter {

    @TypeConverter
    fun fromTags(tags: List<String>): String = tags.joinToString(",")

    @TypeConverter
    fun toTags(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",").map { it.trim() }
}
