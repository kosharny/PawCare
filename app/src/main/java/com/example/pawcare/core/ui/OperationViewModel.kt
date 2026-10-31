package com.example.pawcare.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pawcare.R
import com.example.pawcare.core.files.FileImportException
import com.example.pawcare.data.repository.CareOperationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OperationState(val busy: Boolean = false, val error: Int? = null, val saved: Boolean = false)
open class OperationViewModel : ViewModel() {
    protected val mutableOperation = MutableStateFlow(OperationState())
    val operation = mutableOperation.asStateFlow()
    fun dismissError() { mutableOperation.value = mutableOperation.value.copy(error = null) }
    fun acknowledgeSave() { mutableOperation.value = mutableOperation.value.copy(saved = false) }
    fun reportError(id: Int) { mutableOperation.value = mutableOperation.value.copy(error = id) }
    protected fun perform(exit: Boolean = false, block: suspend () -> Unit) {
        if (mutableOperation.value.busy) return
        mutableOperation.value = OperationState(busy = true)
        viewModelScope.launch {
            try { block(); mutableOperation.value = OperationState(saved = exit) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val message = when (error) {
                    is FileImportException -> R.string.error_file
                    is CareOperationException -> when (error.reason) {
                        CareOperationException.Reason.FUTURE -> R.string.error_future
                        CareOperationException.Reason.HISTORY_PROTECTED -> R.string.error_history
                        CareOperationException.Reason.MISSING -> R.string.not_found
                        CareOperationException.Reason.INVALID -> R.string.error_validation
                    }
                    is IllegalArgumentException, is java.time.DateTimeException, is NumberFormatException -> R.string.error_validation
                    else -> R.string.error_generic
                }
                mutableOperation.value = OperationState(error = message)
            }
        }
    }
}
