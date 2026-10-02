package com.example.sanathanatattva.data.model

import kotlinx.serialization.Serializable

// ---------- Auth ----------

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val referral_code: String? = null,
)

@Serializable
data class RegisterResponse(
    val success: Boolean = false,
    val message: String? = null,
    val dev_otp: String? = null,
)

@Serializable
data class VerifyOtpRequest(val email: String, val otp: String)

@Serializable
data class ResendVerificationRequest(val email: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

/**
 * Passwordless email login: request a one-time code.
 * Contract: POST api/auth/consumer/email/send-otp — backend pending.
 */
@Serializable
data class SendEmailOtpRequest(val email: String)

@Serializable
data class SendEmailOtpResponse(
    val success: Boolean = false,
    val message: String? = null,
    val dev_otp: String? = null,
)

@Serializable
data class AuthResponse(
    val success: Boolean = true,
    val token: String? = null,
    val consumer: Consumer? = null,
)

@Serializable
data class Consumer(
    val id: Long,
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val status: String = "active",
    val email_verified: Int = 0,
    val linked_dealer_id: Long? = null,
    val referral_code_used: String? = null,
    // Auth methods the account has. The backend derives these from the presence
    // of a password / linked Google account and never sends the credentials.
    // Default has_password = true so email accounts keep their controls if an
    // older backend omits the field; has_google defaults false.
    val has_password: Boolean = true,
    val has_google: Boolean = false,
    val dealer: LinkedDealer? = null,
    val parentDealer: LinkedDealer? = null,
)

@Serializable
data class LinkedDealer(
    val id: Long? = null,
    val name: String? = null,
    val phone: String? = null,
    val tier: Int? = null,
    val referral_code: String? = null,
)

@Serializable
data class ConsumerResponse(val consumer: Consumer)

@Serializable
data class UpdateProfileRequest(val name: String? = null, val phone: String? = null)

@Serializable
data class ChangePasswordRequest(val old_password: String, val new_password: String)

@Serializable
data class GoogleAuthRequest(val id_token: String)

@Serializable
data class ValidateDealerResponse(
    val valid: Boolean = false,
    val dealerName: String? = null,
    val tier: Int? = null,
)

// ---------- Products ----------

@Serializable
data class Product(
    val id: Long,
    val name: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val container_cost: Double? = null,
    val container_type: String? = null,
    val category: String = "",
    val unit: String = "",
    val stock: Int = 0,
    val sku: String? = null,
    val image_url: String? = null,
    val image_urls: String? = null,
    val status: String = "active",
)

@Serializable
data class ProductsResponse(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
)

@Serializable
data class ProductResponse(val product: Product)

@Serializable
data class RefillCapsResponse(val caps: Map<String, Int> = emptyMap())

// ---------- Reviews ----------

@Serializable
data class Review(
    val id: Long,
    val consumer_name: String = "",
    val rating: Int = 0,
    val body: String? = null,
    val verified_buyer: Int = 0,
    val created_at: String = "",
)

@Serializable
data class ReviewsResponse(
    val reviews: List<Review> = emptyList(),
    val average_rating: Double? = null,
    val count: Int = 0,
)

@Serializable
data class PostReviewRequest(val rating: Int, val body: String? = null)

// ---------- Containers ----------

@Serializable
data class ContainerHolding(
    val id: Long,
    val source: String = "purchase",
    val container_type: String = "",
    val current_product_id: Long? = null,
    val current_product_name: String? = null,
    val status: String = "",
    val acquired_at: String? = null,
    val deposit_amount: Double = 0.0,
    val forfeit_window_end: String? = null,
)

@Serializable
data class SwappableProduct(
    val id: Long,
    val name: String = "",
    val unit: String = "",
    val container_type: String? = null,
    val price: Double = 0.0,
)

@Serializable
data class ContainersResponse(
    val held: List<ContainerHolding> = emptyList(),
    val history: List<ContainerHolding> = emptyList(),
    val swappable: List<SwappableProduct> = emptyList(),
    val support_whatsapp_number: String? = null,
)

@Serializable
data class RefundRequest(val destination: String, val notes: String? = null)

@Serializable
data class SwapRequest(val target_product_id: Long)

@Serializable
data class DisputeRequest(val notes: String? = null)

// ---------- Wallet ----------

@Serializable
data class LedgerEntry(
    val id: Long,
    val type: String = "",
    val amount: Double = 0.0,
    val reason: String? = null,
    val order_id: Long? = null,
    val created_at: String = "",
)

@Serializable
data class StoreCreditResponse(
    val balance: Double = 0.0,
    val ledger: List<LedgerEntry> = emptyList(),
)

// ---------- Addresses ----------

@Serializable
data class Address(
    val id: Long,
    val label: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val pincode: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_default: Int = 0,
)

@Serializable
data class AddressesResponse(val addresses: List<Address> = emptyList())

@Serializable
data class AddressResponse(val address: Address, val geocoded: Boolean = false)

@Serializable
data class SaveAddressRequest(
    val label: String,
    val name: String,
    val phone: String,
    val address: String,
    val pincode: String,
    val is_default: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

// ---------- Orders ----------

@Serializable
data class OrderItemRequest(
    val product_id: Long,
    val quantity: Int,
    val is_refill: Boolean = false,
)

@Serializable
data class PlaceOrderRequest(
    val items: List<OrderItemRequest>,
    val address_id: Long? = null,
    val delivery_address: String? = null,
    val pincode: String? = null,
    val delivery_name: String? = null,
    val delivery_phone: String? = null,
    val save_address: Boolean? = null,
    val make_default_address: Boolean? = null,
    val address_label: String? = null,
    val notes: String? = null,
    val referral_code: String? = null,
    val store_credit_to_apply: Double? = null,
)

@Serializable
data class Order(
    val id: Long,
    val order_number: String = "",
    val status: String = "pending",
    val payment_status: String = "pending",
    val subtotal: Double = 0.0,
    val discount_percent: Double = 0.0,
    val discount_amount: Double = 0.0,
    val container_costs_total: Double = 0.0,
    val store_credit_applied: Double = 0.0,
    val total_amount: Double = 0.0,
    val pincode: String? = null,
    val delivery_address: String? = null,
    val notes: String? = null,
    val is_direct: Int = 0,
    val dealer_name: String? = null,
    val dealer_phone: String? = null,
    val delivery_dealer_name: String? = null,
    val delivery_dealer_phone: String? = null,
    val created_at: String = "",
    val item_count: Int = 0,
    val items: List<OrderItem> = emptyList(),
)

@Serializable
data class OrderItem(
    val id: Long,
    val product_id: Long,
    val quantity: Int = 0,
    val price: Double = 0.0,
    val total: Double = 0.0,
    val container_cost: Double = 0.0,
    val is_refill: Int = 0,
    val product_name: String = "",
    val image_url: String? = null,
    val unit: String? = null,
)

@Serializable
data class PlaceOrderResponse(
    val order: Order,
    val discount: DiscountInfo? = null,
    val delivery: DeliveryInfo? = null,
    val confirmation: ConfirmationInfo? = null,
)

@Serializable
data class DiscountInfo(val percent: Double = 0.0, val amount: Double = 0.0)

@Serializable
data class DeliveryInfo(
    val dealerId: Long? = null,
    val dealerName: String? = null,
    val dealerPhone: String? = null,
    val distanceKm: Double? = null,
)

@Serializable
data class ConfirmationInfo(
    val orderNumber: String? = null,
    val message: String? = null,
)

@Serializable
data class OrdersResponse(val orders: List<Order> = emptyList())

@Serializable
data class OrderDetailResponse(val order: Order, val items: List<OrderItem> = emptyList())

// ---------- Payments ----------

@Serializable
data class CreatePaymentRequest(val consumer_order_id: Long)

@Serializable
data class CreatePaymentResponse(
    val razorpay_order_id: String,
    val amount: Long,
    val currency: String = "INR",
    val key_id: String,
)

@Serializable
data class VerifyPaymentRequest(
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String,
    val consumer_order_id: Long,
)

// ---------- Misc ----------

@Serializable
data class SettingsResponse(val referral_discount_percent: Double = 0.0)

@Serializable
data class SuccessResponse(val success: Boolean = true, val ok: Boolean = true)

@Serializable
data class ApiError(val error: String? = null)
