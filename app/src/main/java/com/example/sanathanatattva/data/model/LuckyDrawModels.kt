package com.example.sanathanatattva.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LinkDealerRequest(val referral_code: String)

@Serializable
data class LuckyDrawStatus(
    val active_enrollment: DrawEnrollment? = null,
    val open_schemes: List<DrawScheme> = emptyList(),
    val draw_schedule: DrawSchedule = DrawSchedule(),
    val cycle: DrawCycle? = null,
) {
    val enrollment: DrawEnrollment?
        get() = active_enrollment?.takeIf { it.status == "active" || it.status == "won" }
}

@Serializable
data class DrawScheme(
    val id: Long,
    val name: String,
    val joining_fee: Double,
    val enrollment_closes_at: String? = null,
)

@Serializable
data class DrawSchedule(val day: Int = 12, val time: String = "17:00")

@Serializable
data class DrawEnrollment(
    val id: Long,
    val scheme_id: Long,
    val scheme_name: String = "",
    val scheme_status: String = "",
    val status: String,
    val enrolled_at: String? = null,
    val reward_product_name: String? = null,
    val welcome_gift_order_id: Long? = null,
)

@Serializable
data class DrawCycle(
    val enrollment_id: Long,
    val scheme_id: Long,
    val scheme_name: String,
    val month_number: Int = 1,
    val total_months: Int = 12,
    val round_over: Boolean = false,
    val month_key: String = "",
    val entry_deadline: String? = null,
    val draw_at: String? = null,
    val next_draw_at: String? = null,
    val next_entry_deadline: String? = null,
    val entered: Boolean = false,
    val entry_order_number: String? = null,
    val tickets: Int = 1,
    val qualified_months: Int = 0,
    val won: Boolean = false,
    val latest_draw: DrawResult? = null,
    val reward: DrawReward? = null,
) {
    // months_remaining excludes the delivery due THIS month, including month twelve.
    val canClaim: Boolean get() = won && !round_over && reward?.this_month_ordered == false
}

@Serializable
data class DrawResult(
    val month_key: String,
    val month_number: Int = 0,
    val ran_at: String? = null,
    val has_winner: Boolean = true,
    val is_me: Boolean = false,
    val winner_display_name: String? = null,
    val reward_product_name: String? = null,
)

@Serializable
data class DrawReward(
    val product_name: String? = null,
    val quantity: Int = 1,
    val won_at_month: Int = 1,
    val months_remaining: Int = 0,
    val this_month_ordered: Boolean = false,
    val this_month_order_number: String? = null,
    val this_month_delivery_status: String? = null,
    val this_month_delivered: Boolean = false,
    val this_month_delivered_at: String? = null,
)

@Serializable
data class WelcomeGiftsResponse(val product: Product? = null, val products: List<Product> = emptyList())

@Serializable
data class DrawWinner(
    val id: Long,
    val display_name: String,
    val scheme_name: String,
    val reward_product_name: String? = null,
)

@Serializable
data class DrawWinnersResponse(val winners: List<DrawWinner> = emptyList())

@Serializable
data class EnrollDrawRequest(val scheme_id: Long, val welcome_gift: Boolean)

@Serializable
data class EnrollDrawResponse(
    val mocked: Boolean = false,
    val enrollment_id: Long? = null,
    val razorpay_order_id: String? = null,
    val amount: Long? = null,
    val currency: String = "INR",
    val key_id: String? = null,
    val order_number: String? = null,
    val scheme_name: String? = null,
    val welcome_gift: String? = null,
)

@Serializable
data class VerifyDrawRequest(
    val enrollment_id: Long,
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String,
)

@Serializable
data class DrawActionResponse(
    val success: Boolean = false,
    val alreadyProcessed: Boolean = false,
    val order_number: String? = null,
    val scheme_name: String? = null,
    val welcome_gift: String? = null,
    val containers_returned: Int = 0,
)
