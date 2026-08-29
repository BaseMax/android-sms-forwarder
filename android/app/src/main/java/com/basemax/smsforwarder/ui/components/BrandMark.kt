package com.basemax.smsforwarder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun BrandMark(size: Int = 32) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3.5f).dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Sms,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size((size * 0.56f).dp),
        )
    }
}
