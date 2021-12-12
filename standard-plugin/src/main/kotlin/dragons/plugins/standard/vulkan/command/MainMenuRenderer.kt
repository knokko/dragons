package dragons.plugins.standard.vulkan.command

import dragons.plugin.PluginInstance
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.MAX_NUM_INDIRECT_DRAW_CALLS
import dragons.plugins.standard.vulkan.model.generator.FlowerGenerators
import dragons.plugins.standard.vulkan.pipeline.updateBasicDynamicDescriptorSet
import dragons.plugins.standard.vulkan.vertex.BasicVertex
import dragons.state.StaticGameState
import dragons.vulkan.util.assertVkSuccess
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import java.util.*

fun createMainMenuRenderCommands(pluginInstance: PluginInstance, gameState: StaticGameState): Pair<Long, VkCommandBuffer> {
    return stackPush().use { stack ->
        val contextInfo = "standard plug-in: main menu rendering"

        val pluginState = pluginInstance.state as StandardPluginState
        val pluginGraphics = pluginState.graphics
        val graphicsState = gameState.graphics

        updateBasicDynamicDescriptorSet(
            graphicsState.vkDevice,
            pluginGraphics.basicDynamicDescriptorSet,
            pluginGraphics.mainMenu.textureSet.colorTextureList,
            pluginGraphics.mainMenu.textureSet.heightTextureList
        )

        val ciCommandPool = VkCommandPoolCreateInfo.calloc(stack)
        ciCommandPool.`sType$Default`()
        ciCommandPool.queueFamilyIndex(graphicsState.queueManager.generalQueueFamily.index)

        val pCommandPool = stack.callocLong(1)
        assertVkSuccess(
            vkCreateCommandPool(graphicsState.vkDevice, ciCommandPool, null, pCommandPool),
            "CreateCommandPool", contextInfo
        )
        val commandPool = pCommandPool[0]

        val aiCommandBuffer = VkCommandBufferAllocateInfo.calloc(stack)
        aiCommandBuffer.`sType$Default`()
        aiCommandBuffer.commandPool(commandPool)
        aiCommandBuffer.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        aiCommandBuffer.commandBufferCount(1)

        val pCommandBuffer = stack.callocPointer(1)
        assertVkSuccess(
            vkAllocateCommandBuffers(graphicsState.vkDevice, aiCommandBuffer, pCommandBuffer),
            "AllocateCommandBuffers", contextInfo
        )
        val commandBuffer = VkCommandBuffer(pCommandBuffer[0], graphicsState.vkDevice)

        val biCommandBuffer = VkCommandBufferBeginInfo.calloc(stack)
        biCommandBuffer.`sType$Default`()

        assertVkSuccess(
            vkBeginCommandBuffer(commandBuffer, biCommandBuffer),
            "BeginCommandBuffer", contextInfo
        )

        // Before rendering, we should copy the staging buffers of the camera + transformation matrices to the device
        val cameraCopyRegions = VkBufferCopy.calloc(1, stack)
        val cameraCopyRegion = cameraCopyRegions[0]
        cameraCopyRegion.srcOffset(pluginGraphics.buffers.cameraStaging.offset)
        cameraCopyRegion.dstOffset(pluginGraphics.buffers.cameraDevice.offset)
        cameraCopyRegions.size(pluginGraphics.buffers.cameraDevice.size)

        vkCmdCopyBuffer(
            commandBuffer, pluginGraphics.buffers.cameraStaging.buffer.handle,
            pluginGraphics.buffers.cameraDevice.buffer.handle, cameraCopyRegions
        )

        val transformationCopyRegions = VkBufferCopy.calloc(1, stack)
        val transformationCopyRegion = transformationCopyRegions[0]
        transformationCopyRegion.srcOffset(pluginGraphics.buffers.transformationMatrixStaging.offset)
        transformationCopyRegion.dstOffset(pluginGraphics.buffers.transformationMatrixDevice.offset)
        // TODO Consider copying no more matrices than are actually used
        transformationCopyRegion.size(pluginGraphics.buffers.transformationMatrixStaging.size)

        vkCmdCopyBuffer(
            commandBuffer, pluginGraphics.buffers.transformationMatrixStaging.buffer.handle,
            pluginGraphics.buffers.transformationMatrixDevice.buffer.handle, transformationCopyRegions
        )

        val copyBufferBarriers = VkBufferMemoryBarrier.calloc(2, stack)

        val copyCameraBarrier = copyBufferBarriers[0]
        copyCameraBarrier.`sType$Default`()
        copyCameraBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
        copyCameraBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
        copyCameraBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        copyCameraBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        copyCameraBarrier.buffer(pluginGraphics.buffers.cameraDevice.buffer.handle)
        copyCameraBarrier.offset(pluginGraphics.buffers.cameraDevice.offset)
        copyCameraBarrier.size(pluginGraphics.buffers.cameraDevice.size)

        val copyTransformBarrier = copyBufferBarriers[1]
        copyTransformBarrier.`sType$Default`()
        copyTransformBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
        copyTransformBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
        copyTransformBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        copyTransformBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        copyTransformBarrier.buffer(pluginGraphics.buffers.transformationMatrixDevice.buffer.handle)
        copyTransformBarrier.offset(pluginGraphics.buffers.transformationMatrixDevice.offset)
        copyTransformBarrier.size(pluginGraphics.buffers.transformationMatrixDevice.size)

        vkCmdPipelineBarrier(
            commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT,
            VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT or VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
            0, null, copyBufferBarriers, null
        )

        val clearValues = VkClearValue.calloc(2, stack)
        val colorClearValue = clearValues[0]
        colorClearValue.color { color ->
            color.float32(0, 1f)
            color.float32(1, 1f)
            color.float32(2, 0f)
            color.float32(3, 1f)
        }
        val depthClearValue = clearValues[1]
        depthClearValue.depthStencil { depthStencil ->
            depthStencil.depth(1f) // 1 is at the far plane
            depthStencil.stencil(0) // I don't really know what to do this this...
        }

        val biRenderPass = VkRenderPassBeginInfo.calloc(stack)
        biRenderPass.`sType$Default`()
        biRenderPass.renderPass(pluginState.graphics.basicRenderPass)
        // framebuffer will be filled in later
        biRenderPass.renderArea { renderArea ->
            renderArea.offset { offset -> offset.set(0, 0) }
            renderArea.extent { extent -> extent.set(gameState.vrManager.getWidth(), gameState.vrManager.getHeight()) }
        }
        biRenderPass.clearValueCount(2)
        biRenderPass.pClearValues(clearValues)

        for ((eyeIndex, framebuffer) in arrayOf(pluginState.graphics.basicLeftFramebuffer, pluginState.graphics.basicRightFramebuffer).withIndex()) {
            biRenderPass.framebuffer(framebuffer)

            vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pluginState.graphics.basicGraphicsPipeline.handle)
            vkCmdBindVertexBuffers(
                commandBuffer, 0,
                stack.longs(pluginGraphics.mainMenu.modelSet.vertexBuffer.handle),
                stack.longs(0)
            )
            vkCmdBindIndexBuffer(
                commandBuffer,
                pluginGraphics.mainMenu.modelSet.indexBuffer.handle,
                0,
                VK_INDEX_TYPE_UINT32
            )
            vkCmdPushConstants(
                commandBuffer,
                pluginGraphics.basicGraphicsPipeline.pipelineLayout,
                VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT,
                0,
                stack.ints(eyeIndex)
            )
            vkCmdBindDescriptorSets(
                commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                pluginState.graphics.basicGraphicsPipeline.pipelineLayout,
                0, stack.longs(pluginGraphics.basicStaticDescriptorSet, pluginGraphics.basicDynamicDescriptorSet), null
            )
            vkCmdDrawIndexedIndirectCount(
                commandBuffer,
                pluginGraphics.buffers.indirectDrawDevice.buffer.handle, pluginGraphics.buffers.indirectDrawDevice.offset,
                pluginGraphics.buffers.indirectDrawCountDevice.buffer.handle, pluginGraphics.buffers.indirectDrawCountDevice.offset,
                MAX_NUM_INDIRECT_DRAW_CALLS, VkDrawIndexedIndirectCommand.SIZEOF
            )
            vkCmdEndRenderPass(commandBuffer)
        }

        assertVkSuccess(
            vkEndCommandBuffer(commandBuffer), "EndCommandBuffer", "standard plug-in: draw main menu"
        )

        Pair(commandPool, commandBuffer)
    }
}

fun fillDrawingBuffers(
    pluginInstance: PluginInstance, gameState: StaticGameState,
    averageEyePosition: Vector3f, leftEyeMatrix: Matrix4f, rightEyeMatrix: Matrix4f
) {
    val numTerrainDrawCalls = 300
    val numDrawCalls = numTerrainDrawCalls + 1
    val graphicsState = (pluginInstance.state as StandardPluginState).graphics
    val buffers = graphicsState.buffers

    buffers.indirectDrawCountHost.putInt(0, numDrawCalls)

    // TODO Find a more stable way to determine these
    val skyland = graphicsState.mainMenu.skyland
    val numTerrainIndices = (skyland.indices.size / 4).toInt()
    val firstTerrainIndex = (skyland.indices.offset / 4).toInt()
    val terrainVertexOffset = (skyland.vertices.offset / BasicVertex.SIZE).toInt()

    val flower1 = graphicsState.mainMenu.flower1
    val numFlowerIndices = (flower1.indices.size / 4).toInt()
    val firstFlowerIndex = (flower1.indices.offset / 4).toInt()
    val flowerVertexOffset = (flower1.vertices.offset / BasicVertex.SIZE).toInt()

    for (currentDrawCall in 0 until numTerrainDrawCalls) {
        val drawCall1 = VkDrawIndexedIndirectCommand.create(memAddress(buffers.indirectDrawHost) + currentDrawCall * VkDrawIndexedIndirectCommand.SIZEOF)
        drawCall1.indexCount(numTerrainIndices)
        drawCall1.instanceCount(1)
        drawCall1.firstIndex(firstTerrainIndex)
        drawCall1.vertexOffset(terrainVertexOffset)
        drawCall1.firstInstance(currentDrawCall)

        val transformationMatrix1 = Matrix4f().scale(10f).translate(-10f + 2f * ((currentDrawCall % 100) / 10), -4f + 4f * (currentDrawCall / 100), -10f + 2f * (currentDrawCall % 10))
        transformationMatrix1.get(64 * currentDrawCall, buffers.transformationMatrixHost)
    }

    val numFlowerMatrices = FlowerGenerators.BUSH_SIZE1
    VkDrawIndexedIndirectCommand.create(memAddress(buffers.indirectDrawHost) + 300 * VkDrawIndexedIndirectCommand.SIZEOF).run {
        indexCount(numFlowerIndices)
        instanceCount(numFlowerMatrices)
        firstIndex(firstFlowerIndex)
        vertexOffset(flowerVertexOffset)
        firstInstance(300)

        val rng = Random(1234)

        for (flowerMatrixIndex in 0 until numFlowerMatrices) {
            val transformationMatrix = Matrix4f().scale(2f).translate(8 * rng.nextFloat() - 4, 0f, 8 * rng.nextFloat() - 4)
            transformationMatrix.get(64 * (300 + flowerMatrixIndex), buffers.transformationMatrixHost)
        }
    }

    leftEyeMatrix.get(0, buffers.cameraHost)
    rightEyeMatrix.get(64, buffers.cameraHost)
    buffers.cameraHost.putFloat(128, averageEyePosition.x)
    buffers.cameraHost.putFloat(132, averageEyePosition.y)
    buffers.cameraHost.putFloat(136, averageEyePosition.z)
}
