package troll.demos;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import profiler.performance.PerformanceProfiler;
import profiler.performance.PerformanceStorage;
import troll.builder.TrollBuilder;
import troll.builder.TrollSwapchainBuilder;
import troll.cull.FrustumCuller;
import troll.images.VmaImage;
import troll.instance.TrollInstance;
import troll.pipelines.ShaderInfo;
import troll.swapchain.SwapchainResourceManager;
import troll.sync.ResourceUsage;
import troll.sync.WaitSemaphore;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import static java.lang.Math.*;
import static java.lang.Thread.sleep;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
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
    private static HeightLookup coarseHeightLookup;
    private static HeightLookup fineHeightLookup;
    private static HeightLookup coarseDeltaHeightLookup;

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
        var bindings = VkDescriptorSetLayoutBinding.calloc(3, stack);
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
        heightMap.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);
        heightMap.pImmutableSamplers(null);
        var normalMap = bindings.get(2);
        normalMap.binding(2);
        normalMap.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
        normalMap.descriptorCount(1);
        normalMap.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
        normalMap.pImmutableSamplers(null);

        return troll.descriptors.createLayout(stack, bindings, "TerrainDescriptorSetLayout");
    }

    private static long createGroundPipelineLayout(MemoryStack stack, TrollInstance troll, long descriptorSetLayout) {
        var pushConstants = VkPushConstantRange.calloc(1, stack);
        pushConstants.offset(0);
        pushConstants.size(28);
        pushConstants.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

        return troll.pipelines.createLayout(stack, pushConstants, "GroundPipelineLayout", descriptorSetLayout);
    }

    // The grass pipeline layout happens to be identical to the ground pipeline layout
    private static long createGrassPipelineLayout(MemoryStack stack, TrollInstance troll, long descriptorSetLayout) {
        var pushConstants = VkPushConstantRange.calloc(1, stack);
        pushConstants.offset(0);
        pushConstants.size(28);
        pushConstants.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

        return troll.pipelines.createLayout(stack, pushConstants, "GrassPipelineLayout", descriptorSetLayout);
    }

    private static long createGrassPipeline(MemoryStack stack, TrollInstance troll, long pipelineLayout, long renderPass) {
        var vertexShader = troll.pipelines.createShaderModule(
                stack, "troll/graphics/grass.vert.spv", "GrassVertexShader"
        );
        var fragmentShader = troll.pipelines.createShaderModule(
                stack, "troll/graphics/grass.frag.spv", "GrassFragmentShader"
        );

        var vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack);
        vertexInput.sType$Default();
        vertexInput.pVertexBindingDescriptions(null);
        vertexInput.pVertexAttributeDescriptions(null);

        var ciPipelines = VkGraphicsPipelineCreateInfo.calloc(1, stack);
        var ciGrassPipeline = ciPipelines.get(0);
        ciGrassPipeline.sType$Default();
        troll.pipelines.shaderStages(
                stack, ciGrassPipeline,
                new ShaderInfo(VK_SHADER_STAGE_VERTEX_BIT, vertexShader, null),
                new ShaderInfo(VK_SHADER_STAGE_FRAGMENT_BIT, fragmentShader, null)
        );
        ciGrassPipeline.pVertexInputState(vertexInput);
        troll.pipelines.simpleInputAssembly(stack, ciGrassPipeline);
        troll.pipelines.dynamicViewports(stack, ciGrassPipeline, 1);
        troll.pipelines.simpleRasterization(stack, ciGrassPipeline, VK_CULL_MODE_BACK_BIT);
        troll.pipelines.noMultisampling(stack, ciGrassPipeline);
        troll.pipelines.simpleDepthStencil(stack, ciGrassPipeline, VK_COMPARE_OP_LESS_OR_EQUAL);
        troll.pipelines.noColorBlending(stack, ciGrassPipeline, 1);
        troll.pipelines.dynamicStates(stack, ciGrassPipeline, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR);
        ciGrassPipeline.layout(pipelineLayout);
        ciGrassPipeline.renderPass(renderPass);
        ciGrassPipeline.subpass(0);

        var pPipeline = stack.callocLong(1);
        assertVkSuccess(vkCreateGraphicsPipelines(
                troll.vkDevice(), VK_NULL_HANDLE, ciPipelines, null, pPipeline
        ), "CreateGraphicsPipelines", "GrassPipeline");
        long grassPipeline = pPipeline.get(0);
        troll.debug.name(stack, grassPipeline, VK_OBJECT_TYPE_PIPELINE, "GrassPipeline");

        vkDestroyShaderModule(troll.vkDevice(), vertexShader, null);
        vkDestroyShaderModule(troll.vkDevice(), fragmentShader, null);
        return grassPipeline;
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

    private static VmaImage[] createHeightImages(TrollInstance troll) {
        try (var stack = stackPush()){
            var input = TerrainPlayground.class.getClassLoader().getResourceAsStream("troll/height/N44E006.hgt");
            assert input != null;
            var content = input.readAllBytes();
            input.close();

            int numValues = content.length / 2;
            if (content.length % 2 != 0) throw new RuntimeException("Size is odd");
            int gridSize = (int) sqrt(numValues);
            if (gridSize * gridSize != numValues) throw new RuntimeException(numValues + " is not a square");

            ShortBuffer hostHeightBuffer = ByteBuffer.wrap(content).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
            ShortBuffer deltaHeightBuffer = ShortBuffer.allocate(hostHeightBuffer.capacity());

            var image = troll.images.createSimple(
                    stack, gridSize, gridSize, VK_FORMAT_R16_SINT, VK_SAMPLE_COUNT_1_BIT,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_ASPECT_COLOR_BIT, "HeightImage"
            );

            var normalImage = troll.images.createSimple(
                    stack, gridSize, gridSize, VK_FORMAT_R8G8B8A8_SNORM, VK_SAMPLE_COUNT_1_BIT,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT, VK_IMAGE_ASPECT_COLOR_BIT, "DeltaHeightImage"
            );

            var stagingBuffer = troll.buffers.createMapped(
                    content.length, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "HeightImageStagingBuffer"
            );
            var normalStagingBuffer = troll.buffers.createMapped(
                    4L * normalImage.width() * normalImage.height(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, "DeltaHeightImageStagingBuffer"
            );
            var stagingHostBuffer = memShortBuffer(stagingBuffer.hostAddress(), numValues);
            var normalHostBuffer = memByteBuffer(normalStagingBuffer.hostAddress(), (int) normalStagingBuffer.buffer().size());
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
            coarseHeightLookup = new HeightLookup(80, HEIGHT_IMAGE_NUM_PIXELS, hostHeightBuffer);
            fineHeightLookup = new HeightLookup(400, HEIGHT_IMAGE_NUM_PIXELS, hostHeightBuffer);

            for (int v = 0; v < gridSize; v++) {
                for (int u = 0; u < gridSize; u++) {
                    int index = u + v * gridSize;
                    if (u == gridSize - 1 || v == gridSize - 1) {
                        normalHostBuffer.put(4 * index, (byte) 0);
                        normalHostBuffer.put(4 * index + 1, (byte) 127);
                        normalHostBuffer.put(4 * index + 2, (byte) 0);
                        deltaHeightBuffer.put(index, (short) 0);
                    } else {
                        int heightIndex = u + v * gridSize;
                        int currentHeight = stagingHostBuffer.get(heightIndex);
                        int du = stagingHostBuffer.get(heightIndex + 1) - currentHeight;
                        int dv = stagingHostBuffer.get(heightIndex + gridSize) - currentHeight;

                        var vectorX = new Vector3f(30f, du, 0f);
                        var vectorZ = new Vector3f(0f, dv, 30f);
                        var normal = vectorZ.cross(vectorX).normalize();
                        normalHostBuffer.put(4 * index, (byte) (127 * normal.x));
                        normalHostBuffer.put(4 * index + 1, (byte) (127 * normal.y));
                        normalHostBuffer.put(4 * index + 2, (byte) (127 * normal.z));
                        deltaHeightBuffer.put(index, (short) max(abs(du), abs(dv)));
                    }
                }
            }

            coarseDeltaHeightLookup = new HeightLookup(600, HEIGHT_IMAGE_NUM_PIXELS, deltaHeightBuffer);

            troll.commands.begin(commandBuffer, stack, "CopyHeightImage");
            troll.commands.transitionColorLayout(
                    stack, commandBuffer, image.vkImage(), VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    null, new ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
            );
            troll.commands.transitionColorLayout(
                    stack, commandBuffer, normalImage.vkImage(), VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    null, new ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT)
            );
            troll.commands.copyBufferToImage(
                    commandBuffer, stack, VK_IMAGE_ASPECT_COLOR_BIT, image.vkImage(),
                    gridSize, gridSize, stagingBuffer.buffer().vkBuffer()
            );
            troll.commands.copyBufferToImage(
                    commandBuffer, stack, VK_IMAGE_ASPECT_COLOR_BIT, normalImage.vkImage(),
                    normalImage.width(), normalImage.height(), normalStagingBuffer.buffer().vkBuffer()
            );
            troll.commands.transitionColorLayout(
                    stack, commandBuffer, image.vkImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    new ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
                    new ResourceUsage(VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_VERTEX_SHADER_BIT)
            );
            troll.commands.transitionColorLayout(
                    stack, commandBuffer, normalImage.vkImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    new ResourceUsage(VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT),
                    new ResourceUsage(VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
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
            vmaDestroyBuffer(troll.vmaAllocator(), normalStagingBuffer.buffer().vkBuffer(), normalStagingBuffer.buffer().vmaAllocation());
            return new VmaImage[] { image, normalImage };
        } catch (IOException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var profiler = new PerformanceProfiler(new PerformanceStorage(), 1, name -> true);
        profiler.start();
        var troll = new TrollBuilder(
                VK_API_VERSION_1_2, "TerrainPlayground", VK_MAKE_VERSION(0, 1, 0)
        )
                //.validation(new ValidationFeatures(true, true, false, true, true))
                .window(0L, 1000, 800, new TrollSwapchainBuilder(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT))
                .build();

        var heightImages = createHeightImages(troll);
        var heightImage = heightImages[0];
        var normalImage = heightImages[1];
        var uniformBuffer = troll.buffers.createMapped(
                64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, "UniformBuffer"
        );
        long renderPass;
        long descriptorSetLayout;
        long groundPipelineLayout;
        long grassPipelineLayout;
        long groundPipeline;
        long grassPipeline;
        long descriptorPool;
        long descriptorSet;
        int depthFormat;
        long heightSampler;
        long normalSampler;
        try (var stack = stackPush()) {
            depthFormat = troll.images.chooseDepthStencilFormat(
                    stack, VK_FORMAT_X8_D24_UNORM_PACK32, VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT
            );
            renderPass = createRenderPass(stack, troll, depthFormat);

            descriptorSetLayout = createDescriptorSetLayout(stack, troll);
            groundPipelineLayout = createGroundPipelineLayout(stack, troll, descriptorSetLayout);
            grassPipelineLayout = createGrassPipelineLayout(stack, troll, descriptorSetLayout);
            groundPipeline = createGroundPipeline(stack, troll, groundPipelineLayout, renderPass);
            grassPipeline = createGrassPipeline(stack, troll, grassPipelineLayout, renderPass);

            var poolSizes = VkDescriptorPoolSize.calloc(3, stack);
            var uniformPoolSize = poolSizes.get(0);
            uniformPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformPoolSize.descriptorCount(1);
            var heightMapPoolSize = poolSizes.get(1);
            heightMapPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            heightMapPoolSize.descriptorCount(1);
            var normalMapPoolSize = poolSizes.get(2);
            normalMapPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            normalMapPoolSize.descriptorCount(2);

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
            normalSampler = troll.images.simpleSampler(
                    stack, VK_FILTER_LINEAR, VK_SAMPLER_MIPMAP_MODE_NEAREST, VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, "NormalSampler"
            );

            var heightMapInfo = VkDescriptorImageInfo.calloc(1, stack);
            heightMapInfo.sampler(heightSampler);
            heightMapInfo.imageView(heightImage.vkImageView());
            heightMapInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            var normalMapInfo = VkDescriptorImageInfo.calloc(1, stack);
            normalMapInfo.sampler(normalSampler);
            normalMapInfo.imageView(normalImage.vkImageView());
            normalMapInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            var descriptorWrites = VkWriteDescriptorSet.calloc(3, stack);
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
            var normalMapWrite = descriptorWrites.get(2);
            normalMapWrite.sType$Default();
            normalMapWrite.dstSet(descriptorSet);
            normalMapWrite.dstBinding(2);
            normalMapWrite.dstArrayElement(0);
            normalMapWrite.descriptorCount(1);
            normalMapWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            normalMapWrite.pImageInfo(normalMapInfo);

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
                camera.yaw += 0.5 * dx;
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

                float fieldOfView = 45f;
                float aspectRatio = (float) swapchainImage.width() / (float) swapchainImage.height();

                float nearPlane = 0.1f;
                float farPlane = 50_000f;
                var cameraMatrix = new Matrix4f()
                        .scale(1f, -1f, 1f)
                        .perspective(
                                (float) toRadians(fieldOfView), aspectRatio,
                                nearPlane, farPlane, true
                        )
                        .rotateX((float) toRadians(-camera.pitch))
                        .rotateY((float) toRadians(camera.yaw))
                        ;
                cameraMatrix.getToAddress(uniformBuffer.hostAddress());

                var frustumCuller = new FrustumCuller(
                        new Vector3f(), camera.yaw, camera.pitch, aspectRatio, fieldOfView, nearPlane, farPlane
                );

                var fragmentsToRender = new ArrayList<TerrainFragment>();
                float cameraU = 2f * camera.x / HEIGHT_IMAGE_SIZE + 0.5f;
                float cameraV = 2f * camera.z / HEIGHT_IMAGE_SIZE + 0.5f;
                partitionTerrainSpace(cameraU, cameraV, 0.0001f, 1.5f, 15, fragmentsToRender);
                fragmentsToRender.removeIf(fragment -> {
                    float minX = fragment.minX(cameraU);
                    float minZ = fragment.minZ(cameraV);
                    float maxX = fragment.maxX(cameraU);
                    float maxZ = fragment.maxZ(cameraV);

                    float threshold = 0.001f;
                    float fragmentSize = max(fragment.maxU - fragment.minU, fragment.maxV - fragment.minV);
                    var heightLookup = fragmentSize > threshold ? coarseHeightLookup : fineHeightLookup;
                    short[] heightBounds = heightLookup.getHeights(fragment.minU, fragment.minV, fragment.maxU, fragment.maxV);

                    var aabb = new FrustumCuller.AABB(minX, heightBounds[0] - camera.y, minZ, maxX, heightBounds[1] - camera.y, maxZ);
                    return frustumCuller.shouldCullAABB(aabb);
                });

                vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE);
                troll.commands.dynamicViewportAndScissor(stack, commandBuffer, swapchainImage.width(), swapchainImage.height());
                int vertexCount = 0;
                int drawCount = 0;

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, groundPipeline);
                vkCmdBindDescriptorSets(
                        commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, groundPipelineLayout,
                        0, stack.longs(descriptorSet), null
                );
                var pushConstants = stack.calloc(28);

                var divisorMap = new HashMap<Integer, Integer>();
                for (var fragment : fragmentsToRender) {
                    short maxDelta = coarseDeltaHeightLookup.getHeights(fragment.minU, fragment.minV, fragment.maxU, fragment.maxV)[1];
                    int divisor = 1;
                    if (maxDelta > 80) divisor = 2;
                    if (maxDelta > 110) divisor = 3;
                    if (maxDelta > 150) divisor = 4;
                    divisorMap.put(divisor, divisorMap.getOrDefault(divisor, 0) + 1);

                    pushConstants.putFloat(0, fragment.minX(cameraU));
                    pushConstants.putFloat(4, 0f - camera.y);
                    pushConstants.putFloat(8, fragment.minZ(cameraV));
                    pushConstants.putFloat(12, fragment.quadSize / divisor);
                    pushConstants.putFloat(16, fragment.minU);
                    pushConstants.putFloat(20, fragment.minV);
                    pushConstants.putInt(24, fragment.numColumns() * divisor);

                    vkCmdPushConstants(commandBuffer, groundPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants);
                    int numQuads = fragment.numRows() * fragment.numColumns() * divisor * divisor;
                    vkCmdDraw(commandBuffer, 6 * numQuads, 1, 0, 0);
                    vertexCount += 6 * numQuads;
                    drawCount += 1;
                }

                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, grassPipeline);
                vkCmdBindDescriptorSets(
                        commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, grassPipelineLayout,
                        0, stack.longs(descriptorSet), null
                );
                pushConstants = stack.calloc(28);

                int grassModelSize = 12; // Currently 12 vertices, TODO make it larger

                for (var fragment : fragmentsToRender) {
                    float minX = fragment.minX(cameraU);
                    float minZ = fragment.minZ(cameraV);
                    float maxX = fragment.maxX(cameraU);
                    float maxZ = fragment.maxZ(cameraV);

                    float distanceEstimation = (float) sqrt(min(min(minX * minX + minZ * minZ, minX * minX + maxZ * maxZ), min(maxX * maxX + minZ * minZ, maxX * maxX + maxZ * maxZ)));
                    if (distanceEstimation < 30) {
                        pushConstants.putFloat(0, minX);
                        pushConstants.putFloat(4, 0f - camera.y);
                        pushConstants.putFloat(8, minZ);
                        pushConstants.putFloat(12, (fragment.maxU - fragment.minU) * HEIGHT_IMAGE_SIZE);
                        pushConstants.putFloat(16, fragment.minU);
                        pushConstants.putFloat(20, fragment.minV);
                        pushConstants.putInt(24, 100);

                        int numGrassModels = 12000; // TODO Make this depend on the distance from the camera

                        vkCmdPushConstants(commandBuffer, grassPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants);
                        //vkCmdDraw(commandBuffer, numGrassModels * grassModelSize, 1, 0, 0);
                        vertexCount += numGrassModels * grassModelSize;
                        drawCount += 1;
                    }
                }

                vkCmdEndRenderPass(commandBuffer);
                if (Math.random() < 0.002) {
                    System.out.println("Drew " + vertexCount + " vertices with " + drawCount + " draw calls and divisors " + divisorMap);
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
        vkDestroyPipeline(troll.vkDevice(), grassPipeline, null);
        vkDestroyPipelineLayout(troll.vkDevice(), groundPipelineLayout, null);
        vkDestroyPipelineLayout(troll.vkDevice(), grassPipelineLayout, null);
        vkDestroyDescriptorSetLayout(troll.vkDevice(), descriptorSetLayout, null);
        vkDestroyRenderPass(troll.vkDevice(), renderPass, null);
        vmaDestroyBuffer(troll.vmaAllocator(), uniformBuffer.buffer().vkBuffer(), uniformBuffer.buffer().vmaAllocation());
        vkDestroyImageView(troll.vkDevice(), heightImage.vkImageView(), null);
        vmaDestroyImage(troll.vmaAllocator(), heightImage.vkImage(), heightImage.vmaAllocation());
        vkDestroyImageView(troll.vkDevice(), normalImage.vkImageView(), null);
        vmaDestroyImage(troll.vmaAllocator(), normalImage.vkImage(), normalImage.vmaAllocation());
        vkDestroySampler(troll.vkDevice(), heightSampler, null);
        vkDestroySampler(troll.vkDevice(), normalSampler, null);

        troll.destroy();
        profiler.stop();
        profiler.getStorage().dump(new File("terrain-playground-profiling.log"), 5, 0.0);
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

        float minX(float cameraU) {
            return (minU - cameraU) * HEIGHT_IMAGE_SIZE;
        }

        float minZ(float cameraV) {
            return (minV - cameraV) * HEIGHT_IMAGE_SIZE;
        }

        float maxX(float cameraU) {
            return (maxU - cameraU) * HEIGHT_IMAGE_SIZE;
        }

        float maxZ(float cameraV) {
            return (maxV - cameraV) * HEIGHT_IMAGE_SIZE;
        }
    }

    private static void addPartitionFragment(
            float cameraU, float cameraV, int dx, int dy,
            float fragmentSize, float quadSize, Collection<TerrainFragment> fragments, int exponent
    ) {
        float offset = exponent == 0 ? -0.5f : 0f;
        float minU = cameraU + (dx + offset) * fragmentSize;
        float minV = cameraV + (dy + offset) * fragmentSize;
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

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, quadSize, fragments, 0);
            }
        }

        int exponent = 1;
        while (exponent <= maxExponent) {
            double oldMinU = fragments.stream().mapToDouble(fragment -> fragment.minU).min().getAsDouble();

            quadSize *= 1.3f;
            fragmentSize *= 1.5f;

            int[] rowSizes = { 3, 4, 5, 6 };
            if (exponent == 1) rowSizes = new int[] { 2, 3 };
            if (exponent >= 3) rowSizes = new int[] { 5, 6 };

            for (int rowSize : rowSizes) {
                int dx = -rowSize;
                int dy = -rowSize;

                for (; dx < rowSize - 1; dx++) addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, quadSize, fragments, exponent);
                for (; dy < rowSize - 1; dy++) addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, quadSize, fragments, exponent);
                for (; dx > -rowSize; dx--) addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, quadSize, fragments, exponent);
                for (; dy > -rowSize; dy--) addPartitionFragment(cameraU, cameraV, dx, dy, fragmentSize, quadSize, fragments, exponent);
            }

            double minU = fragments.stream().mapToDouble(fragment -> fragment.minU).min().getAsDouble();
            double testU = minU + fragmentSize * rowSizes.length;
            double error = testU - oldMinU;

            if (abs(error) > 0.00001) {
                System.out.printf("%d: fragmentSize = %.5f and minU = %.5f and testU = %.5f -> error = %.5f\n", exponent, fragmentSize, minU, testU, error);
                throw new Error("Too large! abort");
            }

            exponent += 1;
        }
    }

    private static class HeightLookup {

        private final int size;

        private final short[] minHeights, maxHeights;

        HeightLookup(int size, int fullSize, ShortBuffer heights) {
            this.size = size;
            this.minHeights = new short[size * size];
            this.maxHeights = new short[size * size];

            Arrays.fill(minHeights, Short.MAX_VALUE);
            Arrays.fill(maxHeights, Short.MIN_VALUE);

            for (int v = 0; v < fullSize; v++) {
                int ownV = size * v / fullSize;
                for (int u = 0; u < fullSize; u++) {
                    int ownU = size * u / fullSize;
                    int fullIndex = u + fullSize * v;
                    int ownIndex = ownU + size * ownV;
                    minHeights[ownIndex] = (short) min(minHeights[ownIndex], heights.get(fullIndex));
                    maxHeights[ownIndex] = (short) max(maxHeights[ownIndex], heights.get(fullIndex));
                }
            }
        }

        short[] getHeights(float minU, float minV, float maxU, float maxV) {

            int minIntU = (int) (minU * size) - 1;
            int minIntV = (int) (minV * size) - 1;
            int maxIntU = (int) ceil(maxU * size) + 1;
            int maxIntV = (int) ceil(maxV * size) + 1;
            if (minIntU < 0) minIntU = 0;
            if (minIntV < 0) minIntV = 0;
            if (maxIntU >= size) maxIntU = size - 1;
            if (maxIntV >= size) maxIntV = size - 1;
            short minHeight = Short.MAX_VALUE;
            short maxHeight = Short.MIN_VALUE;

            for (int intU = minIntU; intU <= maxIntU; intU++) {
                for (int intV = minIntV; intV <= maxIntV; intV++) {
                    minHeight = (short) min(minHeight, minHeights[intV * size + intU]);
                    maxHeight = (short) max(maxHeight, maxHeights[intV * size + intU]);
                }
            }

            return new short[] { minHeight, maxHeight };
        }
    }
}
