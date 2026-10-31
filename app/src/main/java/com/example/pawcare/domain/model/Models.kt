package com.example.pawcare.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
enum class Sex { UNSPECIFIED, MALE, FEMALE }
enum class Category { TREATMENT, VACCINATION, GROOMING, VET, OTHER }
enum class RepeatMode { ONCE, FIXED, AFTER_COMPLETION }
enum class IntervalUnit { DAYS, WEEKS, MONTHS }
enum class OccurrenceStatus { PENDING, COMPLETED, SKIPPED, CANCELLED }
enum class ContactType { VET, CLINIC, GROOMER, OTHER }

data class Pet(
    val id: String = newId(), val name: String = "", val photoPath: String? = null,
    val breed: String = "", val sex: Sex = Sex.UNSPECIFIED, val birthDate: LocalDate? = null,
    val approximateBirth: Boolean = false, val chip: String = "", val notes: String = "",
    val createdAt: Instant = Instant.now(),
)
data class CarePlan(
    val id: String = newId(), val petId: String, val title: String, val category: Category,
    val notes: String = "", val medication: String = "", val mode: RepeatMode = RepeatMode.ONCE,
    val interval: Int = 1, val unit: IntervalUnit = IntervalUnit.DAYS,
    val anchorDate: LocalDate, val time: LocalTime? = null,
    val remindOnDay: Boolean = false, val advanceDays: Int? = null, val active: Boolean = true,
)
data class CareOccurrence(
    val id: String = newId(), val planId: String, val petId: String,
    val originalDate: LocalDate, val date: LocalDate, val time: LocalTime? = null,
    val status: OccurrenceStatus = OccurrenceStatus.PENDING,
    val spawnedFrom: String? = null,
    val titleOverride: String? = null, val notesOverride: String? = null,
    val medicationOverride: String? = null, val categoryOverride: Category? = null,
)
data class CompletionRecord(
    val id: String = newId(), val occurrenceId: String, val petId: String,
    val completedAt: Instant, val title: String, val category: Category,
    val notes: String, val medication: String, val comment: String = "",
)
data class WeightEntry(
    val id: String = newId(), val petId: String, val date: LocalDate,
    val kilograms: Double, val note: String = "",
)
data class Attachment(
    val id: String = newId(), val petId: String, val occurrenceId: String? = null,
    val completionId: String? = null, val path: String, val name: String,
    val mime: String, val size: Long, val importedAt: Instant,
)
data class Contact(
    val id: String = newId(), val petId: String, val type: ContactType = ContactType.VET,
    val name: String, val phone: String = "", val address: String = "", val note: String = "",
)
data class CareEvent(val occurrence: CareOccurrence, val plan: CarePlan, val pet: Pet) {
    val title get() = occurrence.titleOverride ?: plan.title
    val category get() = occurrence.categoryOverride ?: plan.category
    val notes get() = occurrence.notesOverride ?: plan.notes
    val medication get() = occurrence.medicationOverride ?: plan.medication
}
data class CareSnapshot(
    val pets: List<Pet> = emptyList(), val plans: List<CarePlan> = emptyList(),
    val occurrences: List<CareOccurrence> = emptyList(), val completions: List<CompletionRecord> = emptyList(),
) {
    val events: List<CareEvent> get() {
        val petsById = pets.associateBy { it.id }
        val plansById = plans.associateBy { it.id }
        return occurrences.mapNotNull { occurrence ->
            val plan = plansById[occurrence.planId] ?: return@mapNotNull null
            val pet = petsById[occurrence.petId] ?: return@mapNotNull null
            CareEvent(occurrence, plan, pet)
        }.sortedWith(compareBy({ it.occurrence.date }, { it.occurrence.time ?: LocalTime.MIN }, { it.title }, { it.occurrence.id }))
    }
}
