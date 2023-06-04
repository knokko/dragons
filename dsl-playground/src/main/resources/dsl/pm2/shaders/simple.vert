#version 450

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in int matrixIndex;

layout(push_constant) uniform PushConstants {
    mat3x2 cameraMatrix;
    int matrixIndexOffset;
} pushConstants;

layout(binding = 0) uniform DynamicMatrices {
    mat3x2 matrices[350];
} dynamicMatrices;

layout(location = 0) out vec3 outColor;

void main() {
    mat3x2 transformationMatrix = dynamicMatrices.matrices[pushConstants.matrixIndexOffset + matrixIndex];
    vec2 worldPosition = transformationMatrix * vec3(inPosition, 1.0);
    vec2 screenPosition = pushConstants.cameraMatrix * vec3(worldPosition, 1.0);
    gl_Position = vec4(screenPosition.x, screenPosition.y, 0.5, 1.0);
    outColor = inColor;
}
