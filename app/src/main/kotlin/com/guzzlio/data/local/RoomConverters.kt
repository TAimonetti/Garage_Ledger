package com.guzzlio.data.local

import androidx.room.TypeConverter
import com.guzzlio.domain.model.RecordFamily
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime

class RoomConverters {
    private val json = Json

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)

    @TypeConverter
    fun localDateTimeToString(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDateTime(value: String?): LocalDateTime? = value?.takeIf { it.isNotBlank() }?.let(LocalDateTime::parse)

    @TypeConverter
    fun listToString(value: List<String>): String = json.encodeToString(ListSerializer, value)

    @TypeConverter
    fun stringToList(value: String?): List<String> = value?.takeIf { it.isNotBlank() }?.let {
        runCatching { json.decodeFromString(ListSerializer, it) }.getOrDefault(emptyList())
    } ?: emptyList()

    @TypeConverter
    fun recordFamilyToString(value: RecordFamily): String = value.name

    @TypeConverter
    fun stringToRecordFamily(value: String): RecordFamily = RecordFamily.valueOf(value)

    private companion object {
        val ListSerializer = kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>())
    }
}
