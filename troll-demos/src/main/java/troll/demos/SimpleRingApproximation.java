package troll.demos;

import org.lwjgl.vulkan.*;
import troll.builder.TrollBuilder;
import troll.builder.TrollSwapchainBuilder;
import troll.builder.instance.ValidationFeatures;
import troll.pipelines.ShaderInfo;
import troll.swapchain.SwapchainResourceManager;
import troll.sync.WaitSemaphore;

import static java.lang.Thread.sleep;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class SimpleRingApproximation {

    public static void main(String[] args) throws InterruptedException {
        var instance = new TrollBuilder(
                VK_API_VERSION_1_1, "SimpleRingApproximation", VK_MAKE_VERSION(0, 2, 0)
        )
                .validation(new ValidationFeatures(true, true, false, true, true))
                .window(0L, 1000, 800, new TrollSwapchainBuilder(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
                .build();

        int numFramesInFlight = 3;
        var commandPool = instance.commands.createPool(
                VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
                instance.queueFamilies().graphics().index(),
                "Drawing"
        );
        var commandBuffers = instance.commands.createPrimaryBuffers(commandPool, numFramesInFlight, "Drawing");
        long[] commandFences = instance.sync.createFences(true, numFramesInFlight, "Fence");
        long graphicsPipeline;
        long pipelineLayout;
        long renderPass;

        try (var stack = stackPush()) {
            var pushConstants = VkPushConstantRange.calloc(1, stack);
            pushConstants.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);
            pushConstants.offset(0);
            pushConstants.size(20);

            pipelineLayout = instance.pipelines.createLayout(stack, pushConstants, "DrawingLayout");

            var attachments = VkAttachmentDescription.calloc(1, stack);
            var colorAttachment = attachments.get(0);
            colorAttachment.format(instance.swapchainSettings.surfaceFormat().format());
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            var colorReference = VkAttachmentReference.calloc(1, stack);
            colorReference.attachment(0);
            colorReference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            var subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.pInputAttachments(null);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorReference);
            subpass.pResolveAttachments(null);
            subpass.pDepthStencilAttachment(null);
            subpass.pPreserveAttachments(null);

            var dependency = VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            var ciRenderPass = VkRenderPassCreateInfo.calloc(stack);
            ciRenderPass.sType$Default();
            ciRenderPass.pAttachments(attachments);
            ciRenderPass.pSubpasses(subpass);
            ciRenderPass.pDependencies(dependency);

            var pRenderPass = stack.callocLong(1);
            assertVkSuccess(vkCreateRenderPass(
                    instance.vkDevice(), ciRenderPass, null, pRenderPass
            ), "CreateRenderPass", "RingApproximation");
            renderPass = pRenderPass.get(0);
        }

        try (var stack = stackPush()) {
            var vertexModule = instance.pipelines.createShaderModule(
                    stack, "troll/graphics/ring.vert.spv", "RingVertices"
            );
            var fragmentModule = instance.pipelines.createShaderModule(
                    stack, "troll/graphics/ring.frag.spv", "RingFragments"
            );

            var ciVertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            ciVertexInput.sType$Default();
            ciVertexInput.pVertexBindingDescriptions(null);
            ciVertexInput.pVertexAttributeDescriptions(null);

            var ciPipelines = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            var ciPipeline = ciPipelines.get(0);
            ciPipeline.sType$Default();
            instance.pipelines.shaderStages(
                    stack, ciPipeline,
                    new ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, vertexModule, null),
                    new ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, fragmentModule, null)
            );
            ciPipeline.pVertexInputState(ciVertexInput);
            instance.pipelines.simpleInputAssembly(stack, ciPipeline);
            instance.pipelines.dynamicViewports(stack, ciPipeline, 1);
            instance.pipelines.simpleRasterization(stack, ciPipeline, VK_CULL_MODE_NONE);
            instance.pipelines.noMultisampling(stack, ciPipeline);
            instance.pipelines.noDepthStencil(stack, ciPipeline);
            instance.pipelines.noColorBlending(stack, ciPipeline, 1);
            instance.pipelines.dynamicStates(stack, ciPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR);

            ciPipeline.renderPass(renderPass);
            ciPipeline.layout(pipelineLayout);

            var pPipeline = stack.callocLong(1);
            assertVkSuccess(vkCreateGraphicsPipelines(
                    instance.vkDevice(), VK_NULL_HANDLE, ciPipelines, null, pPipeline
            ), "CreateGraphicsPipelines", "RingApproximation");
            graphicsPipeline = pPipeline.get(0);

            vkDestroyShaderModule(instance.vkDevice(), vertexModule, null);
            vkDestroyShaderModule(instance.vkDevice(), fragmentModule, null);
        }

        long frameCounter = 0;
        var swapchainResources = new SwapchainResourceManager<>(swapchainImage -> {
            try (var stack = stackPush()) {
                long imageView = instance.images.createView(
                        stack, swapchainImage.vkImage(), instance.swapchainSettings.surfaceFormat().format(),
                        VK_IMAGE_ASPECT_COLOR_BIT, "SwapchainView" + swapchainImage.imageIndex()
                );

                long framebuffer = instance.images.createFramebuffer(
                        stack, renderPass, swapchainImage.width(), swapchainImage.height(),
                        "RingFramebuffer", imageView
                );

                return new AssociatedSwapchainResources(framebuffer, imageView);
            }
        }, resources -> {
            vkDestroyFramebuffer(instance.vkDevice(), resources.framebuffer, null);
            vkDestroyImageView(instance.vkDevice(), resources.imageView, null);
        });

        long referenceTime = System.currentTimeMillis();
        long referenceFrames = 0;

        while (!glfwWindowShouldClose(instance.glfwWindow())) {
            glfwPollEvents();

            long currentTime = System.currentTimeMillis();
            if (currentTime > 1000 + referenceTime) {
                System.out.println("FPS is " + (frameCounter - referenceFrames));
                referenceTime = currentTime;
                referenceFrames = frameCounter;
            }

            try (var stack = stackPush()) {
                var swapchainImage = instance.swapchains.acquireNextImage(VK_PRESENT_MODE_MAILBOX_KHR);
                if (swapchainImage == null) {
                    //noinspection BusyWait
                    sleep(100);
                    continue;
                }

                var imageResources = swapchainResources.get(swapchainImage);
                WaitSemaphore[] waitSemaphores = { new WaitSemaphore(
                        swapchainImage.acquireSemaphore(), VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                )};

                int frameIndex = (int) (frameCounter % numFramesInFlight);
                var commandBuffer = commandBuffers[frameIndex];
                long fence = commandFences[frameIndex];
                instance.sync.waitAndReset(stack, fence, 100_000_000);

                instance.commands.begin(commandBuffer, stack, "RingApproximation");

                var pColorClear = VkClearValue.calloc(1, stack);
                pColorClear.color().float32(stack.floats(0.07f, 0.4f, 0.6f, 1f));

                var biRenderPass = VkRenderPassBeginInfo.calloc(stack);
                biRenderPass.sType$Default();
                biRenderPass.renderPass(renderPass);
                biRenderPass.framebuffer(imageResources.framebuffer);
                biRenderPass.renderArea().offset().set(0, 0);
                biRenderPass.renderArea().extent().set(swapchainImage.width(), swapchainImage.height());
                biRenderPass.clearValueCount(1);
                biRenderPass.pClearValues(pColorClear);

                vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE);
                instance.commands.dynamicViewportAndScissor(stack, commandBuffer, swapchainImage.width(), swapchainImage.height());
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

                int numTriangles = 30_000_000;
                var pushConstants = stack.calloc(20);
                pushConstants.putFloat(0, 0.6f);
                pushConstants.putFloat(4, 0.8f);
                pushConstants.putFloat(8, 0.2f);
                pushConstants.putFloat(12, -0.1f);
                pushConstants.putInt(16, 2 * numTriangles);

                vkCmdPushConstants(commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants);

                vkCmdDraw(commandBuffer, 6 * numTriangles, 1, 0, 0);
                vkCmdEndRenderPass(commandBuffer);
                assertVkSuccess(vkEndCommandBuffer(commandBuffer), "RingApproximation", null);

                instance.queueFamilies().graphics().queues().get(0).submit(
                        commandBuffer, "RingApproximation", waitSemaphores, fence, swapchainImage.presentSemaphore()
                );

                instance.swapchains.presentImage(swapchainImage);
                frameCounter += 1;
            }
        }

        vkDeviceWaitIdle(instance.vkDevice());
        for (long fence : commandFences) vkDestroyFence(instance.vkDevice(), fence, null);

        vkDestroyPipelineLayout(instance.vkDevice(), pipelineLayout, null);
        vkDestroyPipeline(instance.vkDevice(), graphicsPipeline, null);
        vkDestroyRenderPass(instance.vkDevice(), renderPass, null);
        vkDestroyCommandPool(instance.vkDevice(), commandPool, null);
        instance.destroy();
    }

    private record AssociatedSwapchainResources(
            long framebuffer,
            long imageView
    ) {}
}
