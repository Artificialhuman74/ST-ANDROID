package com.example.sanathanatattva.ui.screens.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sanathanatattva.data.model.GeoPoint
import com.example.sanathanatattva.data.model.OrderTracking
import com.example.sanathanatattva.data.model.TrackingEvent
import com.example.sanathanatattva.ui.components.cardBorder
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.screens.luckydraw.drawTimestamp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The top of an order (design canvas "Order detail"): where it is now, a live map of the
 * linked partner's shop, the delivery partner and home, the delivery code, who is handling
 * it (one card when the shop delivers itself, two when a rider does), and an expandable
 * history of every step with its time.
 */
@Composable
fun LiveTrackingSection(
    tracking: OrderTracking,
    trackedAt: Long,
    route: List<GeoPoint>,
    calling: Boolean,
    onCall: () -> Unit,
) {
    val finished = tracking.state == "delivered" || tracking.state == "cancelled"
    val courier = tracking.delivery_partner
    val shop = tracking.linked_partner
    val pins = TrackingPins(
        shop = shop?.location,
        mover = courier?.live?.let { GeoPoint(it.lat, it.lng) },
        home = tracking.home,
        samePerson = tracking.same_person,
        route = route,
    )
    var fullMap by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        StatusHeader(tracking, trackedAt)

        if (!finished && !pins.isEmpty) {
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                TrackingMap(pins, Modifier.fillMaxSize())
                if (pins.mover == null && shop?.location != null && tracking.home != null) {
                    MapChip(
                        "${km(shop.location, tracking.home)} km from your shop",
                        Modifier.align(Alignment.TopStart).padding(12.dp),
                    )
                }
                Surface(
                    onClick = { fullMap = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.OpenInFull, contentDescription = "Open map full screen", modifier = Modifier.size(20.dp))
                    }
                }
            }
            Legend(pins)
        }

        tracking.delivery_code?.let { code ->
            Spacer(Modifier.height(16.dp))
            DeliveryCode(code, courier?.name?.takeIf { courier.kind == "rider" })
        }

        if (shop != null || courier != null) {
            Text(
                "Who’s handling your order",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            PartnersCard(tracking, finished, calling, onCall)
        }

        if (tracking.events.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            HistoryCard(tracking.events, finished)
        }
    }

    if (fullMap) {
        Dialog(
            onDismissRequest = { fullMap = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                TrackingMap(pins, Modifier.fillMaxSize(), interactive = true)
                Surface(
                    onClick = { fullMap = false },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp).size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Close map")
                    }
                }
                Box(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        Box(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) { Legend(pins) }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Status: chip, headline, one line of detail
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun StatusHeader(t: OrderTracking, trackedAt: Long) {
    val live = t.delivery_partner?.live != null && (t.state == "on_the_way" || t.state == "rider_coming")
    // Ticks so "Updated 2 min ago" stays true between refreshes.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StateChip(t.state, live)
            val mins = ((now - trackedAt) / 60_000).coerceAtLeast(0)
            Text(
                when {
                    t.state == "delivered" || t.state == "cancelled" -> ""
                    !live -> "Updates by itself"
                    mins < 1 -> "Updated just now"
                    else -> "Updated $mins min ago"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(t.headline, style = MaterialTheme.typography.headlineSmall)
        subline(t)?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StateChip(state: String, live: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val (label, bg, fg) = when {
        live -> Triple("LIVE", scheme.primaryContainer, scheme.onPrimaryContainer)
        state == "delivered" -> Triple("DELIVERED", scheme.primaryContainer, scheme.onPrimaryContainer)
        state == "cancelled" -> Triple("CANCELLED", scheme.errorContainer, scheme.onErrorContainer)
        else -> Triple(
            when (state) {
                "packing" -> "PACKING"
                "packed" -> "PACKED"
                "rider_coming", "on_the_way" -> "ON THE WAY"
                "scheduled" -> "SCHEDULED"
                "retry" -> "TRYING AGAIN"
                else -> "PLACED"
            },
            scheme.surfaceContainer,
            scheme.onSurfaceVariant,
        )
    }
    Row(
        Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .then(if (!live && state != "delivered" && state != "cancelled") Modifier.border(1.dp, scheme.outlineVariant, RoundedCornerShape(13.dp)) else Modifier)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            live -> LiveDot(scheme.primary)
            state == "delivered" -> Icon(Icons.Filled.Check, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            state == "packing" || state == "packed" -> Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            state == "scheduled" || state == "retry" || state == "placed" -> Icon(Icons.Outlined.Schedule, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        }
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
    }
}

@Composable
private fun LiveDot(color: Color) {
    val reduce = rememberReducedMotion()
    val t by rememberInfiniteTransition(label = "live").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "live-t",
    )
    Box(
        Modifier.size(8.dp).drawBehind {
            if (!reduce) drawCircle(color.copy(alpha = 0.5f * (1f - t)), radius = size.minDimension / 2 * (1f + 1.2f * t))
            drawCircle(color)
        },
    )
}

/** The one line under the headline: how far, or when the last thing happened. */
private fun subline(t: OrderTracking): String? {
    val shop = t.linked_partner?.name ?: "your shop"
    fun at(key: String) = t.events.firstOrNull { it.key == key }?.at?.let(::clockTime)
    val km = t.distance_km?.let { "${fmtKm(it)} km away" }
    return when (t.state) {
        "on_the_way" -> when {
            t.delivery_partner?.kind == "rider" -> listOfNotNull(km, at("picked_up")?.let { "picked up from $shop at $it" })
            else -> listOfNotNull(km, at("out")?.let { "left the shop at $it" })
        }.joinToString(" · ").ifEmpty { null }
        "rider_coming" -> "On the way to $shop to pick it up"
        "packing" -> at("accepted")?.let { "$shop accepted it at $it" }
        "packed" -> at("packed")?.let { "$shop packed it at $it" }
        "placed" -> t.placed_at?.let { "Placed ${dayTime(it)}" }
        "delivered" -> t.events.firstOrNull { it.key == "delivered" }?.at?.let { "Delivered ${dayTime(it)}" }
        else -> t.note
    }
}

// ─────────────────────────────────────────────────────────────────────
// Map extras
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun MapChip(text: String, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(pins: TrackingPins) {
    FlowRow(
        Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        @Composable
        fun item(kind: String, label: String) = Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LegendDot(kind)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            pins.samePerson && pins.mover != null -> item("shop", "Linked partner, delivering (live)")
            else -> {
                if (pins.shop != null) item("shop", "Linked partner")
                if (pins.mover != null) item("rider", "Delivery partner (live)")
            }
        }
        if (pins.home != null) item("home", "You")
    }
}

@Composable
private fun DeliveryCode(code: String, riderName: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Delivery code", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "Tell ${riderName ?: "them"} this code at your door",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            code,
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp, fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// Who's handling it: one card, one row per person (one row when it's the same person)
// ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartnersCard(t: OrderTracking, finished: Boolean, calling: Boolean, onCall: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shop = t.linked_partner
    val courier = t.delivery_partner
    val shopRole = if (shop?.is_linked != false) "LINKED PARTNER" else "PARTNER SHOP"
    val packed = t.events.any { it.key == "packed" && it.state != "next" }
    val where = listOfNotNull(
        shop?.area?.takeIf { it.isNotBlank() },
        if (shop?.location != null && t.home != null) "${km(shop.location, t.home)} km away" else null,
    ).joinToString(", ")

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerLowest)
            .border(cardBorder(), RoundedCornerShape(16.dp)),
    ) {
        if (t.same_person && shop != null && courier != null) {
            // The shop packs it and brings it: one person, one row, both roles.
            PersonRow(
                avatar = { SameAvatar() },
                role = null,
                name = shop.name,
                detail = "Same person from packing to your door",
                chips = { RoleChip("Linked partner", scheme.tertiaryContainer, scheme.onTertiaryContainer); RoleChip("Delivery partner", scheme.primaryContainer, scheme.onPrimaryContainer) },
                call = if (t.can_call) ({ CallButton(shop.name, calling, onCall) }) else null,
            )
            return@Column
        }
        if (shop != null) {
            PersonRow(
                avatar = { IconAvatar(Icons.Outlined.Storefront, scheme.tertiaryContainer, scheme.tertiary) },
                role = shopRole to scheme.tertiary,
                name = shop.name,
                detail = listOf(if (packed || finished) "Packed your order" else "Packing your order now", where)
                    .filter { it.isNotBlank() }.joinToString(" · "),
            )
        }
        if (shop != null && (courier != null || !finished)) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = scheme.surfaceVariant)
        }
        when {
            courier != null -> PersonRow(
                avatar = {
                    if (courier.kind == "rider") InitialAvatar(courier.name)
                    else IconAvatar(Icons.Filled.TwoWheeler, scheme.primary, scheme.onPrimary)
                },
                role = "DELIVERY PARTNER" to scheme.primary,
                name = courier.name,
                detail = listOfNotNull(courier.vehicle, courier.plate?.let(::spacedPlate)).joinToString(" · ")
                    .ifEmpty { if (finished) "Delivered your order" else "Bringing your order" },
                call = if (t.can_call) ({ CallButton(courier.name, calling, onCall) }) else null,
            )
            !finished -> PersonRow(
                avatar = { DashedAvatar() },
                role = "DELIVERY PARTNER" to scheme.onSurfaceVariant,
                name = "Not assigned yet",
                detail = "You’ll see who’s coming, and where they are, once it’s packed.",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonRow(
    avatar: @Composable () -> Unit,
    role: Pair<String, Color>?,
    name: String,
    detail: String,
    chips: (@Composable () -> Unit)? = null,
    call: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        avatar()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            role?.let { (label, color) ->
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp, color = color)
            }
            if (chips != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { chips() }
            }
            Text(name, style = MaterialTheme.typography.titleMedium)
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        call?.invoke()
    }
}

@Composable
private fun RoleChip(text: String, bg: Color, fg: Color) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        color = fg,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun CallButton(name: String, calling: Boolean, onCall: () -> Unit) {
    OutlinedIconButton(
        onClick = onCall,
        enabled = !calling,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(48.dp),
    ) {
        if (calling) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Icon(Icons.Filled.Call, contentDescription = "Call $name", tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun IconAvatar(icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, fg: Color) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun InitialAvatar(name: String) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Shop with a scooter badge: the linked partner is also the one delivering. */
@Composable
private fun SameAvatar() {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(52.dp)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(scheme.tertiaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = scheme.tertiary, modifier = Modifier.size(24.dp))
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerLowest)
                .padding(2.dp)
                .clip(CircleShape)
                .background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(13.dp)) }
    }
}

@Composable
private fun DashedAvatar() {
    val line = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier.size(48.dp).drawBehind {
            drawCircle(line, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
        },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp)) }
}

// ─────────────────────────────────────────────────────────────────────
// History: collapsed shows where it is now; open shows every step with its time
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryCard(events: List<TrackingEvent>, finished: Boolean) {
    var open by rememberSaveable { mutableStateOf(false) }
    val reduce = rememberReducedMotion()
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(if (reduce) 0 else 200), label = "chevron")
    val current = events.lastOrNull { it.state == "now" } ?: events.lastOrNull { it.state == "done" }
    val doneCount = events.count { it.state != "next" }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(cardBorder(), RoundedCornerShape(16.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClickLabel = if (open) "Hide the steps" else "Show every step") { open = !open }
                .semantics { stateDescription = if (open) "Expanded" else "Collapsed" }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("What happened to your order", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        open -> "Hide the steps"
                        finished -> "${events.size} steps · tap to see each one"
                        else -> "$doneCount of ${events.size} steps · tap to see each one"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.rotate(turn))
        }
        AnimatedVisibility(
            visible = open,
            enter = if (reduce) fadeIn(tween(0)) else expandVertically() + fadeIn(),
            exit = if (reduce) fadeOut(tween(0)) else shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                events.forEachIndexed { i, e ->
                    StepRow(e, nextState = events.getOrNull(i + 1)?.state)
                }
            }
        }
        if (!open && current != null) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                StepRow(current.copy(state = if (finished) "done" else "now"), nextState = null)
            }
        }
    }
}

@Composable
private fun StepRow(e: TrackingEvent, nextState: String?) {
    val scheme = MaterialTheme.colorScheme
    val upcoming = e.state == "next"
    val now = e.state == "now"
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.width(12.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .size(12.dp)
                    .then(if (now) Modifier.drawBehind { drawCircle(scheme.primaryContainer, radius = size.minDimension / 2 + 4.dp.toPx()) } else Modifier)
                    .clip(CircleShape)
                    .then(
                        if (upcoming) Modifier.background(scheme.surfaceContainerLowest).border(2.dp, scheme.outlineVariant, CircleShape)
                        else Modifier.background(scheme.primary),
                    ),
            )
            if (nextState != null) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(if (nextState == "next") scheme.surfaceVariant else scheme.primary.copy(alpha = 0.45f)),
                )
            }
        }
        Column(Modifier.weight(1f).padding(bottom = if (nextState != null) 14.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                e.title,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = if (now) FontWeight.SemiBold else FontWeight.Medium,
                color = if (upcoming) scheme.onSurfaceVariant else scheme.onSurface,
            )
            val line = listOfNotNull(e.at?.let(::dayTime), e.detail, if (now) "now" else null).joinToString(" · ")
            if (line.isNotEmpty()) {
                Text(line, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Formatting
// ─────────────────────────────────────────────────────────────────────

private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
private val IN = Locale.forLanguageTag("en-IN")

/** "10:52 am" */
fun clockTime(iso: String): String? = drawTimestamp(iso)?.let {
    SimpleDateFormat("h:mm a", IN).apply { timeZone = IST }.format(Date(it)).lowercase(IN)
}

/** "10:52 am" today, "yesterday, 10:52 am", else "19 Oct, 10:52 am". */
fun dayTime(iso: String): String? {
    val ms = drawTimestamp(iso) ?: return null
    val day = SimpleDateFormat("yyyyMMdd", IN).apply { timeZone = IST }
    val today = day.format(Date())
    val that = day.format(Date(ms))
    val yesterday = day.format(Date(System.currentTimeMillis() - 86_400_000))
    val clock = clockTime(iso) ?: return null
    return when (that) {
        today -> "today, $clock"
        yesterday -> "yesterday, $clock"
        else -> SimpleDateFormat("d MMM", IN).apply { timeZone = IST }.format(Date(ms)) + ", " + clock
    }
}

private fun fmtKm(v: Double) = String.format(Locale.ROOT, "%.1f", v)

/** Straight-line distance, one decimal. */
private fun km(a: GeoPoint, b: GeoPoint): String {
    val r = 6371.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return fmtKm(2 * r * asin(sqrt(h)))
}

/** "KA05MX4821" → "KA 05 MX 4821", easier to match against a number plate. */
private fun spacedPlate(p: String): String {
    val m = Regex("^([A-Z]{2})(\\d{1,2})([A-Z]{0,3})(\\d{1,4})$").find(p.uppercase().replace(" ", "")) ?: return p
    return m.groupValues.drop(1).filter { it.isNotEmpty() }.joinToString(" ")
}
