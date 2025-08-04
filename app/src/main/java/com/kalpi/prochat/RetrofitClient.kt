package com.kalpi.prochat

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // IMPORTANT: You MUST update this BASE_URL to point to your Node.js server.
    // For an Android emulator, '10.0.2.2' maps to your computer's localhost.
    // For a physical device, you need to use your computer's local IP address (e.g., '192.168.1.5').
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}