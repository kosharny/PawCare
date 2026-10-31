package com.example.pawcare.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PawCareDao {
    @Query("SELECT * FROM pets ORDER BY createdAt, id") suspend fun allPets(): List<PetEntity>
    @Query("SELECT * FROM plans") suspend fun allPlans(): List<PlanEntity>
    @Query("SELECT * FROM occurrences") suspend fun allOccurrences(): List<OccurrenceEntity>
    @Query("SELECT * FROM completions ORDER BY completedAt DESC") suspend fun allCompletions(): List<CompletionEntity>
    @Query("SELECT path FROM attachments") suspend fun allPaths(): List<String>
    @Query("SELECT * FROM pets ORDER BY createdAt, id") fun pets(): Flow<List<PetEntity>>
    @Query("SELECT * FROM plans") fun plans(): Flow<List<PlanEntity>>
    @Query("SELECT * FROM occurrences") fun occurrences(): Flow<List<OccurrenceEntity>>
    @Query("SELECT * FROM completions ORDER BY completedAt DESC") fun completions(): Flow<List<CompletionEntity>>
    @Query("SELECT * FROM pets WHERE id=:id") suspend fun pet(id: String): PetEntity?
    @Query("SELECT * FROM plans WHERE id=:id") suspend fun plan(id: String): PlanEntity?
    @Query("SELECT * FROM occurrences WHERE id=:id") suspend fun occurrence(id: String): OccurrenceEntity?
    @Query("SELECT * FROM occurrences WHERE planId=:id ORDER BY originalDate") suspend fun series(id: String): List<OccurrenceEntity>
    @Query("SELECT * FROM plans WHERE active=1") suspend fun activePlans(): List<PlanEntity>
    @Upsert suspend fun save(entity: PetEntity)
    @Upsert suspend fun save(entity: PlanEntity)
    @Upsert suspend fun save(entity: OccurrenceEntity)
    @Upsert suspend fun save(entity: CompletionEntity)
    @Upsert suspend fun save(entity: WeightEntity)
    @Upsert suspend fun save(entity: ContactEntity)
    @Upsert suspend fun save(entity: AttachmentEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(entity: OccurrenceEntity)
    @Query("DELETE FROM pets WHERE id=:id") suspend fun deletePet(id: String)
    @Query("SELECT * FROM completions WHERE occurrenceId=:id") suspend fun completion(id: String): CompletionEntity?
    @Query("DELETE FROM completions WHERE occurrenceId=:id") suspend fun deleteCompletion(id: String)
    @Query("DELETE FROM occurrences WHERE id=:id") suspend fun deleteOccurrence(id: String)
    @Query("SELECT * FROM occurrences WHERE spawnedFrom=:id") suspend fun children(id: String): List<OccurrenceEntity>
    @Query("SELECT * FROM weights WHERE petId=:id ORDER BY date DESC, id") fun weights(id: String): Flow<List<WeightEntity>>
    @Query("DELETE FROM weights WHERE id=:id") suspend fun deleteWeight(id: String)
    @Query("SELECT * FROM contacts WHERE petId=:id ORDER BY name, id") fun contacts(id: String): Flow<List<ContactEntity>>
    @Query("DELETE FROM contacts WHERE id=:id") suspend fun deleteContact(id: String)
    @Query("SELECT * FROM attachments WHERE petId=:id ORDER BY importedAt DESC") fun attachments(id: String): Flow<List<AttachmentEntity>>
    @Query("SELECT * FROM attachments WHERE id=:id") suspend fun attachment(id: String): AttachmentEntity?
    @Query("DELETE FROM attachments WHERE id=:id") suspend fun deleteAttachment(id: String)
    @Query("SELECT * FROM reminders") suspend fun reminders(): List<ReminderEntity>
    @Query("SELECT * FROM reminders WHERE id=:id") suspend fun reminder(id: String): ReminderEntity?
    @Upsert suspend fun save(entity: ReminderEntity)
    @Query("DELETE FROM reminders WHERE id=:id") suspend fun deleteReminder(id: String)
    @Query("UPDATE reminders SET delivered=1 WHERE id=:id AND trigger=:trigger AND delivered=0") suspend fun claimReminder(id: String, trigger: Long): Int
}

@Database(entities = [PetEntity::class, PlanEntity::class, OccurrenceEntity::class, CompletionEntity::class, WeightEntity::class, ContactEntity::class, AttachmentEntity::class, ReminderEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class PawCareDatabase : RoomDatabase() { abstract fun dao(): PawCareDao }
