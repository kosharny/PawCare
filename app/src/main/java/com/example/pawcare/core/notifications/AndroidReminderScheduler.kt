package com.example.pawcare.core.notifications

import android.app.*
import android.content.*
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.pawcare.MainActivity
import com.example.pawcare.R
import com.example.pawcare.core.time.TimeSource
import com.example.pawcare.data.local.*
import com.example.pawcare.data.preferences.PreferencesRepository
import com.example.pawcare.domain.model.*
import com.example.pawcare.domain.repository.CareRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val REMINDER_CHANNEL = "care"
@Singleton
class AndroidReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) : ReminderScheduler {
    override fun requestSync() {
        WorkManager.getInstance(context).enqueueUniqueWork("reminder-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, OneTimeWorkRequestBuilder<ReminderSyncWorker>().build())
    }
    override fun cancel(ids: List<String>) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val notifications = context.getSystemService(NotificationManager::class.java)
        ids.forEach { alarms.cancel(alarmIntent(context, it, 0)); notifications.cancel(it, 0) }
    }
    fun initialize() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("reminder-maintenance", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<ReminderSyncWorker>(12, TimeUnit.HOURS).build())
        requestSync()
    }
}

private fun alarmIntent(context: Context, id: String, trigger: Long): PendingIntent = PendingIntent.getBroadcast(
    context, 0, Intent(context, ReminderReceiver::class.java).setAction("com.example.pawcare.REMINDER")
        .setData(Uri.parse("pawcare://reminder/${Uri.encode(id)}")).putExtra("id", id).putExtra("trigger", trigger),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

@Singleton
class ReminderCoordinator @Inject constructor(
    @ApplicationContext private val context: Context, private val db: PawCareDatabase,
    private val repository: CareRepository, private val preferences: PreferencesRepository,
    private val time: TimeSource, private val files: com.example.pawcare.core.files.FileStore,
) {
    private val mutex = Mutex()
    suspend fun reconcile() = mutex.withLock {
        repository.materialize(time.today(), time.today().plusDays(90))
        val referenced = db.dao().allPaths().toSet() + db.dao().allPets().mapNotNull { it.value.photoPath }
        files.cleanup(referenced, time.clock.millis() - TimeUnit.DAYS.toMillis(1))
        val settings = preferences.preferences.first()
        val manager = context.getSystemService(AlarmManager::class.java)
        val dao = db.dao()
        val old = dao.reminders().associateBy { it.id }
        val now = time.clock.instant()
        val candidates = if (!settings.notifications || !NotificationManagerCompat.from(context).areNotificationsEnabled()) emptyList() else
            repository.snapshot.first().events.filter { it.plan.active && it.occurrence.status == OccurrenceStatus.PENDING }.flatMap { event ->
                buildList {
                    if (event.plan.remindOnDay) add(0)
                    event.plan.advanceDays?.let { add(it) }
                }.distinct().map { offset ->
                    val trigger = event.occurrence.date.minusDays(offset.toLong()).atTime(event.occurrence.time ?: settings.defaultTime).atZone(time.zone()).toInstant()
                    ReminderEntity("${event.occurrence.id}:$offset", event.occurrence.id, offset, trigger.toEpochMilli())
                }
            }.filter { it.trigger > now.toEpochMilli() && it.trigger < now.plusSeconds(14 * 86400L).toEpochMilli() }.sortedBy { it.trigger }.take(64)
        val ids = candidates.map { it.id }.toSet()
        old.values.forEach {
            if (it.id !in ids) {
                manager.cancel(alarmIntent(context, it.id, it.trigger))
                val occurrence = dao.occurrence(it.occurrenceId)?.value
                if (occurrence == null || occurrence.status != OccurrenceStatus.PENDING) context.getSystemService(NotificationManager::class.java).cancel(it.id, 0)
                if (!it.delivered) dao.deleteReminder(it.id)
            }
        }
        candidates.forEach { candidate ->
            val previous = old[candidate.id]
            if (previous?.trigger == candidate.trigger && previous.delivered) return@forEach
            dao.save(candidate)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, candidate.trigger, alarmIntent(context, candidate.id, candidate.trigger))
        }
    }
    suspend fun deliver(id: String, trigger: Long) = mutex.withLock {
        val dao = db.dao()
        val alarm = dao.reminder(id) ?: return@withLock
        if (alarm.trigger != trigger || alarm.delivered || time.clock.millis() < trigger) return@withLock
        val settings = preferences.preferences.first()
        if (!settings.notifications || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return@withLock
        val event = repository.snapshot.first().events.find { it.occurrence.id == alarm.occurrenceId } ?: return@withLock
        if (!event.plan.active || event.occurrence.status != OccurrenceStatus.PENDING) return@withLock
        if ((alarm.offsetDays == 0 && !event.plan.remindOnDay) || (alarm.offsetDays != 0 && alarm.offsetDays != event.plan.advanceDays)) return@withLock
        val expected = event.occurrence.date.minusDays(alarm.offsetDays.toLong()).atTime(event.occurrence.time ?: settings.defaultTime).atZone(time.zone()).toInstant().toEpochMilli()
        if (expected != trigger || time.clock.millis() - trigger > 86400000L) return@withLock
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(REMINDER_CHANNEL)
        if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return@withLock
        val intent = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).setData(Uri.parse("pawcare://event/${event.occurrence.id}")).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(event.title).setContentText(context.getString(R.string.reminder_body, event.pet.name, event.occurrence.date.toString()))
            .setContentIntent(intent).setAutoCancel(true).build()
        if (dao.claimReminder(id, trigger) == 1) {
            try { context.getSystemService(NotificationManager::class.java).notify(id, 0, notification) }
            catch (_: SecurityException) { dao.save(alarm) }
        }
    }
}

@HiltWorker
class ReminderSyncWorker @AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters, private val coordinator: ReminderCoordinator) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try { coordinator.reconcile(); Result.success() }
    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
}
@HiltWorker
class ReminderDeliveryWorker @AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters, private val coordinator: ReminderCoordinator) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.failure()
        return try { coordinator.deliver(id, inputData.getLong("trigger", 0)); Result.success() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }
}
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val request = OneTimeWorkRequestBuilder<ReminderDeliveryWorker>().setInputData(workDataOf("id" to id, "trigger" to intent.getLongExtra("trigger", 0))).build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        WorkManager.getInstance(context).enqueueUniqueWork("reminder-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, OneTimeWorkRequestBuilder<ReminderSyncWorker>().build())
    }
}
