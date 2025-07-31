package com.kalpi.prochat


import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager

class ProChatApplication : Application()  {
    override fun onCreate() {
        super.onCreate()

        // Initialize Cloudinary MediaManager
        val config = HashMap<String, String>()
        config["cloud_name"] = "dwdzgxpvh" // <<< IMPORTANT: REPLACE WITH YOUR ACTUAL CLOUD NAME


        MediaManager.init(this, config)
    }
}