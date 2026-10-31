package com.example.pawcare.data.repository

import androidx.room.withTransaction
import com.example.pawcare.core.files.FileStore
import com.example.pawcare.core.notifications.ReminderScheduler
import com.example.pawcare.core.time.TimeSource
import com.example.pawcare.data.local.*
import com.example.pawcare.data.preferences.PreferencesRepository
import com.example.pawcare.domain.model.*
import com.example.pawcare.domain.recurrence.RecurrenceCalculator
import com.example.pawcare.domain.repository.CareRepository
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

class CareOperationException(val reason: Reason) : IllegalStateException() {
    enum class Reason { INVALID, MISSING, FUTURE, HISTORY_PROTECTED }
}
@Singleton
class LocalCareRepository @Inject constructor(
    private val db: PawCareDatabase, private val time: TimeSource,
    private val scheduler: ReminderScheduler, private val preferences: PreferencesRepository,
    private val files: FileStore,
) : CareRepository {
    private val dao = db.dao()
    override val snapshot = db.invalidationTracker.createFlow("pets", "plans", "occurrences", "completions").map {
        db.withTransaction { CareSnapshot(dao.allPets().map { it.value }, dao.allPlans().map { it.value }, dao.allOccurrences().map { it.value }, dao.allCompletions().map { it.value }) }
    }
    override suspend fun savePet(pet: Pet) {
        require(pet.name.isNotBlank() && (pet.birthDate == null || pet.birthDate <= time.today()))
        val old = dao.pet(pet.id)?.value
        dao.save(PetEntity(pet.copy(name = pet.name.trim())))
        if (old?.photoPath != null && old.photoPath != pet.photoPath) files.delete(old.photoPath)
        preferences.selectPet(pet.id)
        preferences.finishOnboarding()
    }
    override suspend fun deletePet(id: String) {
        val attachments = dao.attachments(id).first().map { it.value.path }
        val photo = dao.pet(id)?.value?.photoPath
        val reminderIds = dao.reminders().filter { reminder -> dao.occurrence(reminder.occurrenceId)?.value?.petId == id }.map { it.id }
        db.withTransaction { dao.deletePet(id) }
        scheduler.cancel(reminderIds)
        preferences.selectPet(dao.pets().first().firstOrNull()?.value?.id)
        attachments.forEach { files.delete(it) }
        photo?.let { files.delete(it) }
        scheduler.requestSync()
    }
    override suspend fun savePlan(plan: CarePlan) {
        require(plan.title.isNotBlank() && plan.interval in 1..999 && (plan.advanceDays == null || plan.advanceDays in 1..365))
        db.withTransaction {
            dao.save(PlanEntity(plan))
            dao.insert(OccurrenceEntity(CareOccurrence(planId = plan.id, petId = plan.petId, originalDate = plan.anchorDate, date = plan.anchorDate, time = plan.time)))
            materializePlan(plan, time.today(), time.today().plusDays(90))
        }
        scheduler.requestSync()
    }
    override suspend fun editOccurrence(id: String, title: String, category: Category, notes: String, medication: String, date: LocalDate, time: LocalTime?) {
        require(title.isNotBlank())
        db.withTransaction {
            val occurrence = pending(id)
            dao.save(OccurrenceEntity(occurrence.copy(titleOverride = title.trim(), categoryOverride = category, notesOverride = notes, medicationOverride = medication, date = date, time = time)))
        }
        scheduler.requestSync()
    }
    override suspend fun editSeries(plan: CarePlan, from: LocalDate) {
        require(plan.title.isNotBlank() && plan.interval in 1..999 && (plan.advanceDays == null || plan.advanceDays in 1..365))
        val cancelledReminders = db.withTransaction {
            val previous = dao.series(plan.id).map { it.value }
            val editable = previous.filter { it.status == OccurrenceStatus.PENDING && it.originalDate >= from }
            val reminderIds = dao.reminders().filter { reminder -> editable.any { it.id == reminder.occurrenceId } }.map { it.id }
            editable.forEach { dao.deleteOccurrence(it.id) }
            dao.save(PlanEntity(plan))
            val desired = if (plan.mode == RepeatMode.FIXED) RecurrenceCalculator.fixedDates(plan, from, from.plusDays(90)) else listOf(plan.anchorDate.coerceAtLeast(from))
            desired.forEach { date -> dao.insert(OccurrenceEntity(CareOccurrence(planId = plan.id, petId = plan.petId, originalDate = date, date = date, time = plan.time))) }
            reminderIds
        }
        scheduler.cancel(cancelledReminders)
        scheduler.requestSync()
    }
    private suspend fun pending(id: String): CareOccurrence {
        val value = dao.occurrence(id)?.value ?: throw CareOperationException(CareOperationException.Reason.MISSING)
        if (value.status != OccurrenceStatus.PENDING) throw CareOperationException(CareOperationException.Reason.INVALID)
        return value
    }
    override suspend fun complete(id: String, at: Instant, comment: String) {
        if (at > time.clock.instant()) throw CareOperationException(CareOperationException.Reason.FUTURE)
        db.withTransaction {
            val occurrence = dao.occurrence(id)?.value ?: throw CareOperationException(CareOperationException.Reason.MISSING)
            if (occurrence.status == OccurrenceStatus.COMPLETED) return@withTransaction
            if (occurrence.status != OccurrenceStatus.PENDING) throw CareOperationException(CareOperationException.Reason.INVALID)
            val plan = dao.plan(occurrence.planId)?.value ?: throw CareOperationException(CareOperationException.Reason.MISSING)
            dao.save(OccurrenceEntity(occurrence.copy(status = OccurrenceStatus.COMPLETED)))
            dao.save(CompletionEntity(CompletionRecord(occurrenceId = id, petId = occurrence.petId, completedAt = at,
                title = occurrence.titleOverride ?: plan.title, category = occurrence.categoryOverride ?: plan.category,
                notes = occurrence.notesOverride ?: plan.notes, medication = occurrence.medicationOverride ?: plan.medication, comment = comment)))
            if (plan.mode == RepeatMode.AFTER_COMPLETION && plan.active) {
                val date = RecurrenceCalculator.dateAt(at.atZone(time.zone()).toLocalDate(), plan.interval, plan.unit, 1)
                dao.insert(OccurrenceEntity(CareOccurrence(planId = plan.id, petId = plan.petId, originalDate = date, date = date, time = plan.time, spawnedFrom = id)))
            }
            materializePlan(plan, time.today(), time.today().plusDays(90))
        }
        scheduler.requestSync()
    }
    override suspend fun editCompletion(id: String, at: Instant, comment: String) {
        if (at > time.clock.instant()) throw CareOperationException(CareOperationException.Reason.FUTURE)
        val cancelledReminders = db.withTransaction {
            val occurrence = dao.occurrence(id)?.value ?: throw CareOperationException(CareOperationException.Reason.MISSING)
            val record = dao.completion(id)?.value ?: throw CareOperationException(CareOperationException.Reason.MISSING)
            val plan = dao.plan(occurrence.planId)!!.value
            var reminderIds = emptyList<String>()
            if (at != record.completedAt && plan.mode == RepeatMode.AFTER_COMPLETION && plan.active) {
                val children = dao.children(id).map { it.value }
                if (children.any { it.status != OccurrenceStatus.PENDING }) throw CareOperationException(CareOperationException.Reason.HISTORY_PROTECTED)
                reminderIds = dao.reminders().filter { reminder -> children.any { it.id == reminder.occurrenceId } }.map { it.id }
                children.forEach { dao.deleteOccurrence(it.id) }
                val date = RecurrenceCalculator.dateAt(at.atZone(time.zone()).toLocalDate(), plan.interval, plan.unit, 1)
                dao.insert(OccurrenceEntity(CareOccurrence(planId = plan.id, petId = plan.petId, originalDate = date, date = date, time = plan.time, spawnedFrom = id)))
            }
            dao.save(CompletionEntity(record.copy(completedAt = at, comment = comment)))
            reminderIds
        }
        scheduler.cancel(cancelledReminders)
        scheduler.requestSync()
    }
    override suspend fun undoCompletion(id: String) {
        db.withTransaction {
            val occurrence = dao.occurrence(id)?.value ?: throw CareOperationException(CareOperationException.Reason.MISSING)
            val plan = dao.plan(occurrence.planId)?.value ?: throw CareOperationException(CareOperationException.Reason.MISSING)
            if (!plan.active || occurrence.status != OccurrenceStatus.COMPLETED) throw CareOperationException(CareOperationException.Reason.INVALID)
            val children = dao.children(id).map { it.value }
            if (children.any { it.status != OccurrenceStatus.PENDING }) throw CareOperationException(CareOperationException.Reason.HISTORY_PROTECTED)
            val reminderIds = dao.reminders().filter { reminder -> children.any { it.id == reminder.occurrenceId } }.map { it.id }
            children.forEach { dao.deleteOccurrence(it.id) }
            scheduler.cancel(reminderIds)
            dao.deleteCompletion(id)
            dao.save(OccurrenceEntity(occurrence.copy(status = OccurrenceStatus.PENDING)))
        }
        scheduler.requestSync()
    }
    override suspend fun reschedule(id: String, date: LocalDate, time: LocalTime?) {
        db.withTransaction { dao.save(OccurrenceEntity(pending(id).copy(date = date, time = time))) }
        scheduler.requestSync()
    }
    override suspend fun skip(id: String, nextDate: LocalDate?) {
        db.withTransaction {
            val occurrence = pending(id)
            val plan = dao.plan(occurrence.planId)!!.value
            if (plan.mode == RepeatMode.AFTER_COMPLETION) {
                require(nextDate != null && nextDate > occurrence.originalDate)
                dao.insert(OccurrenceEntity(CareOccurrence(planId = plan.id, petId = plan.petId, originalDate = nextDate, date = nextDate, time = plan.time, spawnedFrom = id)))
            }
            dao.save(OccurrenceEntity(occurrence.copy(status = OccurrenceStatus.SKIPPED)))
        }
        scheduler.requestSync()
    }
    override suspend fun deleteOccurrence(id: String) {
        db.withTransaction { dao.save(OccurrenceEntity(pending(id).copy(status = OccurrenceStatus.CANCELLED))) }
        scheduler.requestSync()
    }
    override suspend fun archivePlan(id: String) {
        db.withTransaction {
            val plan = dao.plan(id)?.value ?: return@withTransaction
            dao.save(PlanEntity(plan.copy(active = false)))
            dao.series(id).map { it.value }.filter { it.status == OccurrenceStatus.PENDING }.forEach {
                dao.save(OccurrenceEntity(it.copy(status = OccurrenceStatus.CANCELLED)))
            }
        }
        scheduler.requestSync()
    }
    override suspend fun materialize(from: LocalDate, through: LocalDate) {
        db.withTransaction { dao.activePlans().forEach { materializePlan(it.value, from, through) } }
    }
    private suspend fun materializePlan(plan: CarePlan, from: LocalDate, through: LocalDate) {
        if (!plan.active || plan.mode != RepeatMode.FIXED) return
        RecurrenceCalculator.fixedDates(plan, from, through).forEach { date ->
            dao.insert(OccurrenceEntity(CareOccurrence(planId = plan.id, petId = plan.petId, originalDate = date, date = date, time = plan.time)))
        }
    }
    override fun weights(petId: String) = dao.weights(petId).map { rows -> rows.map { it.value } }
    override suspend fun saveWeight(entry: WeightEntry) {
        require(entry.kilograms.isFinite() && entry.kilograms > 0 && entry.kilograms <= 300 && entry.date <= time.today())
        dao.save(WeightEntity(entry))
    }
    override suspend fun deleteWeight(id: String) = dao.deleteWeight(id)
    override fun contacts(petId: String) = dao.contacts(petId).map { rows -> rows.map { it.value } }
    override suspend fun saveContact(contact: Contact) { require(contact.name.isNotBlank()); dao.save(ContactEntity(contact.copy(name = contact.name.trim()))) }
    override suspend fun deleteContact(id: String) = dao.deleteContact(id)
    override fun attachments(petId: String) = dao.attachments(petId).map { rows -> rows.map { it.value } }
    override suspend fun saveAttachment(attachment: Attachment) = dao.save(AttachmentEntity(attachment))
    override suspend fun renameAttachment(id: String, name: String) {
        require(name.isNotBlank())
        dao.attachment(id)?.let { dao.save(AttachmentEntity(it.value.copy(name = name.trim()))) }
    }
    override suspend fun deleteAttachment(id: String) {
        val attachment = dao.attachment(id)?.value ?: return
        dao.deleteAttachment(id)
        files.delete(attachment.path)
    }
}
