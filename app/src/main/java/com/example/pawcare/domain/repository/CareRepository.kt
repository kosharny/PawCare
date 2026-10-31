package com.example.pawcare.domain.repository

import com.example.pawcare.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

interface CareRepository {
    val snapshot: Flow<CareSnapshot>
    suspend fun savePet(pet: Pet)
    suspend fun deletePet(id: String)
    suspend fun savePlan(plan: CarePlan)
    suspend fun editOccurrence(id: String, title: String, category: Category, notes: String, medication: String, date: LocalDate, time: LocalTime?)
    suspend fun editSeries(plan: CarePlan, from: LocalDate)
    suspend fun complete(id: String, at: Instant, comment: String)
    suspend fun editCompletion(id: String, at: Instant, comment: String)
    suspend fun undoCompletion(id: String)
    suspend fun reschedule(id: String, date: LocalDate, time: LocalTime?)
    suspend fun skip(id: String, nextDate: LocalDate?)
    suspend fun deleteOccurrence(id: String)
    suspend fun archivePlan(id: String)
    suspend fun materialize(from: LocalDate, through: LocalDate)
    fun weights(petId: String): Flow<List<WeightEntry>>
    suspend fun saveWeight(entry: WeightEntry)
    suspend fun deleteWeight(id: String)
    fun contacts(petId: String): Flow<List<Contact>>
    suspend fun saveContact(contact: Contact)
    suspend fun deleteContact(id: String)
    fun attachments(petId: String): Flow<List<Attachment>>
    suspend fun saveAttachment(attachment: Attachment)
    suspend fun renameAttachment(id: String, name: String)
    suspend fun deleteAttachment(id: String)
}
