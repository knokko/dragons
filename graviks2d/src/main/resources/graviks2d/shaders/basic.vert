#version 450

layout(location = 0) in vec2 inPosition;
layout(location = 1) in int inDepth;
layout(location = 2) in int inOperationIndex;

layout(location = 0) out int outOperationIndex;

layout(push_constant) uniform PushConstants {
    int maxDepth;
} pushConstants;

void main() {
    float z = 0.1 + 0.8 * (float(inDepth) / float(pushConstants.maxDepth));
    gl_Position = vec4(inPosition.x * 2.0 - 1.0, inPosition.y * 2.0 - 1.0, z, 1.0);

    outOperationIndex = inOperationIndex;
}
