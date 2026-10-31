package com.example.pawcare.data.local

import androidx.room.*
import com.example.pawcare.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(primaryKeys = ["id"], tableName = "pets")
data class PetEntity(@Embedded val value: Pet)

@Entity(primaryKeys = ["id"], tableName = "plans", foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)], indices = [Index("petId")])
data class PlanEntity(@Embedded val value: CarePlan)

@Entity(primaryKeys = ["id"], tableName = "occurrences", foreignKeys = [
    ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = PlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("petId"), Index(value = ["planId", "originalDate"], unique = true), Index("date"), Index("spawnedFrom")])
data class OccurrenceEntity(@Embedded val value: CareOccurrence)

@Entity(primaryKeys = ["id"], tableName = "completions", foreignKeys = [
    ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = OccurrenceEntity::class, parentColumns = ["id"], childColumns = ["occurrenceId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("petId"), Index(value = ["occurrenceId"], unique = true)])
data class CompletionEntity(@Embedded val value: CompletionRecord)

@Entity(primaryKeys = ["id"], tableName = "weights", foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)], indices = [Index("petId")])
data class WeightEntity(@Embedded val value: WeightEntry)

@Entity(primaryKeys = ["id"], tableName = "contacts", foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)], indices = [Index("petId")])
data class ContactEntity(@Embedded val value: Contact)

@Entity(primaryKeys = ["id"], tableName = "attachments", foreignKeys = [
    ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = OccurrenceEntity::class, parentColumns = ["id"], childColumns = ["occurrenceId"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = CompletionEntity::class, parentColumns = ["id"], childColumns = ["completionId"], onDelete = ForeignKey.SET_NULL)
], indices = [Index("petId"), Index("occurrenceId"), Index("completionId"), Index(value = ["path"], unique = true)])
data class AttachmentEntity(@Embedded val value: Attachment)

@Entity(tableName = "reminders", foreignKeys = [ForeignKey(entity = OccurrenceEntity::class, parentColumns = ["id"], childColumns = ["occurrenceId"], onDelete = ForeignKey.CASCADE)], indices = [Index("occurrenceId")])
data class ReminderEntity(@PrimaryKey val id: String, val occurrenceId: String, val offsetDays: Int, val trigger: Long, val delivered: Boolean = false)

class Converters {
    @TypeConverter fun date(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun date(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun time(value: String?): LocalTime? = value?.let(LocalTime::parse)
    @TypeConverter fun time(value: LocalTime?): String? = value?.toString()
    @TypeConverter fun instant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun instant(value: Instant?): Long? = value?.toEpochMilli()
}
