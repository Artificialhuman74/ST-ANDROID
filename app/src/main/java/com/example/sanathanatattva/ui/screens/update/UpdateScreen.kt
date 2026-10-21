package com.example.sanathanatattva.ui.screens.update

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sanathanatattva.data.UpdateOffer
import com.example.sanathanatattva.data.openPlayStore

/**
 * Shown right after the splash when the Play Store has a newer version (design canvas
 * "Update, consumer"). "Update now" opens the Play Store page; "Not now" carries on for this
 * launch. When this build is below the server's minimum there is no "Not now".
 */
@Composable
fun UpdateScreen(offer: UpdateOffer, onLater: () -> Unit) {
    val context = LocalContext.current
    BackHandler {
        if (offer.required) (context as? Activity)?.finish() else onLater()
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.padding(bottom = 8.dp).size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                    }
                }
                Text(
                    if (offer.required) "UPDATE NEEDED" else "UPDATE AVAILABLE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (offer.required) "Update to keep ordering" else "A new version is ready",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (offer.required) "This version of the app has stopped working with our shop. Updating takes about a minute."
                    else "Update Sanathana Tattva from the Play Store to get the newest features and fixes.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text(
                        "Your cart, orders and login stay as they are.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { openPlayStore(context) },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(if (offer.required) "Update on Play Store" else "Update now", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                if (!offer.required) {
                    TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Not now", fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
