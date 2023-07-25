package dragons.plugins.standard.vulkan.render.entity

import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.util.nextPowerOf2
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import troll.exceptions.VulkanFailureException.assertVmaSuccess
import java.util.*

internal class EntityMeshTracker(
    /**
     * The size of the entity mesh buffer, in bytes
     */
    internal val size: Int
) {

    private val meshMap = mutableMapOf<UUID, TrackedMesh>()
    private val vmaBlock: Long

    init {
        stackPush().use { stack ->
            val ciBlock = VmaVirtualBlockCreateInfo.calloc(stack)
            ciBlock.size(this.size.toLong())

            val pBlock = stack.callocPointer(1)

            assertVmaSuccess(
                vmaCreateVirtualBlock(ciBlock, pBlock),
                "CreateVirtualBlock", "EntityMeshTracker"
            )
            this.vmaBlock = pBlock[0]
        }
    }

    fun startFrame() {
        for (trackedMesh in meshMap.values) {
            trackedMesh.age += 1
        }
    }

    fun useMesh(mesh: EntityMesh) {
        if (meshMap.containsKey(mesh.id)) {
            meshMap[mesh.id]!!.age = 0
        } else {
            val (vertexOffset, allocation) = stackPush().use { stack ->
                val ciAllocation = VmaVirtualAllocationCreateInfo.calloc(stack)
                ciAllocation.size(mesh.generator.numVertices * BasicVertex.SIZE + mesh.generator.numIndices * 4L)
                ciAllocation.alignment(ALIGNMENT)

                val pAllocation = stack.callocPointer(1)
                val pOffset = stack.callocLong(1)

                if (vmaVirtualAllocate(this.vmaBlock, ciAllocation, pAllocation, pOffset) == VK_SUCCESS) {
                    return@use Pair(pOffset[0].toInt(), pAllocation[0])
                }

                // If the allocation failed, we will keep removing old meshes until the allocation succeeds
                for (thresholdAge in arrayOf(1000L, 100L, 10L, 1L)) {
                    this.removeOldAllocations(thresholdAge)
                    if (vmaVirtualAllocate(this.vmaBlock, ciAllocation, pAllocation, pOffset) == VK_SUCCESS) {
                        return@use Pair(pOffset[0].toInt(), pAllocation[0])
                    }
                }

                // TODO Perhaps try clearing the entire mesh buffer and re-allocating all entries with age 0
                // I will skip it for now because it is rather complicated and unlikely to truly help in practice

                throw IllegalArgumentException("Entity mesh buffer is too small")
            }

            val indexOffset = vertexOffset + mesh.generator.numVertices * BasicVertex.SIZE
            meshMap[mesh.id] = TrackedMesh(
                allocation = allocation, vertexOffset = vertexOffset, indexOffset = indexOffset
            )
        }
    }

    fun getLocation(mesh: EntityMesh): MeshLocation {
        val trackedMesh = this.meshMap[mesh.id] ?: throw IllegalArgumentException("This mesh has not been used")
        if (trackedMesh.age != 0L) throw IllegalArgumentException("This mesh has not been used this frame")
        return MeshLocation(
            vertexOffset = trackedMesh.vertexOffset, indexOffset = trackedMesh.indexOffset,
            hasBeenFilled = trackedMesh.hasBeenFilled
        )
    }

    fun markFilled(mesh: EntityMesh) {
        val trackedMesh = this.meshMap[mesh.id] ?: throw IllegalArgumentException("This mesh has not been used")
        if (trackedMesh.hasBeenFilled) throw IllegalStateException("This mesh has already been filled")
        trackedMesh.hasBeenFilled = true
    }

    private fun removeOldAllocations(thresholdAge: Long) {
        this.meshMap.values.removeIf { trackedMesh ->
            if (trackedMesh.age >= thresholdAge) {
                vmaVirtualFree(this.vmaBlock, trackedMesh.allocation)
                true
            } else {
                false
            }
        }
    }

    fun endFrame() {
        // Hm... I don't need this method now, but could be useful in the future...
    }

    fun destroy() {
        vmaClearVirtualBlock(this.vmaBlock)
        vmaDestroyVirtualBlock(this.vmaBlock)
    }

    companion object {
        // Note: the index buffer needs an alignment of 4,
        // but this is always satisfied since BasicVertex.SIZE is a multiple of 4.
        internal val ALIGNMENT = nextPowerOf2(BasicVertex.SIZE.toLong())
    }
}

private class TrackedMesh(
    val allocation: Long,
    val vertexOffset: Int,
    val indexOffset: Int,
) {
    /**
     * - Age = 0 -> mesh is used in this frame
     * - Age = 1 -> mesh was used during the last frame, but not (yet) in this frame
     * - Age = 2 -> mesh was used 2 frames ago, but not during the last frame, and not (yet) in this frame
     * - ...
     */
    var age = 0L
    var hasBeenFilled = false
}

internal class MeshLocation(
    val vertexOffset: Int,
    val indexOffset: Int,
    val hasBeenFilled: Boolean
)
