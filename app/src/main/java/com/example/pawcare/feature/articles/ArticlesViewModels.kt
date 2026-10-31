package com.example.pawcare.feature.articles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pawcare.domain.model.Article
import com.example.pawcare.domain.repository.ArticlesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArticlesUiState(val loading: Boolean = true, val articles: List<Article> = emptyList(), val failed: Boolean = false)
data class ArticleDetailUiState(val loading: Boolean = true, val article: Article? = null, val failed: Boolean = false)

@HiltViewModel
class ArticlesViewModel @Inject constructor(private val repository: ArticlesRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ArticlesUiState())
    val uiState = mutableState.asStateFlow()
    private var language: String? = null
    private var job: Job? = null

    fun load(language: String, retry: Boolean = false) {
        if (!retry && this.language == language) return
        this.language = language
        job?.cancel()
        job = viewModelScope.launch {
            mutableState.value = ArticlesUiState()
            try { mutableState.value = ArticlesUiState(loading = false, articles = repository.articles(language)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = ArticlesUiState(loading = false, failed = true) }
        }
    }
}

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(private val repository: ArticlesRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(ArticleDetailUiState())
    val uiState = mutableState.asStateFlow()
    private var language: String? = null
    private var job: Job? = null

    fun load(id: String, language: String, retry: Boolean = false) {
        if (!retry && savedState.get<String>("article_id") == id && this.language == language) return
        savedState["article_id"] = id
        this.language = language
        job?.cancel()
        job = viewModelScope.launch {
            mutableState.value = ArticleDetailUiState()
            try { mutableState.value = ArticleDetailUiState(loading = false, article = repository.articles(language).find { it.id == id }) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = ArticleDetailUiState(loading = false, failed = true) }
        }
    }
}
