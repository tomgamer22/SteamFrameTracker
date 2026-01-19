package com.Tomgamer.steamframetommorow.data

import android.util.Log
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface SteamApiService {
    @GET("api/appdetails")
    suspend fun getAppDetails(@Query("appids") appId: String): Response<Map<String, Any>>
}

object SteamApi {
    private const val BASE_URL = "https://store.steampowered.com/"
    private const val TAG = "SteamApi"

    val service: SteamApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SteamApiService::class.java)
    }

    const val STEAM_FRAME_APP_ID = "4165890"

    suspend fun checkSteamFrameAvailability(testMode: Boolean = false): ProductStatus {
        if (testMode) {
            Log.d(TAG, "TEST MODE: Returning NotAvailable")
            return ProductStatus.NotAvailable
        }

        return try {
            val response = service.getAppDetails(STEAM_FRAME_APP_ID)

            if (response.isSuccessful) {
                val data = response.body()
                Log.d(TAG, "Steam API Response: $data")

                data?.let { responseData ->
                    val appData = responseData[STEAM_FRAME_APP_ID] as? Map<*, *>
                    val success = appData?.get("success") as? Boolean

                    if (success == true) {
                        val dataMap = appData["data"] as? Map<*, *>

                        val releaseDate = dataMap?.get("release_date") as? Map<*, *>
                        val comingSoon = releaseDate?.get("coming_soon") as? Boolean ?: true

                        val isPurchasable = dataMap?.get("is_free") == false
                        val priceOverview = dataMap?.get("price_overview") as? Map<*, *>
                        val hasPrice = priceOverview != null

                        val packageGroups = dataMap?.get("package_groups") as? List<*>
                        val hasPurchaseOptions = !packageGroups.isNullOrEmpty()

                        Log.d(TAG, "Coming Soon: $comingSoon, Purchasable: $isPurchasable, HasPrice: $hasPrice, HasPackages: $hasPurchaseOptions")

                        return when {
                            !comingSoon && hasPrice && hasPurchaseOptions -> ProductStatus.Available
                            comingSoon && hasPurchaseOptions -> ProductStatus.PreorderAvailable
                            !comingSoon && !hasPurchaseOptions -> ProductStatus.NotAvailable
                            comingSoon && !hasPurchaseOptions -> ProductStatus.NotAvailable
                            else -> ProductStatus.NotAvailable
                        }
                    } else {
                        Log.w(TAG, "Steam API returned success=false")
                        return ProductStatus.NotAvailable
                    }
                }
            } else {
                Log.e(TAG, "Steam API request failed: ${response.code()}")
            }

            ProductStatus.NotAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Steam availability", e)
            e.printStackTrace()
            ProductStatus.NotAvailable
        }
    }
}

