package troll.demos;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
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
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.Math.*;
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

    /**
     * The width and height of the height image, in pixels
     */
    private static final int HEIGHT_IMAGE_NUM_PIXELS = 3601;

    /**
     * The real-world size of 1 pixel on the height image, in meters
     */
    private static final float HEIGHT_IMAGE_PIXEL_SIZE = 30f;

    /**
     * The real-world size of the height image, in meters
     */
    private static final float HEIGHT_IMAGE_SIZE = HEIGHT_IMAGE_NUM_PIXELS * HEIGHT_IMAGE_PIXEL_SIZE;

    private static int minHeight = Integer.MAX_VALUE;
    private static int maxHeight = Integer.MIN_VALUE;
    private static ShortBuffer hostHeightBuffer;

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

            hostHeightBuffer = ByteBuffer.wrap(content).order(ByteOrder.BIG_ENDIAN).asShortBuffer();

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

            short previousValue = 0;
            for (int counter = 0; counter < numValues; counter++) {
                short value = hostHeightBuffer.get(counter);
                if (value != Short.MIN_VALUE) {
                    stagingHostBuffer.put(value);
                    previousValue = value;
                    if (value < minHeight) minHeight = value;
                    if (value > maxHeight) maxHeight = value;
                    if (counter == numValues / 2) System.out.println("middle value is " + value);
                } else stagingHostBuffer.put(previousValue);
            }
            System.out.println("lowest is " + minHeight + " and highest is " + maxHeight);

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
            float pitch = 0f;

            float x = 0f;
            float y = 2055.3f + 1.7f;
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
            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) scale *= 100f;

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

                float fieldOfView = 45f;
                float aspectRatio = (float) swapchainImage.width() / (float) swapchainImage.height();

                var cameraMatrix = new Matrix4f()
                        .scale(1f, -1f, 1f)
                        .perspective(
                            (float) toRadians(fieldOfView), aspectRatio,
                            0.1f, 50_000f, true
                        )
                        .rotateX((float) toRadians(-camera.pitch))
                        .rotateY((float) toRadians(-camera.yaw))
                        ;
                cameraMatrix.getToAddress(uniformBuffer.hostAddress());

                var fragmentsToRender = new ArrayList<TerrainFragment>();
                float cameraU = 2f * camera.x / HEIGHT_IMAGE_SIZE + 0.5f;
                float cameraV = 2f * camera.z / HEIGHT_IMAGE_SIZE + 0.5f;
                partitionTerrainSpace(cameraU, cameraV, 0.0001f, 0.2f, 8, fragmentsToRender);
//                while (fragmentsToRender.size() > 4) fragmentsToRender.remove(fragmentsToRender.size() - 1);
//                fragmentsToRender.remove(0);
//                fragmentsToRender.remove(0);
//                fragmentsToRender.remove(0);

                var frustumCullMatrix = new Matrix4f()
                        .perspective(
                                (float) toRadians(fieldOfView), aspectRatio,
                                0.1f, 50_000f, true
                        )
                        .rotateX((float) toRadians(-camera.pitch))
                        .rotateY((float) toRadians(-camera.yaw))
                        ;

                float heightScale = 1f;

                var pushConstants = stack.calloc(36);
                int quadCount = 0;
                int fragmentCount = 0;
                int yawQuadCount = 0;
                int xQuadCount = 0;
                int pitchQuadCount = 0;
                int zQuadCount = 0;
                for (var fragment : fragmentsToRender) {

                    float minScreenX = Float.POSITIVE_INFINITY;
                    float maxScreenX = Float.NEGATIVE_INFINITY;
                    float minScreenY = Float.POSITIVE_INFINITY;
                    float maxScreenY = Float.NEGATIVE_INFINITY;
                    float minZ = Float.POSITIVE_INFINITY;
                    float maxZ = Float.NEGATIVE_INFINITY;
                    float minYawDifference = Float.POSITIVE_INFINITY;
                    float maxYawDifference = Float.NEGATIVE_INFINITY;
                    float minYaw = Float.POSITIVE_INFINITY;
                    float maxYaw = Float.NEGATIVE_INFINITY;
                    boolean anyProperY = false;

                    float[][] uvs = {
                            {fragment.minU, fragment.minV},
                            {fragment.maxU, fragment.minV},
                            {fragment.maxU, fragment.maxV},
                            {fragment.minU, fragment.maxV}
                    };
                    //float dy = HEIGHT_IMAGE_SIZE * 0.3f * (abs(fragment.maxU - fragment.minU) + abs(fragment.maxV - fragment.minV));
                    for (float[] uv : uvs) {
                        float dx = (uv[0] - cameraU) * HEIGHT_IMAGE_SIZE;
                        float dz = (uv[1] - cameraV) * HEIGHT_IMAGE_SIZE;
                        float yaw = (float) toDegrees(atan2(dx, -dz));
                        minYaw = min(minYaw, yaw);
                        maxYaw = max(maxYaw, yaw);


                        int minHeight = Integer.MAX_VALUE;
                        int maxHeight = Integer.MIN_VALUE;
                        for (int heightOffsetX = -1; heightOffsetX <= 1; heightOffsetX++) {
                            for (int heightOffsetZ = -1; heightOffsetZ <= 1; heightOffsetZ++) {
                                int heightIndexX = (int) (heightOffsetX + HEIGHT_IMAGE_NUM_PIXELS * uv[0]);
                                if (heightIndexX < 0) heightIndexX = 0;
                                if (heightIndexX >= HEIGHT_IMAGE_NUM_PIXELS) heightIndexX = HEIGHT_IMAGE_NUM_PIXELS - 1;

                                int heightIndexZ = (int) (heightOffsetZ + HEIGHT_IMAGE_NUM_PIXELS * uv[1]);
                                if (heightIndexZ < 0) heightIndexZ = 0;
                                if (heightIndexZ >= HEIGHT_IMAGE_NUM_PIXELS) heightIndexZ = HEIGHT_IMAGE_NUM_PIXELS - 1;
                                int heightIndex = heightIndexX + HEIGHT_IMAGE_NUM_PIXELS * heightIndexZ;
                                int height = hostHeightBuffer.get(heightIndex);
                                minHeight = min(minHeight, height);
                                maxHeight = max(maxHeight, height);
                            }
                        }

                        var rawLowScreen = frustumCullMatrix.transform(dx, minHeight - camera.y, dz, 1f, new Vector4f());
                        var lowScreen = new Vector3f(rawLowScreen.x / rawLowScreen.w, rawLowScreen.y / rawLowScreen.w, rawLowScreen.z / rawLowScreen.w);
                        var rawHighScreen = frustumCullMatrix.transform(dx, maxHeight - camera.y, dz, 1f, new Vector4f());
                        var highScreen = new Vector3f(rawHighScreen.x / rawHighScreen.w, rawHighScreen.y / rawHighScreen.w, rawHighScreen.z / rawHighScreen.w);

                        minScreenX = min(minScreenX, min(lowScreen.x, highScreen.x));
                        maxScreenX = max(maxScreenX, max(lowScreen.x, highScreen.x));
                        if (highScreen.y > lowScreen.y) {
                            anyProperY = true;
                        }
                        minScreenY = min(minScreenY, min(lowScreen.y, highScreen.y));
                        maxScreenY = max(maxScreenY, max(lowScreen.y, highScreen.y));

                        float angleDifference = camera.yaw + yaw;
                        minYawDifference = min(minYawDifference, angleDifference);
                        maxYawDifference = max(maxYawDifference, angleDifference);

                        minZ = min(minZ, min(lowScreen.z, highScreen.z));
                        maxZ = max(maxZ, max(lowScreen.z, highScreen.z));
                    }

                    if (minYawDifference > 180) {
                        minYawDifference -= 360;
                        maxYawDifference -= 360;
                    }

                    //System.out.printf("minPitch is %.1f and maxPitch is %.1f and camera pitch is %.1f\n", minPitch + camera.pitch, maxPitch + camera.pitch, camera.pitch);
                    //if (maxScreenX < -1 || minScreenX > 1 || maxScreenY < -1 || minScreenY > 1 || minZ > 1) continue;
                    //System.out.printf("minYawDifference is %.1f and maxYawDifference is %.1f and minYaw is %.1f and maxYaw is %.1f\n", minYawDifference, maxYawDifference, minYaw, maxYaw);
                    if (maxYaw - minYaw < 200f) {
                        if (maxScreenX < -1f || minScreenX > 1f) {
                            xQuadCount += fragment.numColumns() * fragment.numRows();
                            continue;
                        }

                        float yawThreshold = 1.25f * fieldOfView / aspectRatio / (float) abs(cos(toRadians(camera.pitch)));
                        if ((maxYawDifference < -yawThreshold || minYawDifference > yawThreshold)) {
                            yawQuadCount += fragment.numColumns() * fragment.numRows();
                            continue;
                        }

                        //System.out.printf("minScreenY is %.2f and maxScreenY is %.2f and pitch is %.1f\n", minScreenY, maxScreenY, camera.pitch);
                        if (maxScreenY < -1f || minScreenY > 1f || !anyProperY) {
                            pitchQuadCount += fragment.numColumns() * fragment.numRows();
                            continue;
                        }

                        //System.out.printf("minScreenX is %.2f and maxScreenX is %.2f and yaw is %.1f and minZ is %.3f and yaws are (%.1f, %.1f)\n", minScreenX, maxScreenX, camera.yaw, minZ, minYawDifference, maxYawDifference);

                    }

                    if (minZ > 1f) {
                        zQuadCount += fragment.numColumns() * fragment.numRows();
                        continue;
                    }


                    //System.out.println("maxZ is " + maxZ + " and minZ is " + minZ + " and yaw is " + camera.yaw);
                    //System.out.printf("minScreenX is %.2f and maxScreenX is %.2f and yaw is %.1f\n", minScreenX, maxScreenX, camera.yaw);
                    //System.out.printf("minScreenY is %.2f and maxScreenY is %.2f\n", minScreenY, maxScreenY);

                    pushConstants.putFloat(0, (fragment.minU - cameraU) * HEIGHT_IMAGE_SIZE);
                    pushConstants.putFloat(4, 0f - camera.y);
                    pushConstants.putFloat(8, (fragment.minV - cameraV) * HEIGHT_IMAGE_SIZE);
                    pushConstants.putFloat(12, fragment.quadSize);
                    pushConstants.putFloat(16, fragment.minU);
                    pushConstants.putFloat(20, fragment.minV);
                    pushConstants.putFloat(24, heightScale);
                    pushConstants.putFloat(28, HEIGHT_IMAGE_SIZE);
                    pushConstants.putInt(32, fragment.numColumns());

                    vkCmdPushConstants(commandBuffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants);
                    int numQuads = fragment.numRows() * fragment.numColumns();
                    vkCmdDraw(commandBuffer, 6 * numQuads, 1, 0, 0);
                    quadCount += numQuads;
                    fragmentCount += 1;
                }
                vkCmdEndRenderPass(commandBuffer);
                if (Math.random() < 0.01) {
                    System.out.println("Drew " + quadCount + " quads in " + fragmentCount + " fragments; yaw culled " + yawQuadCount + " and pitch culled " + pitchQuadCount + " and x culled " + xQuadCount + " and z culled " + zQuadCount);
                }

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

    private record TerrainFragment(
            float minU,
            float minV,
            float maxU,
            float maxV,
            float quadSize
    ) {
        int numColumns() {
            return (int) ceil((maxU - minU) * HEIGHT_IMAGE_SIZE / quadSize);
        }

        int numRows() {
            return (int) ceil((maxV - minV) * HEIGHT_IMAGE_SIZE / quadSize);
        }
    }

    private static void addPartitionFragment(
            float cameraU, float cameraV, int dx, int dy,
            float fragmentSize, float quadSize, Collection<TerrainFragment> fragments
    ) {
        float minU = cameraU + dx * fragmentSize;
        float minV = cameraV + dy * fragmentSize;
        float maxU = minU + fragmentSize;
        float maxV = minV + fragmentSize;
        if (maxU <= 0f || maxV <= 0f || minU >= 1f || minV >= 1f) return;

        fragments.add(new TerrainFragment(max(minU, 0f), max(minV, 0f), min(maxU, 1f), min(maxV, 1f), quadSize));
    }

    private static void partitionTerrainSpace(
            float cameraU, float cameraV, float initialFragmentSize, float initialQuadSize, int maxExponent,
            Collection<TerrainFragment> fragments
    ) {
        float fragmentSize = initialFragmentSize;
        float quadSize = initialQuadSize;
        //float cameraGridSize = 1000f * fragmentSize / HEIGHT_IMAGE_SIZE;
        //float cameraGridSize = quadSize / (HEIGHT_IMAGE_SIZE - 0) * (float) pow(2.4, 6);
        float cameraGridSize = 0.01f;

        cameraU = cameraGridSize * round(cameraU / cameraGridSize);
        cameraV = cameraGridSize * round(cameraV / cameraGridSize);

        for (int dx = -1; dx <= 0; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, quadSize, fragments);
            }
        }

        int exponent = 1;
        while (exponent <= maxExponent) {

            for (int rowSize : new int[] { 2, 3 }) {
                int dx = -rowSize;
                int dy = -rowSize;

                float rowQuadSize = 0.5f * rowSize == 2 ? 1.5f * quadSize : 3f * quadSize;

                for (; dx < rowSize - 1; dx++) addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, rowQuadSize, fragments);
                for (; dy < rowSize - 1; dy++) addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, rowQuadSize, fragments);
                for (; dx > -rowSize; dx--) addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, rowQuadSize, fragments);
                for (; dy > -rowSize; dy--) addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, rowQuadSize, fragments);
            }

            quadSize *= 2.4f;
            fragmentSize *= 3f;
            exponent += 1;
        }
    }
}
