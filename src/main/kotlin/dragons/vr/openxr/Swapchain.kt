package dragons.vr.openxr

import dragons.init.trouble.SimpleStartupException
import dragons.state.StaticGraphicsState
import dragons.vulkan.memory.VulkanImage
import dragons.vulkan.util.assertVkSuccess
import org.lwjgl.openxr.*
import org.lwjgl.openxr.XR10.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageViewCreateInfo
import kotlin.math.min

internal fun determineOpenXrSwapchainSize(
    xrInstance: XrInstance, xrSystemId: Long
): Pair<Int, Int> {
    return stackPush().use { stack ->
        val pNumViewConfigurations = stack.callocInt(1)
        assertXrSuccess(
            xrEnumerateViewConfigurations(xrInstance, xrSystemId, pNumViewConfigurations, null),
            "EnumerateViewConfigurations", "count"
        )
        val numViewConfigurations = pNumViewConfigurations[0]

        val pViewConfigurations = stack.callocInt(numViewConfigurations)
        assertXrSuccess(
            xrEnumerateViewConfigurations(xrInstance, xrSystemId, pNumViewConfigurations, pViewConfigurations),
            "EnumerateViewConfigurations", "configurations"
        )

        var hasStereo = false
        for (index in 0 until numViewConfigurations) {
            if (pViewConfigurations[index] == XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO) {
                hasStereo = true
            }
        }

        if (!hasStereo) {
            throw UnsupportedOperationException("XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO is required, but not supported")
        }

        val pNumViews = stack.callocInt(1)
        assertXrSuccess(
            xrEnumerateViewConfigurationViews(xrInstance, xrSystemId, XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO, pNumViews, null),
            "EnumerateViewConfigurationViews", "count"
        )
        val numViews = pNumViews[0]

        if (numViews != 2) {
            throw UnsupportedOperationException("Number of view configuration views ($numViews) must be 2")
        }

        val pViews = XrViewConfigurationView.calloc(numViews, stack)
        for (viewIndex in 0 until numViews) {
            pViews[viewIndex].`type$Default`()
        }
        assertXrSuccess(
            xrEnumerateViewConfigurationViews(xrInstance, xrSystemId, XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO, pNumViews, pViews),
            "EnumerateViewConfigurationViews", "views"
        )

        val width = min(pViews[0].recommendedImageRectWidth(), pViews[1].recommendedImageRectWidth())
        val height = min(pViews[0].recommendedImageRectHeight(), pViews[1].recommendedImageRectHeight())
        Pair(width, height)
    }
}

private fun chooseSwapchainFormat(xrSession: XrSession, graphicsState: StaticGraphicsState): Int {
    stackPush().use { stack ->

        val pNumFormats = stack.callocInt(1)
        assertXrSuccess(
            xrEnumerateSwapchainFormats(xrSession, pNumFormats, null),
            "EnumerateSwapchainFormats", "count"
        )
        val numFormats = pNumFormats[0]

        val pFormats = stack.callocLong(numFormats)
        assertXrSuccess(
            xrEnumerateSwapchainFormats(xrSession, pNumFormats, pFormats),
            "EnumerateSwapchainFormats"
        )

        for (index in 0 until numFormats) {
            if (pFormats[index] == graphicsState.renderImageInfo.colorFormat.toLong()) {
                return graphicsState.renderImageInfo.colorFormat
            }
        }

        // TODO Maybe support more swapchain formats
        throw SimpleStartupException(
            "XrSwapchainFormat not supported",
            listOf(
                "The OpenXR swapchain format ${graphicsState.renderImageInfo.colorFormat} is required,",
                "but not supported by the OpenXR runtime."
            )
        )
    }
}

internal fun createOpenXrSwapchains(
    xrSession: XrSession, graphicsState: StaticGraphicsState, width: Int, height: Int
): List<OpenXrSwapchain> {
    return stackPush().use { stack ->

        val swapchainColorFormat = chooseSwapchainFormat(xrSession, graphicsState)
        (0 until 2).map {
            val ciSwapchain = XrSwapchainCreateInfo.calloc(stack)
            ciSwapchain.`type$Default`()
            // The transfer destination usage is required because it will be used as destination for vkCmdResolveImage
            ciSwapchain.usageFlags(XR_SWAPCHAIN_USAGE_TRANSFER_DST_BIT.toLong())
            ciSwapchain.format(swapchainColorFormat.toLong())
            ciSwapchain.sampleCount(1)
            ciSwapchain.width(width)
            ciSwapchain.height(height)
            ciSwapchain.faceCount(1)
            ciSwapchain.arraySize(1)
            ciSwapchain.mipCount(1)

            val pSwapchain = stack.callocPointer(1)
            assertXrSuccess(xrCreateSwapchain(xrSession, ciSwapchain, pSwapchain), "CreateSwapchain")
            val xrSwapchain = XrSwapchain(pSwapchain[0], xrSession)

            val pNumImages = stack.callocInt(1)
            assertXrSuccess(
                xrEnumerateSwapchainImages(xrSwapchain, pNumImages, null),
                "EnumerateSwapchainImages", "count"
            )
            val numImages = pNumImages[0]

            val pImages = XrSwapchainImageVulkan2KHR.calloc(numImages, stack)
            for (index in 0 until numImages) {
                pImages[index].`type$Default`()
            }
            val rawImages = XrSwapchainImageBaseHeader.create(pImages.address(), numImages)
            assertXrSuccess(
                xrEnumerateSwapchainImages(xrSwapchain, pNumImages, rawImages),
                "EnumerateSwapchainImages", "images"
            )

            val ciImageView = VkImageViewCreateInfo.calloc(stack)
            ciImageView.`sType$Default`()
            // The `image` will be changed in each iteration of the next loop
            ciImageView.viewType(VK_IMAGE_VIEW_TYPE_2D)
            ciImageView.format(swapchainColorFormat)
            ciImageView.components { components ->
                components.r(VK_COMPONENT_SWIZZLE_IDENTITY)
                components.g(VK_COMPONENT_SWIZZLE_IDENTITY)
                components.b(VK_COMPONENT_SWIZZLE_IDENTITY)
                components.a(VK_COMPONENT_SWIZZLE_IDENTITY)
            }
            ciImageView.subresourceRange { subresourceRange ->
                subresourceRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                subresourceRange.baseMipLevel(0)
                subresourceRange.levelCount(1)
                subresourceRange.baseArrayLayer(0)
                subresourceRange.layerCount(1)
            }

            val pImageView = stack.callocLong(1)
            val images = (0 until numImages).map { imageIndex ->
                val vulkanImage = VulkanImage(pImages[imageIndex].image(), width, height)

                ciImageView.image(vulkanImage.handle)
                assertVkSuccess(
                    vkCreateImageView(graphicsState.vkDevice, ciImageView, null, pImageView),
                    "CreateImageView", "OpenXR swapchain image"
                )
                vulkanImage.fullView = pImageView[0]
                vulkanImage
            }

            OpenXrSwapchain(xrSwapchain, images)
        }
    }
}

internal class OpenXrSwapchain(
    val handle: XrSwapchain,
    val images: List<VulkanImage>
)
