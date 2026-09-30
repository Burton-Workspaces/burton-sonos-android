package com.burton.sonos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burton.sonos.BuildConfig
import com.burton.sonos.ui.components.FullScreenModal
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute

@Composable
fun SettingsModal(onDismiss: () -> Unit) {
    FullScreenModal(
        onDismiss = onDismiss,
        title = "Settings",
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BurtonCharcoal, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("About", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
                Spacer(Modifier.height(4.dp))
                Text("Burton Sonos", style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
            }
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodyLarge,
                color = BurtonMute,
            )
        }
    }
}
