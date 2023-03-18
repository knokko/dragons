package gruviks.util

import gruviks.component.RectangularDrawnRegion

fun optimizeRecentDrawnRegions(originalDrawnRegions: Collection<RectangularDrawnRegion>): List<RectangularDrawnRegion> {
    val margin = 0.001f

    // Divide the viewport in *numSlots* slots can keep track of which regions (partially) overlap which slots
    val numSlotsPerAxis = 4
    val numSlots = numSlotsPerAxis * numSlotsPerAxis
    val slots = Array(numSlots) { mutableListOf<RectangularDrawnRegion>() }

    fun insertDrawnRegion(region: RectangularDrawnRegion) {
        val slotMinX = (region.minX * numSlotsPerAxis - margin).toInt()
        val slotMinY = (region.minY * numSlotsPerAxis - margin).toInt()
        val slotMaxX = (region.maxX * numSlotsPerAxis + margin).toInt()
        val slotMaxY = (region.maxY * numSlotsPerAxis + margin).toInt()
        for (slotX in slotMinX .. slotMaxX) {
            for (slotY in slotMinY .. slotMaxY) {
                if (slotX >= 0 && slotY >= 0 && slotX < numSlotsPerAxis && slotY < numSlotsPerAxis) {
                    slots[slotX + numSlotsPerAxis * slotY].add(region)
                }
            }
        }
    }

    for (region in originalDrawnRegions) {
        insertDrawnRegion(region)
    }

    // We allow every slot to contain at most 1 region: when multiple are found, they are merged into 1
    // Repeat this process until every slot contains at most 1 region
    var didMerge = true
    while (didMerge) {
        didMerge = false
        for (slotRegions in slots) {
            if (slotRegions.size > 1) {
                val minX = slotRegions.minOf { it.minX }
                val minY = slotRegions.minOf { it.minY }
                val maxX = slotRegions.maxOf { it.maxX }
                val maxY = slotRegions.maxOf { it.maxY }

                val oldRegions = ArrayList(slotRegions)
                for (oldSlotRegions in slots) {
                    oldSlotRegions.removeAll(oldRegions)
                }

                insertDrawnRegion(RectangularDrawnRegion(minX, minY, maxX, maxY))
                didMerge = true
            }
        }
    }

    val result = mutableSetOf<RectangularDrawnRegion>()
    for (slotRegions in slots) {
        result.addAll(slotRegions)
    }
    return result.toList()
}
