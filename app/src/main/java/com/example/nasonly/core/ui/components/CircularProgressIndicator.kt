package com.example.nasonly.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    size: Int = 48,
    strokeWidth: Int = 4
) {
    CircularProgressIndicator(
        modifier = modifier.size(size.dp),
        strokeWidth = strokeWidth.dp,
        color = MaterialTheme.colorScheme.primary
    )
}
