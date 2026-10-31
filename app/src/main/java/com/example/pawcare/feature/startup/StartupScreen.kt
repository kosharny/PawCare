package com.example.pawcare.feature.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pawcare.R
import com.example.pawcare.ui.theme.PawCareTheme

@Composable
fun StartupScreen(
    state: StartupUiState = StartupUiState(),
    onRetry: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pet),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(28.dp)
                        .size(56.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = stringResource(R.string.startup_tagline),
                style = MaterialTheme.typography.titleMedium,
            )
            if (state.status == StartupState.ERROR) {
                Text(
                    text = stringResource(R.string.error_generic),
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            } else {
                LinearProgressIndicator(
                    progress = {
                        state.progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.startup_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartupPreview() {
    PawCareTheme {
        StartupScreen(state = StartupUiState(progress = 0.65f))
    }
}
