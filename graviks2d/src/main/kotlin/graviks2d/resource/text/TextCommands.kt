package graviks2d.resource.text

import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal fun rasterizeTextAtlas(
    commandBuffer: VkCommandBuffer, textCache: TextShapeCache, isFirstDraw: Boolean
) {
    stackPush().use { stack ->

        if (textCache.currentVertexIndex > 0 || isFirstDraw) {
            val clearValues = VkClearValue.calloc(1, stack)
            val clearValue = clearValues[0]
            clearValue.color().int32(0, 0)

            val biRenderPass = VkRenderPassBeginInfo.calloc(stack)
            biRenderPass.`sType$Default`()
            biRenderPass.renderPass(textCache.context.instance.textPipelines.vkRenderPass)
            biRenderPass.framebuffer(textCache.textAtlasFramebuffer)
            biRenderPass.renderArea {
                it.offset().set(0, 0)
                it.extent().set(textCache.width, textCache.height)
            }
            biRenderPass.clearValueCount(1)
            biRenderPass.pClearValues(clearValues)

            val viewports = VkViewport.calloc(1, stack)
            val viewport = viewports[0]
            viewport.x(0f)
            viewport.y(0f)
            viewport.width(textCache.width.toFloat())
            viewport.height(textCache.height.toFloat())
            viewport.minDepth(0f)
            viewport.maxDepth(1f)

            val scissors = VkRect2D.calloc(1, stack)
            val scissor = scissors[0]
            scissor.offset().set(0, 0)
            scissor.extent().set(textCache.width, textCache.height)

            vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE)
            vkCmdBindPipeline(
                commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                textCache.context.instance.textPipelines.countPipeline
            )
            vkCmdSetViewport(commandBuffer, 0, viewports)
            vkCmdSetScissor(commandBuffer, 0, scissors)
            if (textCache.currentVertexIndex > 0) {
                vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(textCache.countVertexBuffer), stack.longs(0))
                vkCmdDraw(commandBuffer, textCache.currentVertexIndex, 1, 0, 0)
            }
            vkCmdNextSubpass(commandBuffer, VK_SUBPASS_CONTENTS_INLINE)
            vkCmdBindPipeline(
                commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                textCache.context.instance.textPipelines.oddPipeline
            )
            vkCmdBindDescriptorSets(
                commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                textCache.context.instance.textPipelines.oddPipelineLayout,
                0, stack.longs(textCache.descriptorSet), null
            )
            if (textCache.currentVertexIndex > 0) {
                vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(textCache.oddVertexBuffer), stack.longs(0))
                vkCmdDraw(commandBuffer, 6, 1, 0, 0)
            }
            vkCmdEndRenderPass(commandBuffer)

            val imageBarriers = VkImageMemoryBarrier.calloc(1, stack)
            val imageBarrier = imageBarriers[0]
            imageBarrier.`sType$Default`()
            imageBarrier.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            imageBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            imageBarrier.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            imageBarrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            imageBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            imageBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            imageBarrier.image(textCache.textOddAtlas)
            imageBarrier.subresourceRange {
                it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                it.baseMipLevel(0)
                it.levelCount(1)
                it.baseArrayLayer(0)
                it.layerCount(1)
            }

            vkCmdPipelineBarrier(
                commandBuffer, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                0, null, null, imageBarriers
            )
        }
    }
}
