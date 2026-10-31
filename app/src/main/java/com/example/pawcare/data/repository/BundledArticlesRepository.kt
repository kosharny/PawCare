package com.example.pawcare.data.repository

import android.content.Context
import com.example.pawcare.domain.model.*
import com.example.pawcare.domain.repository.ArticlesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BundledArticlesRepository @Inject constructor(@ApplicationContext private val context: Context) : ArticlesRepository {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, List<Article>>()

    override suspend fun articles(language: String): List<Article> = withContext(Dispatchers.IO) {
        val selected = language.takeIf { it in setOf("en", "ru", "uk", "pl") } ?: "en"
        mutex.withLock {
            cache[selected] ?: read(selected).also { cache[selected] = it }
        }
    }

    private fun read(language: String): List<Article> {
        val json = context.assets.open("articles/$language.json").bufferedReader().use { it.readText() }
        val articles = JSONArray(json)
        return List(articles.length()) { index ->
            val item = articles.getJSONObject(index)
            val blocks = item.getJSONArray("blocks")
            val sources = item.getJSONArray("sources")
            Article(
                id = item.getString("id"), title = item.getString("title"), summary = item.getString("summary"),
                category = item.getString("category"), readingMinutes = item.getInt("readingMinutes"),
                cover = item.getString("cover"), coverDescription = item.getString("coverDescription"),
                featured = item.optBoolean("featured"),
                blocks = List(blocks.length()) { blockIndex ->
                    val block = blocks.getJSONObject(blockIndex)
                    when (block.getString("type")) {
                        "heading" -> ArticleBlock.Heading(block.getString("text"))
                        "paragraph" -> ArticleBlock.Paragraph(block.getString("text"))
                        "bullet" -> ArticleBlock.Bullet(block.getString("text"))
                        "callout" -> ArticleBlock.Callout(block.getString("title"), block.getString("text"))
                        "image" -> ArticleBlock.Image(block.getString("asset"), block.getString("description"), block.getString("caption"))
                        else -> error("Unsupported article block")
                    }
                },
                sources = List(sources.length()) { sourceIndex ->
                    val source = sources.getJSONObject(sourceIndex)
                    ArticleSource(source.getString("title"), source.getString("url"))
                },
            )
        }.also { items ->
            check(items.map { it.id }.distinct().size == items.size)
            check(items.all { it.blocks.isNotEmpty() && it.readingMinutes > 0 })
        }
    }
}
