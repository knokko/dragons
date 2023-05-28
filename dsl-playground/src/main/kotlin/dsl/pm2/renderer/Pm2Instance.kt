package dsl.pm2.renderer

import dsl.pm2.renderer.pipeline.Pm2Pipeline
import dsl.pm2.renderer.pipeline.Pm2PipelineInfo
import dsl.pm2.renderer.pipeline.createGraphicsPipeline
import org.joml.Matrix3x2f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkDevice
import java.util.concurrent.ConcurrentHashMap

class Pm2Instance(
    private val device: VkDevice,
    vmaAllocator: Long,
    queueFamilyIndex: Int
) {

    private val pipelines = ConcurrentHashMap<Pm2PipelineInfo, Pm2Pipeline>()
    val allocations = Pm2Allocations(device, vmaAllocator, queueFamilyIndex)

    fun recordDraw(commandBuffer: VkCommandBuffer, pipelineInfo: Pm2PipelineInfo, meshes: List<Pm2Mesh>, cameraMatrix: Matrix3x2f) {
        val pipeline = pipelines.computeIfAbsent(pipelineInfo) { info -> createGraphicsPipeline(device, info) }

        stackPush().use { stack ->
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.vkPipeline)

            val matrixBuffer = stack.callocFloat(3 * 2)
            cameraMatrix.get(0, matrixBuffer)
            vkCmdPushConstants(commandBuffer, pipeline.vkPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, matrixBuffer)

            // TODO Bind descriptor sets
            val vertexBuffers = mutableSetOf<Long>()
            for (mesh in meshes) vertexBuffers.add(mesh.vkBuffer)

            for (vertexBuffer in vertexBuffers) {
                val currentMeshes = meshes.filter { it.vkBuffer == vertexBuffer }
                vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(vertexBuffer), stack.longs(0))
                // TODO Experiment with optimizations
                for (mesh in currentMeshes) {
                    vkCmdDraw(commandBuffer, mesh.numVertices, 1, mesh.vertexOffset, 0)
                }
            }
        }
    }

    fun destroy() {
        pipelines.forEachValue(50) { pipeline -> pipeline.destroy(device)}
        pipelines.clear()
        allocations.destroy()
    }
}
