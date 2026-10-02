package com.example.sanathanatattva.ui.navigation

object Routes {
    // Bottom-nav destinations
    const val SHOP = "shop"
    const val ORDERS = "orders"
    const val CONTAINERS = "containers"
    const val ACCOUNT = "account"

    // Stacked destinations
    const val PRODUCT = "product/{productId}"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val ORDER_DETAIL = "order/{orderId}"
    const val ADDRESSES = "addresses"
    const val WALLET = "wallet"
    const val LUCKY_DRAW = "lucky_draw"

    // Auth
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VERIFY_PENDING = "verify_pending"

    fun product(id: Long) = "product/$id"
    fun orderDetail(id: Long) = "order/$id"
}
