package com.example

import com.example.engine.PdfCompressionEngine
import com.example.engine.PdfCompressionEngine.CompressionConfig
import com.example.engine.PdfCompressionEngine.CompressionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfCompressionEngineTest {

    @Test
    fun testEstimateSavingsExtremePreset() {
        val originalSize = 10_000_000L // 10 MB
        val config = CompressionConfig(preset = CompressionPreset.EXTREME)
        val (estimatedSize, savingsPct) = PdfCompressionEngine.estimateSavings(originalSize, config)

        assertEquals(65, savingsPct)
        assertTrue("Estimated size should be smaller than original", estimatedSize < originalSize)
        assertEquals(3_500_000L, estimatedSize)
    }

    @Test
    fun testEstimateSavingsRecommendedPreset() {
        val originalSize = 10_000_000L // 10 MB
        val config = CompressionConfig(preset = CompressionPreset.RECOMMENDED)
        val (estimatedSize, savingsPct) = PdfCompressionEngine.estimateSavings(originalSize, config)

        assertEquals(45, savingsPct)
        assertEquals(5_500_000L, estimatedSize)
    }

    @Test
    fun testEstimateSavingsHighQualityPreset() {
        val originalSize = 10_000_000L // 10 MB
        val config = CompressionConfig(preset = CompressionPreset.HIGH_QUALITY)
        val (estimatedSize, savingsPct) = PdfCompressionEngine.estimateSavings(originalSize, config)

        assertEquals(25, savingsPct)
        assertEquals(7_500_000L, estimatedSize)
    }

    @Test
    fun testEstimateSavingsCustomWithGrayscale() {
        val originalSize = 5_000_000L
        val config = CompressionConfig(
            preset = CompressionPreset.CUSTOM,
            qualityPercent = 40,
            scaleFactor = 0.7f,
            convertToGrayscale = true,
            stripMetadata = true
        )
        val (estimatedSize, savingsPct) = PdfCompressionEngine.estimateSavings(originalSize, config)

        assertTrue("Savings percentage should be substantial", savingsPct >= 40)
        assertTrue("Estimated size should be significantly smaller", estimatedSize < originalSize)
    }
}
