package gruviks.space

import java.lang.Integer.max

class RectTree<T>(
    val maxNodeSize: Int = 47
) {

    private val leaves = mutableListOf<Pair<RectRegion, T>>()
    private val childNodes = mutableListOf<RectTree<T>>()
    private var splitX = Coordinate.ZERO
    private var splitY = Coordinate.ZERO

    var size = 0
        private set

    var depth = 1
        private set

    init {
        if (maxNodeSize <= 2) throw IllegalArgumentException("maxNodeSize ($maxNodeSize) must be larger than 2")
        if (maxNodeSize % 2 == 0) throw IllegalArgumentException("maxNodeSize ($maxNodeSize) must be odd")
    }

    private fun determineChildNodeIndex(region: RectRegion): Int {
        val indexX = if (region.boundX <= splitX) 0 else if (region.minX > splitX) 2 else 1
        val indexY = if (region.boundY <= splitY) 0 else if (region.minY > splitY) 2 else 1
        return indexX + 3 * indexY
    }

    private fun determineChildNode(region: RectRegion) = childNodes[determineChildNodeIndex(region)]

    private fun updateSizeAndDepth() {
        if (childNodes.isEmpty()) {
            size = leaves.size
            depth = 1
        } else {
            var newSize = 0
            var maxDepth = 0
            for (node in childNodes) {
                newSize += node.size
                maxDepth = max(maxDepth, node.depth)
            }

            this.size = newSize
            this.depth = 1 + maxDepth
        }
    }

    private fun stayBalanced() {
        if (childNodes.isNotEmpty()) {
            val depthLowX = childNodes[0].depth + childNodes[3].depth + childNodes[6].depth
            val depthMidX = childNodes[1].depth + childNodes[4].depth + childNodes[7].depth
            val depthHighX = childNodes[2].depth + childNodes[5].depth + childNodes[8].depth

            val depthLowY = childNodes[0].depth + childNodes[1].depth + childNodes[2].depth
            val depthMidY = childNodes[3].depth + childNodes[4].depth + childNodes[5].depth
            val depthHighY = childNodes[6].depth + childNodes[7].depth + childNodes[8].depth

            var shouldBalance = false

            if (depthLowX > 5 && depthLowX > 2 * (depthMidX + depthHighX)) shouldBalance = true
            if (depthHighX > 5 && depthHighX > 2 * (depthMidX + depthLowX)) shouldBalance = true
            if (depthLowY > 5 && depthLowY > 2 * (depthMidY + depthHighY)) shouldBalance = true
            if (depthHighY > 5 && depthHighY > 2 * (depthMidY + depthLowY)) shouldBalance = true

            if (shouldBalance) restoreBalance()
        }
    }

    private fun restoreBalance() {
        assert(leaves.isEmpty())
        assert(childNodes.size == 9)

        val allEntries = ArrayList<Pair<RectRegion, T>>(size)
        collectAll(allEntries)
        restoreBalance(allEntries)
    }

    private fun restoreBalance(allEntries: MutableList<Pair<RectRegion, T>>) {
        leaves.clear()
        childNodes.clear()

        val nodesToProcess = ArrayList<Pair<RectTree<T>, MutableList<Pair<RectRegion, T>>>>(100)
        nodesToProcess.add(Pair(this, allEntries))

        val processedNodes = ArrayList<RectTree<T>>(100)

        while (nodesToProcess.isNotEmpty()) {
            val (currentNode, currentEntries) = nodesToProcess.removeLast()
            val size = currentEntries.size

            if (size <= currentNode.maxNodeSize) {
                currentNode.leaves.addAll(currentEntries)
                currentNode.updateSizeAndDepth()
            }
            else {
                currentEntries.sortBy { (it.first.minX + it.first.boundX) / 2 }
                currentNode.splitX = currentEntries[size / 2].first.let { (it.minX + it.boundX) / 2 }
                currentEntries.sortBy { (it.first.minY + it.first.boundY) / 2 }
                currentNode.splitY = currentEntries[size / 2].first.let { (it.minY + it.boundY) / 2 }

                val grandchildren = Array(9) { ArrayList<Pair<RectRegion, T>>(size / 9) }
                for (entry in currentEntries) {
                    grandchildren[currentNode.determineChildNodeIndex(entry.first)].add(entry)
                }
                for (index in 0 until 9) {
                    val childNode = RectTree<T>(currentNode.maxNodeSize)
                    currentNode.childNodes.add(childNode)
                    nodesToProcess.add(Pair(childNode, grandchildren[index]))
                }

                processedNodes.add(currentNode)
            }
        }

        processedNodes.reverse()
        for (node in processedNodes) node.updateSizeAndDepth()
    }

    private fun collectAll(entries: MutableCollection<Pair<RectRegion, T>>) {
        entries.addAll(leaves)
        for (child in childNodes) {
            child.collectAll(entries)
        }
    }

    @Throws(IllegalArgumentException::class)
    fun insert(element: T, region: RectRegion) {

        val affectedNodes = ArrayList<RectTree<T>>(this.depth)

        var currentNode = this
        while (true) {
            affectedNodes.add(currentNode)

            if (currentNode.leaves.any { it.first.overlaps(region) }) {
                throw IllegalArgumentException("New region $region would overlap existing region")
            }

            if (currentNode.childNodes.isEmpty() && currentNode.leaves.size < currentNode.maxNodeSize) {
                currentNode.leaves.add(Pair(region, element))
                break
            } else if (currentNode.leaves.size == currentNode.maxNodeSize) {

                currentNode.splitX = currentNode.leaves.map { (it.first.minX + it.first.boundX) / 2 }.sorted()[currentNode.maxNodeSize / 2]
                currentNode.splitY = currentNode.leaves.map { (it.first.minY + it.first.boundY) / 2 }.sorted()[currentNode.maxNodeSize / 2]

                assert(currentNode.childNodes.isEmpty())
                currentNode.leaves.add(Pair(region, element))
                for (index in 0 until 9) currentNode.childNodes.add(RectTree(currentNode.maxNodeSize))
                for (leaf in currentNode.leaves) {
                    currentNode.determineChildNode(leaf.first).insert(leaf.second, leaf.first)
                }
                currentNode.leaves.clear()
                break
            } else {
                assert(currentNode.leaves.isEmpty())
                assert(currentNode.childNodes.size == 9)

                currentNode = currentNode.determineChildNode(region)
            }
        }

        affectedNodes.reverse()
        for (node in affectedNodes) {
            node.updateSizeAndDepth()
        }

        affectedNodes.reverse()
        for (node in affectedNodes) {
            val oldDepth = node.depth
            node.stayBalanced()
            if (oldDepth != node.depth) {
                break
            }
        }
    }

    // TODO Add delete method

    fun findBetween(region: RectRegion): List<Pair<RectRegion, T>> {
        val result = mutableListOf<Pair<RectRegion, T>>()

        val relevantNodes = ArrayList<RectTree<T>>(1000)
        relevantNodes.add(this)

        while (relevantNodes.isNotEmpty()) {
            val nextNode = relevantNodes.removeLast()
            if (nextNode.childNodes.isEmpty()) {
                for (leaf in nextNode.leaves) {
                    if (leaf.first.overlaps(region)) {
                        result.add(leaf)
                    }
                }
            } else {
                if (region.minX < nextNode.splitX) {
                    if (region.minY < nextNode.splitY) relevantNodes.add(nextNode.childNodes[0])
                    relevantNodes.add(nextNode.childNodes[3])
                    if (region.boundY > nextNode.splitY) relevantNodes.add(nextNode.childNodes[6])
                }

                if (region.minY < nextNode.splitY) relevantNodes.add(nextNode.childNodes[1])
                relevantNodes.add(nextNode.childNodes[4])
                if (region.boundY > nextNode.splitY) relevantNodes.add(nextNode.childNodes[7])

                if (region.boundX > nextNode.splitX) {
                    if (region.minY < nextNode.splitY) relevantNodes.add(nextNode.childNodes[2])
                    relevantNodes.add(nextNode.childNodes[5])
                    if (region.boundY > nextNode.splitY) relevantNodes.add(nextNode.childNodes[8])
                }
            }
        }

        return result
    }
}
