package com.example.pawcare.core.notifications

interface ReminderScheduler {
    fun requestSync()
    fun cancel(ids: List<String>)
}
