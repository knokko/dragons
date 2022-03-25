#version 450

layout(location = 0) in vec2 inPosition;
layout(location = 1) in int inDepth;
layout(location = 2) in int inOperationIndex;

layout(location = 0) out int outOperationIndex;
layout(location = 1) out vec2 quadCoordinates;

layout(set = 0, binding = 0) readonly buffer ShaderStorage {
    int operations[];
} shaderStorage;

layout(push_constant) uniform PushConstants {
    int maxDepth;
} pushConstants;

const int OP_CODE_FILL_RECT = 1;
const int OP_CODE_DRAW_IMAGE_BOTTOM_LEFT = 2;
const int OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT = 3;
const int OP_CODE_DRAW_IMAGE_TOP_RIGHT = 4;
const int OP_CODE_DRAW_IMAGE_TOP_LEFT = 5;

void main() {
    float z = 0.99 - 0.98 * (float(inDepth) / float(pushConstants.maxDepth));
    gl_Position = vec4(inPosition.x * 2.0 - 1.0, inPosition.y * -2.0 + 1.0, z, 1.0);

    int operationCode = shaderStorage.operations[inOperationIndex];

    // Some operation codes exist only in the vertex shader and need special treatment
    if (operationCode == OP_CODE_DRAW_IMAGE_BOTTOM_RIGHT) {
        outOperationIndex = inOperationIndex - 2;
        quadCoordinates = vec2(1.0, 0.0);
    } else if (operationCode == OP_CODE_DRAW_IMAGE_TOP_RIGHT) {
        outOperationIndex = inOperationIndex - 3;
        quadCoordinates = vec2(1.0, 1.0);
    } else if (operationCode == OP_CODE_DRAW_IMAGE_TOP_LEFT) {
        outOperationIndex = inOperationIndex - 4;
        quadCoordinates = vec2(0.0, 1.0);
    } else {
        outOperationIndex = inOperationIndex;
        quadCoordinates = vec2(0.0, 0.0);
    }
}
