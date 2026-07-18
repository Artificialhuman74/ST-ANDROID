package com.example.sanathanatattva.data.api

import com.example.sanathanatattva.data.model.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ---- Auth ----
    @POST("api/auth/consumer/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("api/auth/consumer/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): AuthResponse

    @POST("api/auth/consumer/resend-verification")
    suspend fun resendVerification(@Body body: ResendVerificationRequest): RegisterResponse

    @POST("api/auth/consumer/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    // Passwordless email login (backend pending; contract defined app-side).
    @POST("api/auth/consumer/email/send-otp")
    suspend fun sendEmailOtp(@Body body: SendEmailOtpRequest): SendEmailOtpResponse

    @POST("api/auth/consumer/email/verify-otp")
    suspend fun verifyEmailOtp(@Body body: VerifyOtpRequest): AuthResponse

    @POST("api/auth/consumer/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequest): AuthResponse

    @GET("api/auth/consumer/validate-dealer/{code}")
    suspend fun validateDealer(@Path("code") code: String): ValidateDealerResponse

    // ---- Profile ----
    @GET("api/consumer/me")
    suspend fun me(): ConsumerResponse

    @PATCH("api/consumer/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ConsumerResponse

    @POST("api/consumer/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): SuccessResponse

    // ---- Products (public) ----
    @GET("api/consumer/products")
    suspend fun products(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
    ): ProductsResponse

    @GET("api/consumer/products/{id}")
    suspend fun product(@Path("id") id: Long): ProductResponse

    @GET("api/consumer/products/{id}/reviews")
    suspend fun reviews(@Path("id") id: Long): ReviewsResponse

    @POST("api/consumer/products/{id}/reviews")
    suspend fun postReview(@Path("id") id: Long, @Body body: PostReviewRequest): SuccessResponse

    @GET("api/consumer/refill-caps")
    suspend fun refillCaps(): RefillCapsResponse

    @GET("api/consumer/settings")
    suspend fun settings(): SettingsResponse

    // ---- Containers ----
    @GET("api/consumer/containers")
    suspend fun containers(): ContainersResponse

    @POST("api/consumer/containers/{id}/request-refund")
    suspend fun requestRefund(@Path("id") id: Long, @Body body: RefundRequest): SuccessResponse

    @POST("api/consumer/containers/{id}/cancel-refund")
    suspend fun cancelRefund(@Path("id") id: Long): SuccessResponse

    @POST("api/consumer/containers/{id}/swap")
    suspend fun swapContainer(@Path("id") id: Long, @Body body: SwapRequest): SuccessResponse

    @POST("api/consumer/containers/{id}/dispute")
    suspend fun disputeContainer(@Path("id") id: Long, @Body body: DisputeRequest): SuccessResponse

    // ---- Wallet ----
    @GET("api/consumer/store-credit")
    suspend fun storeCredit(): StoreCreditResponse

    // ---- Addresses ----
    @GET("api/consumer/addresses")
    suspend fun addresses(): AddressesResponse

    @POST("api/consumer/addresses")
    suspend fun addAddress(@Body body: SaveAddressRequest): AddressResponse

    @PUT("api/consumer/addresses/{id}")
    suspend fun updateAddress(@Path("id") id: Long, @Body body: SaveAddressRequest): AddressResponse

    @DELETE("api/consumer/addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: Long): SuccessResponse

    @PUT("api/consumer/addresses/{id}/default")
    suspend fun makeDefaultAddress(@Path("id") id: Long): SuccessResponse

    // ---- Orders ----
    @POST("api/consumer/orders")
    suspend fun placeOrder(@Body body: PlaceOrderRequest): PlaceOrderResponse

    @GET("api/consumer/orders")
    suspend fun orders(): OrdersResponse

    @GET("api/consumer/orders/{id}")
    suspend fun order(@Path("id") id: Long): OrderDetailResponse

    @DELETE("api/consumer/orders/{id}")
    suspend fun deleteUnpaidOrder(@Path("id") id: Long): SuccessResponse

    // ---- Payments ----
    @POST("api/payments/create-order")
    suspend fun createPayment(@Body body: CreatePaymentRequest): CreatePaymentResponse

    @POST("api/payments/verify")
    suspend fun verifyPayment(@Body body: VerifyPaymentRequest): SuccessResponse
}
