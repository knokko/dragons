package dragons.vr.openxr

import dragons.vr.assertXrSuccess
import org.lwjgl.openxr.XR10.XR_TYPE_API_LAYER_PROPERTIES
import org.lwjgl.openxr.XR10.xrEnumerateApiLayerProperties
import org.lwjgl.openxr.XrApiLayerProperties
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memPutInt
import org.slf4j.Logger

internal fun getAvailableOpenXrLayers(logger: Logger): Set<String> {
    return stackPush().use { stack ->

        val pNumLayers = stack.callocInt(1)
        assertXrSuccess(
            xrEnumerateApiLayerProperties(pNumLayers, null),
            "EnumerateApiLayerProperties", "count"
        )
        val numLayers = pNumLayers[0]

        val pLayers = XrApiLayerProperties.calloc(numLayers, stack)
        for (index in 0 until numLayers) {
            memPutInt(pLayers[index].address() + XrApiLayerProperties.TYPE, XR_TYPE_API_LAYER_PROPERTIES)
        }
        assertXrSuccess(
            xrEnumerateApiLayerProperties(pNumLayers, pLayers),
            "EnumerateApiLayerProperties", "layers"
        )

        val layers = mutableSetOf<String>()
        for (index in 0 until numLayers) {
            layers.add(pLayers[index].layerNameString())
        }

        logger.info("${layers.size} OpenXR layers are available:")
        for (layer in layers) {
            logger.info(layer)
        }
        layers
    }
}
