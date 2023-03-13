#version 450

layout(location = 0) in vec2 inPosition;
layout(location = 1) in int inOperationIndex;

layout(location = 0) out int outOperationIndex;
layout(location = 1) out vec2 outQuadCoordinates;
layout(location = 2) out vec4 outTextColor;
layout(location = 3) out vec4 outBackgroundColor;
layout(location = 4) out vec4 outStrokeColor;
layout(location = 5) out vec2 outStrokeDelta;

layout(set = 0, binding = 0) readonly buffer ShaderStorage {
    int operations[];
} shaderStorage;

const int OP_CODE_FILL_RECT = 1;
const int OP_CODE_DRAW_IMAGE_BOTTOM_LEFT = 2;
const int OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT = 3;
const int OP_CODE_DRAW_IMAGE_TOP_RIGHT = 4;
const int OP_CODE_DRAW_IMAGE_TOP_LEFT = 5;
const int OP_CODE_DRAW_TEXT = 6;
const int OP_CODE_DRAW_ROUNDED_RECT = 7;

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

const vec4 ERROR_TEXT_COLOR = vec4(0.8, 0.6, 0.4, 1.0);

float decodeFloat(int rawValue) {
    return float(rawValue) * 0.000001;
}

void main() {
    gl_Position = vec4(inPosition.x * 2.0 - 1.0, inPosition.y * -2.0 + 1.0, 0.0, 1.0);

    int operationCode = shaderStorage.operations[inOperationIndex];

    // Some operation codes exist only in the vertex shader and/or need special treatment
    if (operationCode == OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT) {
        outOperationIndex = inOperationIndex - 2;
        outQuadCoordinates = vec2(1.0, 0.0);
    } else if (operationCode == OP_CODE_DRAW_IMAGE_TOP_RIGHT) {
        outOperationIndex = inOperationIndex - 3;
        outQuadCoordinates = vec2(1.0, 1.0);
    } else if (operationCode == OP_CODE_DRAW_IMAGE_TOP_LEFT) {
        outOperationIndex = inOperationIndex - 4;
        outQuadCoordinates = vec2(0.0, 1.0);
    } else if (operationCode == OP_CODE_DRAW_TEXT) {
        outOperationIndex = inOperationIndex;
        outQuadCoordinates = vec2(
            decodeFloat(shaderStorage.operations[inOperationIndex + 1]),
            decodeFloat(shaderStorage.operations[inOperationIndex + 2])
        );
    } else if (operationCode == OP_CODE_DRAW_ROUNDED_RECT) {
        outOperationIndex = inOperationIndex;
        outQuadCoordinates = inPosition;
    } else {
        outOperationIndex = inOperationIndex;
        outQuadCoordinates = vec2(0.0, 0.0);
    }

    if (operationCode == OP_CODE_DRAW_TEXT) {
        outTextColor = decodeColor(shaderStorage.operations[inOperationIndex + 3]);
        outBackgroundColor = decodeColor(shaderStorage.operations[inOperationIndex + 4]);
        outStrokeColor = decodeColor(shaderStorage.operations[inOperationIndex + 5]);
        outStrokeDelta = vec2(
            decodeFloat(shaderStorage.operations[inOperationIndex + 6]),
            decodeFloat(shaderStorage.operations[inOperationIndex + 7])
        );
    } else {
        // The error text color will be displayed when the fragment shader tries to
        // use the text color outside a text drawing operation.
        outTextColor = ERROR_TEXT_COLOR;
        outBackgroundColor = ERROR_TEXT_COLOR;
    }
}
