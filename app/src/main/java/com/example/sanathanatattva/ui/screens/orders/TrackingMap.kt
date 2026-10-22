package com.example.sanathanatattva.ui.screens.orders

import android.graphics.PointF
import org.maplibre.android.MapLibre
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.sanathanatattva.data.model.GeoPoint
import com.example.sanathanatattva.ui.components.rememberReducedMotion
import com.example.sanathanatattva.ui.theme.Gold700
import com.example.sanathanatattva.ui.theme.Green600
import com.example.sanathanatattva.ui.theme.Ink
import com.example.sanathanatattva.ui.theme.OutlineGreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.roundToInt

/** Same OpenStreetMap style as the ST Delivery app (assets/map). */
private const val MAP_STYLE_URL = "asset://map/st-google.json"

/**
 * What the order map shows. [shop] is the linked partner's place; [mover] is the delivery
 * partner's last live fix. When the shop is delivering itself ([samePerson]) the two are one
 * marker that moves.
 */
data class TrackingPins(
    val shop: GeoPoint?,
    val mover: GeoPoint?,
    val home: GeoPoint?,
    val samePerson: Boolean,
    /** The road still to go, mover → home (from our OSRM); empty draws nothing. */
    val route: List<GeoPoint> = emptyList(),
) {
    val isEmpty get() = shop == null && mover == null && home == null
}

/**
 * A live OpenStreetMap map of the order: the linked partner's shop (gold ring), the delivery
 * partner (green, pulsing, glides between fixes) and the customer's home (black). It frames
 * all of them by itself; [interactive] turns on panning and zoom for the full-screen view.
 */
@Composable
fun TrackingMap(pins: TrackingPins, modifier: Modifier = Modifier, interactive: Boolean = false) {
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var frame by remember { mutableIntStateOf(0) } // bumps as the camera moves, to re-place markers

    val first = pins.mover ?: pins.shop ?: pins.home ?: return
    LiveMapView(
        start = LatLng(first.lat, first.lng),
        onReady = { m ->
            m.uiSettings.setAllGesturesEnabled(interactive)
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.uiSettings.isCompassEnabled = false
            m.uiSettings.isLogoEnabled = false
            m.addOnCameraMoveListener { frame++ }
            m.addOnCameraIdleListener { frame++ }
            map = m
        },
        modifier = modifier,
    )

    // Lines: the road still to go once someone is moving, else a dotted shop → home.
    LaunchedEffect(map, pins.route, pins.shop, pins.home, pins.mover) {
        val style = map?.style ?: return@LaunchedEffect
        val road = pins.route.takeIf { it.size >= 2 && pins.mover != null }
        val hint = if (road == null && pins.mover == null && pins.shop != null && pins.home != null) listOf(pins.shop, pins.home) else null
        setLine(style, "road", road, Green600, dotted = false)
        setLine(style, "hint", hint, OutlineGreen, dotted = true)
    }

    // Frame everything once, and again whenever a marker moves out of the old frame.
    LaunchedEffect(map, pins.shop, pins.mover, pins.home, pins.samePerson) {
        val m = map ?: return@LaunchedEffect
        val points = listOfNotNull(if (pins.samePerson && pins.mover != null) null else pins.shop, pins.mover, pins.home)
            .map { LatLng(it.lat, it.lng) }.distinct()
        runCatching {
            if (points.size == 1) m.easeCamera(CameraUpdateFactory.newLatLngZoom(points[0], 15.0), 600)
            else m.easeCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), 72, 72, 72, 72), 600)
        }
    }

    val m = map ?: return
    // Keyed on the camera frame so the markers stay pinned to the map as it moves.
    key(frame) {
        fun place(p: GeoPoint): PointF? = runCatching { m.projection.toScreenLocation(LatLng(p.lat, p.lng)) }.getOrNull()
        pins.home?.let { p -> place(p)?.let { Marker(it) { HomeMarker() } } }
        if (!(pins.samePerson && pins.mover != null)) pins.shop?.let { p -> place(p)?.let { Marker(it) { ShopMarker() } } }
        pins.mover?.let { p ->
            place(p)?.let { GlidingMarker(it) { if (pins.samePerson) ShopDeliveringMarker() else RiderMarker() } }
        }
    }
}

@Composable
private fun LiveMapView(start: LatLng, onReady: (MapLibreMap) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        // Started here rather than at app launch: only this screen shows a map. Safe to repeat.
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder().target(start).zoom(14.0).build()
                map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { onReady(map) }
            }
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        // Already started by the time this joins the screen.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun setLine(style: Style, id: String, points: List<GeoPoint>?, color: Color, dotted: Boolean) {
    val source = style.getSourceAs<GeoJsonSource>(id) ?: GeoJsonSource(id).also { style.addSource(it) }
    val line = points.orEmpty().map { Point.fromLngLat(it.lng, it.lat) }
    source.setGeoJson(if (line.size >= 2) Feature.fromGeometry(LineString.fromLngLats(line)) else Feature.fromGeometry(LineString.fromLngLats(emptyList())))
    if (style.getLayer("$id-line") != null) return
    val props = mutableListOf<PropertyValue<*>>(
        PropertyFactory.lineColor(color.toArgb()),
        PropertyFactory.lineWidth(if (dotted) 3f else 5f),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    )
    if (dotted) props += PropertyFactory.lineDasharray(arrayOf(0.5f, 3f))
    style.addLayer(LineLayer("$id-line", id).withProperties(*props.toTypedArray()))
}

/** Centres [content] on a screen point. */
@Composable
private fun Marker(at: PointF, content: @Composable () -> Unit) {
    Box(
        Modifier
            .offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(0, 0) { p.place(-p.width / 2, -p.height / 2) }
            },
    ) { content() }
}

/** The delivery partner slides to each new fix instead of jumping. */
@Composable
private fun GlidingMarker(at: PointF, content: @Composable () -> Unit) {
    val reduce = rememberReducedMotion()
    val x = remember { Animatable(at.x) }
    val y = remember { Animatable(at.y) }
    LaunchedEffect(at.x, at.y) {
        if (reduce) { x.snapTo(at.x); y.snapTo(at.y); return@LaunchedEffect }
        coroutineScope {
            launch { x.animateTo(at.x, tween(900, easing = FastOutSlowInEasing)) }
            launch { y.animateTo(at.y, tween(900, easing = FastOutSlowInEasing)) }
        }
    }
    Marker(PointF(x.value, y.value), content)
}

// Gold marks the shop here (approved on the design canvas) so it reads apart from the
// delivery partner's green; everywhere else in the app gold still means money.

@Composable
internal fun ShopMarker(size: Int = 44) {
    Box(
        Modifier
            .size(size.dp)
            .shadow(3.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(3.dp, Gold700, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Gold700, modifier = Modifier.size((size / 2).dp)) }
}

/** The delivery partner: a green disc that sends out a soft ring while live. */
@Composable
internal fun RiderMarker() {
    PulseRing(Modifier.size(52.dp)) {
        Box(
            Modifier
                .size(52.dp)
                .shadow(4.dp, CircleShape)
                .background(Green600, CircleShape)
                .border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) }
    }
}

/** The shop delivering itself: the shop marker with a scooter badge, pulsing as it moves. */
@Composable
internal fun ShopDeliveringMarker() {
    PulseRing(Modifier.size(56.dp)) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            ShopMarker(size = 48)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .background(Green600, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.TwoWheeler, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
internal fun HomeMarker() {
    Box(
        Modifier
            .size(40.dp)
            .shadow(3.dp, CircleShape)
            .background(Ink, CircleShape)
            .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun PulseRing(modifier: Modifier, content: @Composable () -> Unit) {
    val reduce = rememberReducedMotion()
    val t by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse-t",
    )
    Box(
        modifier.drawBehind {
            if (!reduce) {
                val p = FastOutSlowInEasing.transform(t)
                drawCircle(Green600.copy(alpha = 0.35f * (1f - p)), radius = size.minDimension / 2 * (1f + 0.6f * p))
            }
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Little legend dots under the map. */
@Composable
internal fun LegendDot(kind: String) {
    val m = Modifier.size(12.dp)
    when (kind) {
        "shop" -> Box(m.background(Color.White, CircleShape).border(3.dp, Gold700, CircleShape))
        "rider" -> Box(m.background(Green600, CircleShape))
        else -> Box(m.background(Ink, CircleShape))
    }
}
