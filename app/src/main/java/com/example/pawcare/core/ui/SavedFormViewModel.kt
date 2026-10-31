package com.example.pawcare.core.ui

import androidx.lifecycle.SavedStateHandle

open class SavedFormViewModel(protected val savedState: SavedStateHandle) : OperationViewModel() {
    val fields: kotlinx.coroutines.flow.StateFlow<Map<String, String>> = savedState.getStateFlow("fields", hashMapOf<String, String>())
    fun change(key: String, value: String) {
        savedState["fields"] = HashMap(fields.value).apply { put(key, value) }
    }
    protected fun initialize(values: Map<String, String>) {
        if (savedState.get<Boolean>("initialized") == true) return
        savedState["fields"] = HashMap(values)
        savedState["initialized"] = true
    }
    protected fun value(key: String): String = fields.value[key].orEmpty()
}
