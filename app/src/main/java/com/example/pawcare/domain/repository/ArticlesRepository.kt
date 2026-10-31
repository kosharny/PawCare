package com.example.pawcare.domain.repository

import com.example.pawcare.domain.model.Article

interface ArticlesRepository {
    suspend fun articles(language: String): List<Article>
}
