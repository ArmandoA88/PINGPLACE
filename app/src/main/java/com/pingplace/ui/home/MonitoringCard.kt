package com.pingplace.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pingplace.background.MonitoringStatus
import com.pingplace.data.local.entity.UserSettingsEntity
import com.pingplace.domain.MonitoringPolicy
import com.pingplace.ui.common.ReliabilityStatus

@Composable
fun MonitoringCard(
    status: MonitoringStatus,
    settings: UserSettingsEntity,
    reliability: ReliabilityStatus,
    onSettings: () -> Unit
) {
    val mint = Color(0xFF9AF4CF)
    val ready = status.running && status.hasFix && settings.notificationsEnabled && reliability.notificationsGranted &&
        reliability.fineLocationGranted && reliability.locationServicesEnabled
    val label = when {
        !settings.backgroundLocationEnabled -> "Turn on nearby pings"
        !reliability.fineLocationGranted || !reliability.locationServicesEnabled -> "Location needs attention"
        !settings.notificationsEnabled || !reliability.notificationsGranted -> "Your alerts are turned off"
        !status.running -> "Monitoring is on standby"
        !status.hasFix -> "Finding your location…"
        status.speedMps >= MonitoringPolicy.FAST_SPEED_MPS -> "Driving · faster checks"
        status.speedMps >= 1f -> "On the move · pings are on"
        else -> "Nearby pings are on"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF103C40), Color(0xFF122934))))
            .animateContentSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A LITTLE NUDGE. RIGHT ON TIME.", style = MaterialTheme.typography.labelSmall, color = mint)
                Text("Good places.\nGreat timing.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            }
            PingRadar(active = ready, color = mint)
        }
        AnimatedContent(targetState = label, label = "monitoring status") { text ->
            Text(text, style = MaterialTheme.typography.titleSmall, color = mint, fontWeight = FontWeight.SemiBold)
        }
        Text(
            when {
                ready && status.speedMps >= MonitoringPolicy.FAST_SPEED_MPS -> "Checking about every 5 seconds, with more time to reach your next stop."
                ready -> "Your errands travel with you. Checks speed up automatically when you do."
                status.running && !status.hasFix -> "Waiting for a fresh location to check your places."
                else -> "Enable location and alerts, then add an errand. We’ll watch for your places."
            },
            style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD0E3E5)
        )
        TextButton(onClick = onSettings, contentPadding = PaddingValues(0.dp)) {
            Text(if (ready) "Alert settings  →" else "Check alert setup  →", color = mint)
        }
    }
}

@Composable
private fun PingRadar(active: Boolean, color: Color) {
    val phase = if (active) {
        val transition = rememberInfiniteTransition(label = "nearby radar")
        val value by transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(2_600, easing = LinearEasing), RepeatMode.Restart), label = "ping rings")
        value
    } else 0.35f
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val middle = Offset(size.width / 2, size.height / 2)
            repeat(3) { index ->
                val progress = (phase + index / 3f) % 1f
                drawCircle(color.copy(alpha = (1f - progress) * 0.45f),
                    radius = 12.dp.toPx() + progress * 30.dp.toPx(), center = middle, style = Stroke(1.3.dp.toPx()))
            }
            drawCircle(color.copy(alpha = 0.16f), radius = 22.dp.toPx(), center = middle)
        }
        Icon(Icons.Outlined.NearMe, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
    }
}
