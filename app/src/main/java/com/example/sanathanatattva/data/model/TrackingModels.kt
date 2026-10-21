package com.example.sanathanatattva.data.model

import kotlinx.serialization.Serializable

// ---------- Live order tracking (GET api/consumer/orders/{id}/tracking) ----------

@Serializable
data class GeoPoint(val lat: Double, val lng: Double)

@Serializable
data class LiveFix(val lat: Double, val lng: Double, val at: String? = null)

/** The shop that packs the order; usually the customer's linked partner. */
@Serializable
data class TrackingShop(
    val name: String = "",
    val area: String? = null,
    val is_linked: Boolean = true,
    val location: GeoPoint? = null,
)

/** Who brings it to the door: a rider, or the shop itself. */
@Serializable
data class TrackingCourier(
    val kind: String = "rider",
    val name: String = "",
    val vehicle: String? = null,
    val plate: String? = null,
    val has_photo: Boolean = false,
    val live: LiveFix? = null,
)

/** One step of the order's history. state: done | now | next. */
@Serializable
data class TrackingEvent(
    val key: String = "",
    val title: String = "",
    val detail: String? = null,
    val at: String? = null,
    val state: String = "done",
)

@Serializable
data class OrderTracking(
    val order_id: Long = 0,
    val state: String = "placed",
    val step: Int = 0,
    val headline: String = "",
    val note: String? = null,
    val placed_at: String? = null,
    val linked_partner: TrackingShop? = null,
    val delivery_partner: TrackingCourier? = null,
    val same_person: Boolean = false,
    val home: GeoPoint? = null,
    val events: List<TrackingEvent> = emptyList(),
    val distance_km: Double? = null,
    val delivery_code: String? = null,
    val can_call: Boolean = false,
)

@Serializable
data class CallResponse(val phone: String = "")

@Serializable
data class RoutePoints(val points: List<List<Double>> = emptyList(), val metres: Double = 0.0, val seconds: Double = 0.0)

@Serializable
data class AppVersionRule(val min_version_code: Int = 0)

@Serializable
data class AppVersions(val consumer: AppVersionRule = AppVersionRule(), val delivery: AppVersionRule = AppVersionRule())
