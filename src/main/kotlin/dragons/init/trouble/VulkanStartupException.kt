package dragons.init.trouble

import dragons.vulkan.util.VulkanFailureException

class VulkanStartupException(failure: VulkanFailureException): SimpleStartupException(
    "A Vulkan function call returned an error result code",
    listOf(failure.message!!, "This is probably a programming error")
)
