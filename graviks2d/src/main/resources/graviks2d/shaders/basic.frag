#version 450

layout(location = 0) in flat int operationIndex;
layout(location = 1) in vec2 quadCoordinates;
layout(location = 2) in vec4 textColor;
layout(location = 3) in vec4 backgroundColor;

layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 0) readonly buffer ShaderStorage {
    int operations[];
} shaderStorage;
layout(set = 0, binding = 1) uniform sampler textureSampler;
layout(set = 0, binding = 2) uniform texture2D textures[100]; // TODO Avoid hardcoding this
layout(set = 0, binding = 3) uniform texture2D textAtlasTexture;

float decodeColorComponent(int rawValue) {
    return float(rawValue & 255) / 255.0;
}

vec4 decodeColor(int rawColor) {
    return vec4(
        decodeColorComponent(rawColor),
        decodeColorComponent(rawColor >> 8),
        decodeColorComponent(rawColor >> 16),
        decodeColorComponent(rawColor >> 24)
    );
}

const int OP_CODE_FILL_RECT = 1;
const int OP_CODE_DRAW_IMAGE = 2;
const int OP_CODE_DRAW_TEXT = 6;

void main() {
    int operationCode = shaderStorage.operations[operationIndex];

    if (operationCode == OP_CODE_FILL_RECT) {
        vec4 fillColor = decodeColor(shaderStorage.operations[operationIndex + 1]);
        outColor = fillColor;
    } else if (operationCode == OP_CODE_DRAW_IMAGE) {
        int imageIndex = shaderStorage.operations[operationIndex + 1];
        vec2 textureCoordinates = vec2(quadCoordinates.x, 1.0 - quadCoordinates.y);
        outColor = texture(sampler2D(textures[imageIndex], textureSampler), textureCoordinates);
    } else if (operationCode == OP_CODE_DRAW_TEXT) {
        vec2 textureCoordinates = vec2(quadCoordinates.x, 1.0 - quadCoordinates.y);

        // TODO This needs rework if I decide to implement anti-aliasing!
        float rawDensity = texture(sampler2D(textAtlasTexture, textureSampler), textureCoordinates).r;
        int numFlips = int(rawDensity * 255.0 + 0.5);
        if (numFlips % 2 == 0) {
            outColor = backgroundColor;
        } else {
            outColor = textColor;
        }
    } else {
        // This is the 'unknown operation code' color, for the sake of debugging
        outColor = vec4(1.0, 0.2, 0.6, 1.0);
    }
}
