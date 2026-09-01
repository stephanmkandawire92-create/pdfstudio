package com.example.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    fun loadInterstitial(context: Context) {
        if (mInterstitialAd != null) return // Already loaded
        
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext, 
            AdConfig.interstitialAdUnitId, 
            adRequest, 
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            }
        )
    }

    fun loadRewarded(context: Context) {
        if (mRewardedAd != null) return // Already loaded
        
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            AdConfig.rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mRewardedAd = null
                }
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        val interstitialAd = mInterstitialAd
        if (interstitialAd != null) {
            interstitialAd.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitial(activity) // Pre-load next ad
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    mInterstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd.show(activity)
        } else {
            // Ad wasn't loaded, continue anyway and try loading for next time
            onAdDismissed()
            loadInterstitial(activity)
        }
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdDismissed: () -> Unit) {
        val rewardedAd = mRewardedAd
        if (rewardedAd != null) {
            rewardedAd.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mRewardedAd = null
                    loadRewarded(activity) // Pre-load next ad
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    mRewardedAd = null
                    onAdDismissed()
                }
            }
            rewardedAd.show(activity) { rewardItem ->
                // User earned reward
                onRewardEarned()
            }
        } else {
            // Ad wasn't loaded, continue anyway (or you might want to show a message)
            // For now, grant the reward if ad fails to load so user isn't blocked, or just dismiss
            onRewardEarned()
            onAdDismissed()
            loadRewarded(activity)
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
