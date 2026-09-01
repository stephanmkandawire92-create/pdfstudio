package com.example.util

import com.example.BuildConfig

object AdConfig {
    // Real App ID (Update in AndroidManifest.xml when launching)
    const val REAL_APP_ID = "ca-app-pub-4641751826914800~5300658596"
    
    // Real Ad Units (Assigned to formats)
    const val REAL_BANNER_ID = "ca-app-pub-4641751826914800/8108101983"
    const val REAL_INTERSTITIAL_ID = "ca-app-pub-4641751826914800/4047286674"
    const val REAL_REWARDED_ID = "ca-app-pub-4641751826914800/2622524362"

    // Test Ad Units (Provided by Google)
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Getters that automatically use TEST ads in debug builds to protect your account
    val bannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_ID else REAL_BANNER_ID

    val interstitialAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else REAL_INTERSTITIAL_ID
        
    val rewardedAdUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_ID else REAL_REWARDED_ID
}
