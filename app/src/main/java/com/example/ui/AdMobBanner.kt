package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

enum class AdLoadState { Loading, Loaded, Error }

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    var adState by remember { mutableStateOf(AdLoadState.Loading) }

    // If the ad failed to load, collapse the UI entirely so it doesn't take up empty space
    if (adState == AdLoadState.Error) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp), // Generous padding to prevent obstruction of primary PDF tools
        contentAlignment = Alignment.Center
    ) {
        // Show a loading skeleton/indicator while fetching the ad
        if (adState == AdLoadState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp) // Standard AdMob Banner height
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }

        // The actual AdView
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                // Hide visually until loaded to prevent layout popping/flashing
                .alpha(if (adState == AdLoadState.Loaded) 1f else 0f),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    // Test banner ad unit ID
                    adUnitId = com.example.util.AdConfig.bannerAdUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            adState = AdLoadState.Loaded
                        }

                        override fun onAdFailedToLoad(p0: LoadAdError) {
                            adState = AdLoadState.Error
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
