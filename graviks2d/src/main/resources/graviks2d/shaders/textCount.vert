#version 450

layout(location = 0) in vec2 inPosition;
layout(location = 1) in int inOperation;

layout(location = 0) out int outOperation;
layout(location = 1) out vec3 outBarycentricCoordinates;

const int OPERATION_CORRECT_START = 2;
const int OPERATION_CORRECT_CONTROL = 3;
const int OPERATION_CORRECT_END = 4;

void main() {
    gl_Position = vec4(inPosition.x * 2.0 - 1.0, inPosition.y * -2.0 + 1.0, 0.5, 1.0);

    outOperation = inOperation;
    if (inOperation == OPERATION_CORRECT_START) {
        outBarycentricCoordinates = vec3(1.0, 0.0, 0.0);
    } else if (inOperation == OPERATION_CORRECT_CONTROL) {
        outBarycentricCoordinates = vec3(0.0, 1.0, 0.0);
    } else if (inOperation == OPERATION_CORRECT_END) {
        outBarycentricCoordinates = vec3(0.0, 0.0, 1.0);
    } else {
        // The barycentric coordinates are only needed for the CORRECT operation
        outBarycentricCoordinates = vec3(0.0, 0.0, 0.0);
    }
}
