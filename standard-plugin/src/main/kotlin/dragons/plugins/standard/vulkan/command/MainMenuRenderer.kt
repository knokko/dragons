package dragons.plugins.standard.vulkan.command

import dragons.plugin.PluginInstance
import dragons.plugins.standard.state.StandardPluginState
import dragons.plugins.standard.vulkan.MAX_NUM_INDIRECT_DRAW_CALLS
import dragons.state.StaticGameState
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDrawIndirectCount.vkCmdDrawIndexedIndirectCountKHR
import org.lwjgl.vulkan.VK10.*

fun createMainMenuRenderCommands(pluginInstance: PluginInstance, gameState: StaticGameState): Pair<Long, VkCommandBuffer> {
    return stackPush().use { stack ->
        val contextInfo = "standard plug-in: main menu rendering"
        val pluginState = pluginInstance.state as StandardPluginState
        val pluginGraphics = pluginState.graphics
        val graphicsState = gameState.graphics

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

        for (framebuffer in arrayOf(pluginState.graphics.basicLeftFramebuffer, pluginState.graphics.basicRightFramebuffer)) {
            biRenderPass.framebuffer(framebuffer)

            vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pluginState.graphics.basicGraphicsPipeline.handle)
            vkCmdBindVertexBuffers(
                commandBuffer, 0,
                stack.longs(pluginGraphics.vertexBuffer.buffer.handle),
                stack.longs(pluginGraphics.vertexBuffer.offset)
            )
            vkCmdBindIndexBuffer(
                commandBuffer,
                pluginGraphics.indexBuffer.buffer.handle, pluginGraphics.indexBuffer.offset,
                VK_INDEX_TYPE_UINT32
            )
            vkCmdBindDescriptorSets(
                commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                pluginState.graphics.basicGraphicsPipeline.pipelineLayout,
                0, descriptorSets, null
            )
            vkCmdDrawIndexedIndirectCountKHR(
                commandBuffer,
                pluginGraphics.indirectDrawDeviceBuffer.buffer.handle, pluginGraphics.indirectDrawDeviceBuffer.offset,
                pluginGraphics.indirectDrawCountDeviceBuffer.buffer.handle, pluginGraphics.indirectDrawCountDeviceBuffer.offset,
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
