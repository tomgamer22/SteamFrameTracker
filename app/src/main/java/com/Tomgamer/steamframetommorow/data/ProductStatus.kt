package com.Tomgamer.steamframetommorow.data

sealed class ProductStatus {
    object Unknown : ProductStatus()
    object NotAvailable : ProductStatus()
    object PreorderAvailable : ProductStatus()
    object Available : ProductStatus()
    object SoldOut : ProductStatus()

    fun toDisplayString(): String {
        return when (this) {
            is Unknown -> "Unknown"
            is NotAvailable -> "Not Available"
            is PreorderAvailable -> "Pre-order Available! 🎉"
            is Available -> "Available Now! 🎉"
            is SoldOut -> "Sold Out"
        }
    }

    fun toStorageString(): String {
        return when (this) {
            is Unknown -> "UNKNOWN"
            is NotAvailable -> "NOT_AVAILABLE"
            is PreorderAvailable -> "PREORDER_AVAILABLE"
            is Available -> "AVAILABLE"
            is SoldOut -> "SOLD_OUT"
        }
    }

    companion object {
        fun fromStorageString(value: String): ProductStatus {
            return when (value) {
                "UNKNOWN" -> Unknown
                "NOT_AVAILABLE" -> NotAvailable
                "PREORDER_AVAILABLE" -> PreorderAvailable
                "AVAILABLE" -> Available
                "SOLD_OUT" -> SoldOut
                else -> Unknown
            }
        }
    }
}

