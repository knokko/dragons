package dragons.init.trouble

import com.github.knokko.boiler.exceptions.VulkanFailureException

class VulkanStartupException(failure: VulkanFailureException): SimpleStartupException(
    "A Vulkan function call returned an error result code",
    listOf(failure.message!!, "This is probably a programming error")
)
