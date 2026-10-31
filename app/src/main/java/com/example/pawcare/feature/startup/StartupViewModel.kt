package com.example.pawcare.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pawcare.data.preferences.PreferencesRepository
import com.example.pawcare.domain.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val repository: CareRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(StartupUiState())
    val uiState: StateFlow<StartupUiState> = mutableState.asStateFlow()
    private var job: Job? = null

    init {
        retry()
    }

    fun retry() {
        if (mutableState.value.status == StartupState.READY || job?.isActive == true) {
            return
        }

        job = viewModelScope.launch {
            mutableState.value = StartupUiState()
            try {
                coroutineScope {
                    val initialization = async {
                        combine(repository.snapshot, preferences.preferences) { _, _ ->
                            Unit
                        }
                            .first()
                    }

                    animateProgressTo(target = 0.9f, durationMillis = 1_600L)
                    initialization.await()
                    animateProgressTo(target = 1f, durationMillis = 250L)
                    delay(150L)
                }
                mutableState.value = mutableState.value.copy(status = StartupState.READY)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(status = StartupState.ERROR)
            }
        }
    }

    private suspend fun animateProgressTo(target: Float, durationMillis: Long) {
        val initialProgress = mutableState.value.progress
        val started = TimeSource.Monotonic.markNow()

        while (true) {
            val fraction = (started.elapsedNow().inWholeMilliseconds.toFloat() / durationMillis)
                .coerceIn(0f, 1f)
            mutableState.value = mutableState.value.copy(
                progress = initialProgress + (target - initialProgress) * fraction,
            )
            if (fraction >= 1f) {
                return
            }
            delay(16L)
        }
    }
}
