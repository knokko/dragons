#version 450

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec3 inColor;

layout(push_constant) uniform PushConstants {
    mat3x2 cameraMatrix;
} pushConstants;

layout(location = 0) out vec3 outColor;

void main() {
    vec2 worldPosition = pushConstants.cameraMatrix * vec3(inPosition, 1.0);
    gl_Position = vec4(worldPosition.x, worldPosition.y, 0.5, 1.0);
    outColor = inColor;
}
