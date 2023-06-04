package dsl.pm2.renderer

import dsl.pm2.interpreter.Pm2RuntimeError
import dsl.pm2.renderer.pipeline.Pm2PipelineInfo
import dsl.pm2.renderer.pipeline.createGraphicsPipeline
import dsl.pm2.renderer.pipeline.createPipelineLayout
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

    private val pipelines = ConcurrentHashMap<Pm2PipelineInfo, Long>()
    val allocations = Pm2Allocations(device, vmaAllocator, queueFamilyIndex)

    private val pipelineLayout: Long
    val descriptorSetLayout: Long

    init {
        stackPush().use { stack ->
            val (pipelineLayout, descriptorSetLayout) = createPipelineLayout(device, stack)
            this.pipelineLayout = pipelineLayout
            this.descriptorSetLayout = descriptorSetLayout
        }
    }

    @Throws(Pm2RuntimeError::class)
    fun recordDraw(
            commandBuffer: VkCommandBuffer, pipelineInfo: Pm2PipelineInfo, descriptorSet: Long,
            meshesWithMatrices: List<Pair<Pm2Mesh, Int>>, cameraMatrix: Matrix3x2f
    ) {
        val pipeline = pipelines.computeIfAbsent(pipelineInfo) { info -> createGraphicsPipeline(device, info, pipelineLayout) }

        stackPush().use { stack ->
            val pushBuffer = stack.callocInt(1)
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)
            vkCmdBindDescriptorSets(
                    commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout,
                    0, stack.longs(descriptorSet), null
            )

            val matrixBuffer = stack.callocFloat(3 * 2)
            cameraMatrix.get(0, matrixBuffer)
            vkCmdPushConstants(commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, matrixBuffer)

            val vertexBuffers = mutableSetOf<Long>()
            for (mesh in meshesWithMatrices) vertexBuffers.add(mesh.first.vertexBuffer)

            for (vertexBuffer in vertexBuffers) {
                val currentMeshes = meshesWithMatrices.filter { it.first.vertexBuffer == vertexBuffer }
                vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(vertexBuffer), stack.longs(0))
                // TODO Experiment with optimizations
                for ((mesh, matrixIndexOffset) in currentMeshes) {

                    pushBuffer.put(0, matrixIndexOffset)
                    vkCmdPushConstants(commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 2 * 3 * 4, pushBuffer)
                    vkCmdDraw(commandBuffer, mesh.numVertices, 1, mesh.vertexOffset, 0)
                }
            }
        }
    }

    fun destroy() {
        pipelines.forEachValue(50) { pipeline -> vkDestroyPipeline(device, pipeline, null) }
        pipelines.clear()
        vkDestroyPipelineLayout(device, pipelineLayout, null)
        vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null)
        allocations.destroy()
    }
}
