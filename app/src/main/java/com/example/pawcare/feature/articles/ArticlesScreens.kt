package com.example.pawcare.feature.articles

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pawcare.R
import com.example.pawcare.core.ui.*
import com.example.pawcare.domain.model.*
import com.example.pawcare.ui.theme.PawCareTheme

@Composable
fun articleLanguage(): String = ConfigurationCompat.getLocales(LocalConfiguration.current)[0]?.language ?: "en"

@DrawableRes
private fun articleImage(asset: String): Int = when (asset) {
    "article_sniff_walk" -> R.drawable.article_sniff_walk
    "article_walk_pause" -> R.drawable.article_walk_pause
    "article_body_language" -> R.drawable.article_body_language
    "article_give_space" -> R.drawable.article_give_space
    "article_cooperative_care" -> R.drawable.article_cooperative_care
    "article_care_setup" -> R.drawable.article_care_setup
    "article_home_enrichment" -> R.drawable.article_home_enrichment
    "article_toy_rotation" -> R.drawable.article_toy_rotation
    "article_quiet_rest" -> R.drawable.article_quiet_rest
    "article_rest_corner" -> R.drawable.article_rest_corner
    "article_car_travel" -> R.drawable.article_car_travel
    "article_travel_kit" -> R.drawable.article_travel_kit
    else -> R.drawable.article_sniff_walk
}

@Composable
fun ArticlesRoute(onArticle: (String) -> Unit, viewModel: ArticlesViewModel = hiltViewModel()) {
    val language = articleLanguage()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(language) { viewModel.load(language) }
    ArticlesScreen(state, onArticle) { viewModel.load(language, retry = true) }
}

@Composable
fun ArticlesScreen(state: ArticlesUiState, onArticle: (String) -> Unit, onRetry: () -> Unit) {
    LazyPage(stringResource(R.string.articles)) {
        item("intro") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.articles_headline), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.articles_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.failed) ArticleError(onRetry)
                if (!state.loading && !state.failed && state.articles.isEmpty()) EmptyState(stringResource(R.string.articles_empty))
            }
        }
        items(state.articles, key = { it.id }, contentType = { "article" }) { article ->
            ArticleCard(article, onClick = { onArticle(article.id) })
        }
    }
}

@Composable
fun ArticleCard(article: Article, modifier: Modifier = Modifier, recommended: Boolean = false, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Image(painterResource(articleImage(article.cover)), article.coverDescription,
            Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Crop)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (recommended) Text(stringResource(R.string.recommended_article), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(article.category + " · " + stringResource(R.string.article_reading_time, article.readingMinutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(article.title, style = MaterialTheme.typography.headlineSmall)
            Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.read_article) + " →", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ArticleDetailRoute(id: String, onBack: () -> Unit, viewModel: ArticleDetailViewModel = hiltViewModel()) {
    val language = articleLanguage()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(id, language) { viewModel.load(id, language) }
    ArticleDetailScreen(state, onBack) { viewModel.load(id, language, retry = true) }
}

@Composable
fun ArticleDetailScreen(state: ArticleDetailUiState, onBack: () -> Unit, onRetry: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var failedLink by remember { mutableStateOf(false) }
    LazyPage(stringResource(R.string.article_detail), onBack) {
        if (state.loading) item("loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (state.failed) item("error") { ArticleError(onRetry) }
        val article = state.article
        if (!state.loading && !state.failed && article == null) item("missing") { EmptyState(stringResource(R.string.not_found)) }
        if (article != null) {
            item("hero") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = MaterialTheme.shapes.large) {
                        Image(painterResource(articleImage(article.cover)), article.coverDescription, Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Crop)
                    }
                    Text(article.category + " · " + stringResource(R.string.article_reading_time, article.readingMinutes), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text(article.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                    SelectionContainer { Text(article.summary, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            items(article.blocks.size, key = { "block:$it" }, contentType = { article.blocks[it]::class.simpleName }) { index ->
                when (val block = article.blocks[index]) {
                    is ArticleBlock.Heading -> Text(block.text, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp).semantics { heading() })
                    is ArticleBlock.Paragraph -> SelectionContainer { Text(emphasize(block.text), style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)) }
                    is ArticleBlock.Bullet -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary)
                        SelectionContainer(Modifier.weight(1f)) { Text(emphasize(block.text), style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)) }
                    }
                    is ArticleBlock.Callout -> Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(block.title, style = MaterialTheme.typography.titleMedium)
                            SelectionContainer { Text(emphasize(block.text), style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                    is ArticleBlock.Image -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = MaterialTheme.shapes.large) {
                            Image(painterResource(articleImage(block.asset)), block.description, Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Crop)
                        }
                        Text(block.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item("sources") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(stringResource(R.string.article_sources), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.article_editorial_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    article.sources.forEach { source ->
                        TextButton({
                            try { uriHandler.openUri(source.url) }
                            catch (_: android.content.ActivityNotFoundException) { failedLink = true }
                            catch (_: SecurityException) { failedLink = true }
                        }, contentPadding = PaddingValues(vertical = 8.dp)) { Text(source.title) }
                    }
                    ErrorState(if (failedLink) R.string.error_open else null) { failedLink = false }
                }
            }
        }
    }
}

private fun emphasize(text: String): AnnotatedString = buildAnnotatedString {
    var start = 0
    Regex("\\*\\*(.+?)\\*\\*").findAll(text).forEach { match ->
        append(text.substring(start, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(match.groupValues[1]) }
        start = match.range.last + 1
    }
    append(text.substring(start))
}

@Composable private fun ArticleError(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.articles_error), color = MaterialTheme.colorScheme.error)
        OutlinedButton(onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Preview(showBackground = true)
@Composable private fun ArticleCardPreview() {
    PawCareTheme { ArticleCard(Article("preview", "Прогулка глазами собаки", "Меньше спешки, больше открытий и спокойного общения.", "Каждый день", 5,
        "article_sniff_walk", "Собака исследует парк", emptyList(), emptyList()), recommended = true) {} }
}
