package dsl.pm2.renderer

import dsl.pm2.interpreter.Pm2RuntimeError
import org.joml.Matrix3x2f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import troll.instance.TrollInstance
import java.util.concurrent.ConcurrentHashMap

class Pm2Instance(
    val troll: TrollInstance
) {

    private val pipelines = ConcurrentHashMap<Pm2PipelineInfo, Long>()
    val allocations = Pm2Allocations(troll)

    private val pipelineLayout: Long
    val descriptorSetLayout: Long

    init {
        stackPush().use { stack ->
            val (pipelineLayout, descriptorSetLayout) = createPipelineLayout(troll, stack)
            this.pipelineLayout = pipelineLayout
            this.descriptorSetLayout = descriptorSetLayout
        }
    }

    @Throws(Pm2RuntimeError::class)
    fun recordDraw(
            commandBuffer: VkCommandBuffer, pipelineInfo: Pm2PipelineInfo, descriptorSet: Long,
            meshesWithMatrices: List<Pair<Pm2Mesh, Int>>, cameraMatrix: Matrix3x2f
    ) {
        val pipeline = pipelines.computeIfAbsent(pipelineInfo) { info -> createGraphicsPipeline(troll, info, pipelineLayout) }

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
            for (mesh in meshesWithMatrices) vertexBuffers.add(mesh.first.vertexBuffer.vkBuffer)

            for (vertexBuffer in vertexBuffers) {
                val currentMeshes = meshesWithMatrices.filter { it.first.vertexBuffer.vkBuffer == vertexBuffer }
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
        pipelines.forEachValue(50) { pipeline -> vkDestroyPipeline(troll.vkDevice(), pipeline, null) }
        pipelines.clear()
        vkDestroyPipelineLayout(troll.vkDevice(), pipelineLayout, null)
        vkDestroyDescriptorSetLayout(troll.vkDevice(), descriptorSetLayout, null)
        allocations.destroy()
    }
}
