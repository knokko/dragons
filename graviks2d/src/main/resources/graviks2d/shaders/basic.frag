#version 450

layout(location = 0) in flat int operationIndex;

layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 0) readonly buffer ShaderStorage {
    int operations[];
} shaderStorage;

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

void main() {
    int operationCode = shaderStorage.operations[operationIndex];

    if (operationCode == OP_CODE_FILL_RECT) {
        vec4 fillColor = decodeColor(shaderStorage.operations[operationIndex + 1]);
        outColor = fillColor;
    } else {
        // This is the 'unknown operation code' color, for the sake of debugging
        outColor = vec4(1.0, 0.2, 0.6, 1.0);
    }
}
