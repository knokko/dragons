#version 450

layout(push_constant) uniform PushConstants {
    vec3 base;
    float regionSize;
    vec2 textureOffset;
    int discreteRegionSize;
} pushConstants;

layout(location = 0) out vec2 outNormal;
layout(location = 1) out float outHeightFraction;

layout(set = 0, binding = 0) uniform Camera {
    mat4 matrix;
} camera;
layout(set = 0, binding = 1) uniform isampler2D heightMap;

#include "height.glsl"

int simpleHash(int i) {
    int x = 12 + 34 * i - 56 * i * i + 78 * i * i * i - 9 * i * i * i * i;
    x += ( x << 10 );
    x ^= ( x >>  6 );
    x += ( x <<  3 );
    x ^= ( x >> 11 );
    x += ( x << 15 );
    return x;
}

void main() {
    int grassModelSize = 12;
    int grassIndex = gl_VertexIndex / grassModelSize;
    int modelIndex = gl_VertexIndex % grassModelSize;

    uint grassHash = uint(simpleHash(grassIndex));
    int discreteRegionSize = pushConstants.discreteRegionSize;
    float centerOffsetX = pushConstants.regionSize * (grassHash % discreteRegionSize) / discreteRegionSize;
    grassHash = uint(simpleHash(int(grassHash)));
    float centerOffsetZ = pushConstants.regionSize * (grassHash % discreteRegionSize) / discreteRegionSize;

    vec3 heightAndDelta = computeHeightAndDeltaXZ(
        vec2(centerOffsetX, centerOffsetZ), pushConstants.textureOffset, heightMap, false
    );

    float offsetX = centerOffsetX;
    float offsetY = heightAndDelta.y;
    float offsetZ = centerOffsetZ;

    float radius = 0.01;
    if (modelIndex == 0 || modelIndex == 10) {
        offsetX -= radius;
        offsetZ += radius;
    }
    if (modelIndex == 1 || modelIndex == 3) {
        offsetX += radius;
        offsetZ += radius;
    }
    if (modelIndex == 4 || modelIndex == 6) {
        offsetX += radius;
        offsetZ -= radius;
    }
    if (modelIndex == 7 || modelIndex == 9) {
        offsetX -= radius;
        offsetZ -= radius;
    }
    if (modelIndex % 3 == 2) {
        offsetY += 0.3;
        outHeightFraction = 1.0;
    } else outHeightFraction = 0.0;

    if (modelIndex == 0 || modelIndex == 1 || modelIndex == 2) outNormal = vec2(0.0, 1.0);
    if (modelIndex == 3 || modelIndex == 4 || modelIndex == 5) outNormal = vec2(1.0, 0.0);
    if (modelIndex == 6 || modelIndex == 7 || modelIndex == 8) outNormal = vec2(0.0, -1.0);
    if (modelIndex == 9 || modelIndex == 10 || modelIndex == 11) outNormal = vec2(-1.0, 0.0);

    vec3 worldPosition = pushConstants.base + vec3(offsetX, offsetY, offsetZ);
    gl_Position = camera.matrix * vec4(worldPosition, 1.0);
}
