package com.example.pawcare.domain.model

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val readingMinutes: Int,
    val cover: String,
    val coverDescription: String,
    val blocks: List<ArticleBlock>,
    val sources: List<ArticleSource>,
    val featured: Boolean = false,
)

sealed interface ArticleBlock {
    data class Heading(val text: String) : ArticleBlock
    data class Paragraph(val text: String) : ArticleBlock
    data class Bullet(val text: String) : ArticleBlock
    data class Callout(val title: String, val text: String) : ArticleBlock
    data class Image(val asset: String, val description: String, val caption: String) : ArticleBlock
}

data class ArticleSource(val title: String, val url: String)
