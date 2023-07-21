package troll.demos;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import troll.builder.TrollBuilder;
import troll.builder.TrollSwapchainBuilder;
import troll.builder.instance.ValidationFeatures;
import troll.images.VmaImage;
import troll.instance.TrollInstance;
import troll.pipelines.ShaderInfo;
import troll.swapchain.SwapchainResourceManager;
import troll.sync.ResourceUsage;
import troll.sync.WaitSemaphore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;
import static java.lang.Thread.sleep;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memShortBuffer;
import static org.lwjgl.util.vma.Vma.vmaDestroyBuffer;
import static org.lwjgl.util.vma.Vma.vmaDestroyImage;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;
import static troll.exceptions.VulkanFailureException.assertVkSuccess;

public class TerrainPlayground {

    private static long createRenderPass(MemoryStack stack, TrollInstance troll, int depthFormat) {
        var attachments = VkAttachmentDescription.calloc(2, stack);
        var colorAttachment = attachments.get(0);
        colorAttachment.flags(0);
        colorAttachment.format(troll.swapchainSettings.surfaceFormat().format());
        colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
        colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
        colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
        colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
        colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        var depthAttachment = attachments.get(1);
        depthAttachment.flags(0);
        depthAttachment.format(depthFormat);
        depthAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
        depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
        depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
        depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
        depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
        depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        var colorReference = VkAttachmentReference.calloc(1, stack);
        colorReference.attachment(0);
        colorReference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        var depthReference = VkAttachmentReference.calloc();
        depthReference.attachment(1);
        depthReference.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        var subpass = VkSubpassDescription.calloc(1, stack);
        subpass.flags(0);
        subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
        subpass.pInputAttachments(null);
        subpass.colorAttachmentCount(1);
        subpass.pColorAttachments(colorReference);
        subpass.pResolveAttachments(null);
        subpass.pDepthStencilAttachment(depthReference);
        subpass.pPreserveAttachments(null);

        var dependencies = VkSubpassDependency.calloc(2, stack);
        var colorDependency = dependencies.get(0);
        colorDependency.srcSubpass(VK_SUBPASS_EXTERNAL);
        colorDependency.dstSubpass(0);
        colorDependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
        colorDependency.srcAccessMask(0);
        colorDependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
        colorDependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        var depthDependency = dependencies.get(1);
        depthDependency.srcSubpass(VK_SUBPASS_EXTERNAL);
        depthDependency.dstSubpass(0);
        depthDependency.srcStageMask(VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);
        depthDependency.srcAccessMask(0);
        depthDependency.dstStageMask(VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
        depthDependency.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

        var ciRenderPass = VkRenderPassCreateInfo.calloc(stack);
        ciRenderPass.sType$Default();
        ciRenderPass.pAttachments(attachments);
        ciRenderPass.pSubpasses(subpass);
        ciRenderPass.pDependencies(dependencies);

        var pRenderPass = stack.callocLong(1);
        assertVkSuccess(vkCreateRenderPass(
                troll.vkDevice(), ciRenderPass, null, pRenderPass
        ), "CreateRenderPass", "TerrainPlayground");
        long renderPass = pRenderPass.get(0);
        troll.debug.name(stack, renderPass, VK_OBJECT_TYPE_RENDER_PASS, "TerrainRendering");
        return renderPass;
    }

    private static long createDescriptorSetLayout(MemoryStack stack, TrollInstance troll) {
        var bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);
        var camera = bindings.get(0);
        camera.binding(0);
        camera.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
        camera.descriptorCount(1);
        camera.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);
        camera.pImmutableSamplers(null);
        var heightMap = bindings.get(1);
        heightMap.binding(1);
        heightMap.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
        heightMap.descriptorCount(1);
        heightMap.stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
        heightMap.pImmutableSamplers(null);

        return troll.descriptors.createLayout(stack, bindings, "TerrainDescriptorSetLayout");
    }

    private static long createGroundPipelineLayout(MemoryStack stack, TrollInstance troll, long descriptorSetLayout) {
        var pushConstants = VkPushConstantRange.calloc(1, stack);
        pushConstants.offset(0);
        pushConstants.size(36);
        pushConstants.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

        return troll.pipelines.createLayout(stack, pushConstants, "GroundPipelineLayout", descriptorSetLayout);
    }

    private static long createGroundPipeline(MemoryStack stack, TrollInstance troll, long pipelineLayout, long renderPass) {
        var vertexShader = troll.pipelines.createShaderModule(
                stack, "troll/graphics/ground.vert.spv", "GroundVertexShader"
        );
        var fragmentShader = troll.pipelines.createShaderModule(
                stack, "troll/graphics/ground.frag.spv", "GroundFragmentShader"
        );

        var vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack);
        vertexInput.sType$Default();
        vertexInput.pVertexBindingDescriptions(null);
        vertexInput.pVertexAttributeDescriptions(null);

        var ciPipelines = VkGraphicsPipelineCreateInfo.calloc(1, stack);
        var ciGroundPipeline = ciPipelines.get(0);
        ciGroundPipeline.sType$Default();
        troll.pipelines.shaderStages(
                stack, ciGroundPipeline,
                new ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, vertexShader, null),
                new ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, fragmentShader, null)
        );
        ciGroundPipeline.pVertexInputState(vertexInput);
        troll.pipelines.simpleInputAssembly(stack, ciGroundPipeline);
        troll.pipelines.dynamicViewports(stack, ciGroundPipeline, 1);
        troll.pipelines.simpleRasterization(stack, ciGroundPipeline, VK_CULL_MODE_BACK_BIT);
        troll.pipelines.noMultisampling(stack, ciGroundPipeline);
        troll.pipelines.simpleDepthStencil(stack, ciGroundPipeline, VK_COMPARE_OP_LESS_OR_EQUAL);
        troll.pipelines.noColorBlending(stack, ciGroundPipeline, 1);
        troll.pipelines.dynamicStates(stack, ciGroundPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR);
        ciGroundPipeline.layout(pipelineLayout);
        ciGroundPipeline.renderPass(renderPass);
        ciGroundPipeline.subpass(0);

        var pPipeline = stack.callocLong(1);
        assertVkSuccess(vkCreateGraphicsPipelines(
                troll.vkDevice(), VK_NULL_HANDLE, ciPipelines, null, pPipeline
        ), "CreateGraphicsPipelines", "GroundPipeline");
        long groundPipeline = pPipeline.get(0);
        troll.debug.name(stack, groundPipeline, VK_OBJECT_TYPE_PIPELINE, "GroundPipeline");

        vkDestroyShaderModule(troll.vkDevice(), vertexShader, null);
        vkDestroyShaderModule(troll.vkDevice(), fragmentShader, null);
        return groundPipeline;
    }

    private static VmaImage createHeightImage(TrollInstance troll) {
        try (var stack = stackPush()){
            var input = TerrainPlayground.class.getClassLoader().getResourceAsStream("troll/height/N44E006.hgt");
            assert input != null;
            var content = input.readAllBytes();
            input.close();

            int numValues = content.length / 2;
            if (content.length % 2 != 0) throw new RuntimeException("Size is odd");
            int gridSize = (int) sqrt(numValues);
            if (gridSize * gridSize != numValues) throw new RuntimeException(numValues + " is not a square");

            var contentBuffer = ByteBuffer.wrap(content).order(ByteOrder.BIG_ENDIAN).asShortBuffer();

            var image = troll.images.createSimple(
                    stack, gridSize, gridSize, VK_FORMAT_R16_SINT, VK_SAMPLE_COUNT_1_BIT,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_ASPECT_COLOR_BIT, "HeightImage"
            );

            var stagingBuffer = troll.buffers.createMapped(
                    content.length, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "HeightImageStagingBuffer"
            );
            var stagingHostBuffer = memShortBuffer(stagingBuffer.hostAddress(), numValues);
            var commandPool = troll.commands.createPool(0, troll.queueFamilies().graphics().index(), "HeightImageCopyPool");
            var commandBuffer = troll.commands.createPrimaryBuffers(commandPool, 1, "HeightImageCopyCommands")[0];
            var fence = troll.sync.createFences(false, 1, "WaitHeightImageCopy")[0];

            int lowest = Integer.MAX_VALUE;
            int highest = Integer.MIN_VALUE;
            for (int counter = 0; counter < numValues; counter++) {
                short value = contentBuffer.get();
                stagingHostBuffer.put(value);
                if (value < lowest && value != -32768) lowest = value;
                if (value > highest) highest = value;
                if (counter == numValues / 2) System.out.println("middle value is " + value);
            }
            System.out.println("lowest is " + lowest + " and highest is " + highest);

            troll.commands.begin(commandBuffer, stack, "CopyHeightImage");
            troll.commands.transitionColorLayout(
                    stack, commandBuffer, image.vkImage(), VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    null, new ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
            );
            troll.commands.copyBufferToImage(
                    commandBuffer, stack, VK_IMAGE_ASPECT_COLOR_BIT, image.vkImage(),
                    gridSize, gridSize, stagingBuffer.buffer().vkBuffer()
            );
            troll.commands.transitionColorLayout(
                    stack, commandBuffer, image.vkImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    new ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
                    new ResourceUsage(VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT)
            );
            assertVkSuccess(vkEndCommandBuffer(commandBuffer), "EndCommandBuffer", "CopyHeightImage");

            troll.queueFamilies().graphics().queues().get(0).submit(
                    commandBuffer, "CopyHeightImage", new WaitSemaphore[0], fence
            );
            assertVkSuccess(vkWaitForFences(
                    troll.vkDevice(), stack.longs(fence), true, 100_000_000
            ), "WaitForFences", "CopyHeightImage");

            vkDestroyFence(troll.vkDevice(), fence, null);
            vkDestroyCommandPool(troll.vkDevice(), commandPool, null);
            vmaDestroyBuffer(troll.vmaAllocator(), stagingBuffer.buffer().vkBuffer(), stagingBuffer.buffer().vmaAllocation());
            return image;
        } catch (IOException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var troll = new TrollBuilder(
                VK_API_VERSION_1_2, "TerrainPlayground", VK_MAKE_VERSION(0, 1, 0)
        )
                .validation(new ValidationFeatures(true, true, false, true, true))
                .window(0L, 1000, 800, new TrollSwapchainBuilder(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
                .build();

        var heightImage = createHeightImage(troll);
        var uniformBuffer = troll.buffers.createMapped(
                64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, "UniformBuffer"
        );
        long renderPass;
        long descriptorSetLayout;
        long pipelineLayout;
        long groundPipeline;
        long descriptorPool;
        long descriptorSet;
        int depthFormat;
        long heightSampler;
        try (var stack = stackPush()) {
            depthFormat = troll.images.chooseDepthStencilFormat(
                    stack, VK_FORMAT_X8_D24_UNORM_PACK32, VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT
            );
            renderPass = createRenderPass(stack, troll, depthFormat);

            descriptorSetLayout = createDescriptorSetLayout(stack, troll);
            pipelineLayout = createGroundPipelineLayout(stack, troll, descriptorSetLayout);
            groundPipeline = createGroundPipeline(stack, troll, pipelineLayout, renderPass);

            var poolSizes = VkDescriptorPoolSize.calloc(2, stack);
            var uniformPoolSize = poolSizes.get(0);
            uniformPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformPoolSize.descriptorCount(1);
            var heightMapPoolSize = poolSizes.get(1);
            heightMapPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            heightMapPoolSize.descriptorCount(1);

            var ciDescriptorPool = VkDescriptorPoolCreateInfo.calloc(stack);
            ciDescriptorPool.sType$Default();
            ciDescriptorPool.maxSets(1);
            ciDescriptorPool.pPoolSizes(poolSizes);

            var pDescriptorPool = stack.callocLong(1);
            assertVkSuccess(vkCreateDescriptorPool(
                    troll.vkDevice(), ciDescriptorPool, null, pDescriptorPool
            ), "CreateDescriptorPool", "TerrainPlayground");
            descriptorPool = pDescriptorPool.get(0);
            troll.debug.name(stack, descriptorPool, VK_OBJECT_TYPE_DESCRIPTOR_POOL, "TerrainDescriptorPool");

            descriptorSet = troll.descriptors.allocate(stack, 1, descriptorPool, "TerrainDescriptor", descriptorSetLayout)[0];

            heightSampler = troll.images.createSampler(
                    stack, VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
                    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, 0f, 0f, false, "HeightSampler"
            );

            var heightMapInfo = VkDescriptorImageInfo.calloc(1, stack);
            heightMapInfo.sampler(heightSampler);
            heightMapInfo.imageView(heightImage.vkImageView());
            heightMapInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            var descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);
            var uniformWrite = descriptorWrites.get(0);
            uniformWrite.sType$Default();
            uniformWrite.dstSet(descriptorSet);
            uniformWrite.dstBinding(0);
            uniformWrite.dstArrayElement(0);
            uniformWrite.descriptorCount(1);
            uniformWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformWrite.pBufferInfo(troll.descriptors.bufferInfo(stack, uniformBuffer.buffer()));
            var heightMapWrite = descriptorWrites.get(1);
            heightMapWrite.sType$Default();
            heightMapWrite.dstSet(descriptorSet);
            heightMapWrite.dstBinding(1);
            heightMapWrite.dstArrayElement(0);
            heightMapWrite.descriptorCount(1);
            heightMapWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            heightMapWrite.pImageInfo(heightMapInfo);

            vkUpdateDescriptorSets(troll.vkDevice(), descriptorWrites, null);
        }

        int numFramesInFlight = 3;
        var commandPool = troll.commands.createPool(
                VK_COMMAND_POOL_CREATE_TRANSIENT_BIT | VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
                troll.queueFamilies().graphics().index(), "TerrainPool"
        );
        var commandBuffers = troll.commands.createPrimaryBuffers(
                commandPool, numFramesInFlight, "TerrainCommands"
        );
        var commandFences = troll.sync.createFences(true, numFramesInFlight, "TerrainCommandFences");

        long frameCounter = 0;
        var swapchainResources = new SwapchainResourceManager<>(swapchainImage -> {
            try (var stack = stackPush()) {
                long imageView = troll.images.createView(
                        stack, swapchainImage.vkImage(), troll.swapchainSettings.surfaceFormat().format(),
                        VK_IMAGE_ASPECT_COLOR_BIT, "SwapchainView" + swapchainImage.imageIndex()
                );

                var depthImage = troll.images.createSimple(
                        stack, swapchainImage.width(), swapchainImage.height(), depthFormat,
                        VK_SAMPLE_COUNT_1_BIT, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VK_IMAGE_ASPECT_DEPTH_BIT, "Depth"
                );

                long framebuffer = troll.images.createFramebuffer(
                        stack, renderPass, swapchainImage.width(), swapchainImage.height(),
                        "TerrainFramebuffer", imageView, depthImage.vkImageView()
                );

                return new AssociatedSwapchainResources(framebuffer, imageView, depthImage);
            }
        }, resources -> {
            vkDestroyFramebuffer(troll.vkDevice(), resources.framebuffer, null);
            vkDestroyImageView(troll.vkDevice(), resources.imageView, null);
            vkDestroyImageView(troll.vkDevice(), resources.depthImage.vkImageView(), null);
            vmaDestroyImage(troll.vmaAllocator(), resources.depthImage.vkImage(), resources.depthImage.vmaAllocation());
        });

        long referenceTime = System.currentTimeMillis();
        long referenceFrames = 0;

        class Camera {
            float yaw = 0f;
            float pitch = -40f;

            float x = 0f;
            float y = 2039.6f + 1.7f;
            float z = 0f;
        }

        class CameraController {
            double oldX = Double.NaN;
            double oldY = Double.NaN;
        }

        var camera = new Camera();
        var cameraController = new CameraController();

        glfwSetKeyCallback(troll.glfwWindow(), ((window, key, scancode, action, mods) -> {
            float dx = 0f, dy = 0f, dz = 0f;
            if (key == GLFW_KEY_A) dx = -1f;
            if (key == GLFW_KEY_D) dx = 1f;
            if (key == GLFW_KEY_S) dz = 1f;
            if (key == GLFW_KEY_W) dz = -1f;
            if (key == GLFW_KEY_Q) dy = -1f;
            if (key == GLFW_KEY_E) dy = 1f;

            float scale = 1f;
            if ((mods & GLFW_MOD_SHIFT) != 0) scale *= 0.1f;
            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) scale *= 10f;

            camera.x += dx * scale;
            camera.y += dy * scale;
            camera.z += dz * scale;
        }));

        glfwSetCursorPosCallback(troll.glfwWindow(), (window, x, y) -> {
            if (!Double.isNaN(cameraController.oldX) && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                double dx = x - cameraController.oldX;
                double dy = y - cameraController.oldY;
                camera.yaw -= 0.5 * dx;
                camera.pitch -= 0.2 * dy;
                if (camera.pitch < -90) camera.pitch = -90;
                if (camera.pitch > 90) camera.pitch = 90;
                if (camera.yaw < 0) camera.yaw += 360;
                if (camera.yaw > 360) camera.yaw -= 360;
            }

            cameraController.oldX = x;
            cameraController.oldY = y;
        });

        while (!glfwWindowShouldClose(troll.glfwWindow())) {
            glfwPollEvents();

            long currentTime = System.currentTimeMillis();
            if (currentTime > 1000 + referenceTime) {
                System.out.println("FPS is " + (frameCounter - referenceFrames));
                referenceTime = currentTime;
                referenceFrames = frameCounter;
            }

            try (var stack = stackPush()) {
                var swapchainImage = troll.swapchains.acquireNextImage(VK_PRESENT_MODE_MAILBOX_KHR);
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
                troll.sync.waitAndReset(stack, fence, 100_000_000);

                troll.commands.begin(commandBuffer, stack, "TerrainDraw");

                var clearValues = VkClearValue.calloc(2, stack);
                var clearColor = clearValues.get(0);
                clearColor.color().float32(stack.floats(0.2f, 0.8f, 0.6f, 1f));
                var clearDepth = clearValues.get(1);
                clearDepth.depthStencil().set(1f, 0);

                var biRenderPass = VkRenderPassBeginInfo.calloc(stack);
                biRenderPass.sType$Default();
                biRenderPass.renderPass(renderPass);
                biRenderPass.framebuffer(imageResources.framebuffer);
                biRenderPass.renderArea().offset().set(0, 0);
                biRenderPass.renderArea().extent().set(swapchainImage.width(), swapchainImage.height());
                biRenderPass.clearValueCount(2);
                biRenderPass.pClearValues(clearValues);

                vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE);
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, groundPipeline);
                troll.commands.dynamicViewportAndScissor(stack, commandBuffer, swapchainImage.width(), swapchainImage.height());
                vkCmdBindDescriptorSets(
                        commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout,
                        0, stack.longs(descriptorSet), null
                );

                var cameraMatrix = new Matrix4f()
                        .scale(1f, -1f, 1f)
                        .perspective(
                            (float) toRadians(45f), (float) swapchainImage.width() / (float) swapchainImage.height(),
                            0.1f, 50_000f, true
                        )
                        .rotateX((float) toRadians(-camera.pitch))
                        .rotateY((float) toRadians(-camera.yaw))
                        ;
                cameraMatrix.getToAddress(uniformBuffer.hostAddress());

                int gridSize = 3601;
                float visibleFraction = 0.15f;
                float realTextureSize = gridSize * 30f;
                float pixelSize = 30f;
                float quadSize = 5f;

                int numRows = (int) (gridSize * 2 * visibleFraction * pixelSize / quadSize);
                int numColumns = (int) (gridSize * 2 * visibleFraction * pixelSize / quadSize);
                int numQuads = numRows * numColumns;

                float heightScale = 1f;

                float textureOffsetU = 0.5f - visibleFraction;
                float textureOffsetV = 0.5f - visibleFraction;

                var pushConstants = stack.calloc(36);
                pushConstants.putFloat(0, (textureOffsetU - 0.5f) * pixelSize * gridSize - camera.x);
                pushConstants.putFloat(4, 0f - camera.y);
                pushConstants.putFloat(8, (textureOffsetV - 0.5f) * pixelSize * gridSize - camera.z);
                pushConstants.putFloat(12, quadSize);
                pushConstants.putFloat(16, textureOffsetU);
                pushConstants.putFloat(20, textureOffsetV);
                pushConstants.putFloat(24, heightScale);
                pushConstants.putFloat(28, realTextureSize);
                pushConstants.putInt(32, numColumns);

                vkCmdPushConstants(commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants);
                vkCmdDraw(commandBuffer, 6 * numQuads, 1, 0, 0);
                vkCmdEndRenderPass(commandBuffer);

                assertVkSuccess(vkEndCommandBuffer(commandBuffer), "EndCommandBuffer", "TerrainDraw");

                troll.queueFamilies().graphics().queues().get(0).submit(
                        commandBuffer, "TerrainDraw", waitSemaphores, fence, swapchainImage.presentSemaphore()
                );

                troll.swapchains.presentImage(swapchainImage);
                frameCounter += 1;
            }
        }

        assertVkSuccess(vkDeviceWaitIdle(troll.vkDevice()), "DeviceWaitIdle", "FinishTerrainPlayground");
        for (long fence : commandFences) vkDestroyFence(troll.vkDevice(), fence, null);
        vkDestroyCommandPool(troll.vkDevice(), commandPool, null);

        vkDestroyDescriptorPool(troll.vkDevice(), descriptorPool, null);
        vkDestroyPipeline(troll.vkDevice(), groundPipeline, null);
        vkDestroyPipelineLayout(troll.vkDevice(), pipelineLayout, null);
        vkDestroyDescriptorSetLayout(troll.vkDevice(), descriptorSetLayout, null);
        vkDestroyRenderPass(troll.vkDevice(), renderPass, null);
        vmaDestroyBuffer(troll.vmaAllocator(), uniformBuffer.buffer().vkBuffer(), uniformBuffer.buffer().vmaAllocation());
        vkDestroyImageView(troll.vkDevice(), heightImage.vkImageView(), null);
        vmaDestroyImage(troll.vmaAllocator(), heightImage.vkImage(), heightImage.vmaAllocation());
        vkDestroySampler(troll.vkDevice(), heightSampler, null);

        troll.destroy();
    }

    private record AssociatedSwapchainResources(
            long framebuffer,
            long imageView,
            VmaImage depthImage
    ) {}
}
