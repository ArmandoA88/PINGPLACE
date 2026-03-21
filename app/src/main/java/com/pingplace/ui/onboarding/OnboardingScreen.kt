package com.pingplace.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingplace.ui.theme.CreamSurface
import com.pingplace.ui.theme.MeadowGreen
import com.pingplace.ui.theme.PingPlaceTheme
import com.pingplace.ui.theme.Sand

@Composable
fun OnboardingScreen(
    innerPadding: PaddingValues,
    onRequestNotifications: () -> Unit,
    onRequestFineLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onContinue: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CreamSurface, Sand)))
            .padding(innerPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Text("PingPlace", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Reminders that show up when you show up.",
                style = MaterialTheme.typography.titleLarge,
                color = MeadowGreen
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "PingPlace reminds you when you are near a brand or place type where you already have something to do.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        item {
            OnboardingCard(
                icon = { Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(28.dp)) },
                title = "Brand-based reminders",
                body = "Whole Foods means any Whole Foods nearby. Same idea for Costco, Target, UPS Store, CVS, and custom chains."
            )
        }
        item {
            OnboardingCard(
                icon = { Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(28.dp)) },
                title = "Smart grouped alerts",
                body = "If you have two errands for the same brand, PingPlace combines them into one useful notification."
            )
        }
        item {
            OnboardingCard(
                icon = { Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(28.dp)) },
                title = "Blocked times",
                body = "Busy hours, school, sleep, or work windows can suppress reminders until the timing makes sense."
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRequestNotifications, modifier = Modifier.weight(1f)) {
                    Text("Allow alerts")
                }
                Button(onClick = onRequestFineLocation, modifier = Modifier.weight(1f)) {
                    Text("Allow location")
                }
            }
        }
        item {
            Button(onClick = onRequestBackgroundLocation, modifier = Modifier.fillMaxWidth()) {
                Text("Allow background location")
            }
        }
        item {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text("Start using PingPlace")
            }
        }
    }
}

@Composable
private fun OnboardingCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            icon()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    PingPlaceTheme {
        OnboardingScreen(
            innerPadding = PaddingValues(),
            onRequestNotifications = {},
            onRequestFineLocation = {},
            onRequestBackgroundLocation = {},
            onContinue = {}
        )
    }
}
