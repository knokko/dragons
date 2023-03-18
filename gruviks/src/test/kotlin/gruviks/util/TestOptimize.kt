package gruviks.util

import gruviks.component.RectangularDrawnRegion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TestOptimize {

    @Test
    fun testOptimizeRecentDrawnRegionsSingular() {
        for (region in arrayOf(
                RectangularDrawnRegion(0f, 0f, 1f, 1f),
                RectangularDrawnRegion(0f, 0f, 0.1f, 0.2f),
                RectangularDrawnRegion(0f, 0.4f, 0.3f, 0.9f),
                RectangularDrawnRegion(0f, 0.7f, 0.2f, 1f),
                RectangularDrawnRegion(0.4f, 0f, 0.5f, 0.1f),
                RectangularDrawnRegion(0.2f, 0.3f, 0.4f, 0.5f),
                RectangularDrawnRegion(0.6f, 0.8f, 0.7f, 1f),
                RectangularDrawnRegion(0.6f, 0.8f, 1f, 1f)
        )) {
            assertEquals(listOf(region), optimizeRecentDrawnRegions(listOf(region)))
        }
    }

    @Test
    fun testOptimizeRecentDrawnRegionsDuoSpread() {
        for ((region1, region2) in arrayOf(
                Pair(
                        RectangularDrawnRegion(0.1f, 0.2f, 0.3f, 0.4f),
                        RectangularDrawnRegion(0.8f, 0.7f, 0.9f, 0.9f)
                ), Pair(
                        RectangularDrawnRegion(0f, 0f, 0.3f, 0.2f),
                        RectangularDrawnRegion(0.7f, 0.9f, 1f, 1f)
                )
        )) {
            assertEquals(setOf(region1, region2), optimizeRecentDrawnRegions(listOf(region1, region2)).toSet())
        }
    }

    @Test
    fun testOptimizeRecentDrawnRegionsDuoMerge() {
        for ((region1, region2, mergedRegion) in arrayOf(
                Triple(
                        RectangularDrawnRegion(0f, 0f, 0.3f, 0.2f),
                        RectangularDrawnRegion(0f, 0.2f, 0.4f, 0.6f),
                        RectangularDrawnRegion(0f, 0f, 0.4f, 0.6f)
                ), Triple(
                        RectangularDrawnRegion(0.3f, 0.6f, 0.4f, 0.8f),
                        RectangularDrawnRegion(0.4f, 0.55f, 1f, 0.7f),
                        RectangularDrawnRegion(0.3f, 0.55f, 1f, 0.8f)
                )
        )) {
            assertEquals(listOf(mergedRegion), optimizeRecentDrawnRegions(listOf(region1, region2)))
        }
    }

    @Test
    fun testOptimizeRecentDrawnRegionsComplex() {
        val originalRegions = listOf(
                // Lonely component on the bottom-left
                RectangularDrawnRegion(0f, 0.1f, 0.2f, 0.15f),

                // Duo component on the top-left
                RectangularDrawnRegion(0f, 0.6f, 0.3f, 0.7f),
                RectangularDrawnRegion(0.1f, 0.6f, 0.2f, 1f),

                // Triple component on the top-right
                RectangularDrawnRegion(0.6f, 0.7f, 0.7f, 0.8f),
                RectangularDrawnRegion(0.65f, 0.7f, 0.8f, 0.85f),
                RectangularDrawnRegion(0.65f, 0.85f, 0.9f, 1f),

                // 4 components on the bottom-right
                RectangularDrawnRegion(0.7f, 0.1f, 0.8f, 0.2f),
                RectangularDrawnRegion(0.7f, 0.22f, 0.8f, 0.3f),
                RectangularDrawnRegion(0.8f, 0.05f, 0.9f, 0.2f),
                RectangularDrawnRegion(0.8f, 0.2f, 1f, 0.4f)
        ).shuffled(Random(1245))

        val expectedRegions = setOf(
                // The lonely bottom-left component
                RectangularDrawnRegion(0f, 0.1f, 0.2f, 0.15f),

                // The merged components on the top-left
                RectangularDrawnRegion(0f, 0.6f, 0.3f, 1f),

                // The merged components on the top-right
                RectangularDrawnRegion(0.6f, 0.7f, 0.9f, 1f),

                // The merged components on the bottom-right
                RectangularDrawnRegion(0.7f, 0.05f, 1f, 0.4f)
        )

        assertEquals(expectedRegions, optimizeRecentDrawnRegions(originalRegions).toSet())
    }
}
