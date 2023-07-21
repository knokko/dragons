#version 450

layout(location = 0) out vec3 passPosition;
layout(location = 1) out vec3 deltaHeight;

layout(push_constant) uniform PushConstants {
    vec3 base;
    float quadSize;
    vec2 textureOffset;
    float heightScale;
    float realTextureSize;
    int numColumns;
} pushConstants;

layout(set = 0, binding = 0) uniform Camera {
    mat4 matrix;
} camera;
layout(set = 0, binding = 1) uniform isampler2D heightMap;

float bezier(float t, float p0, float p1, float p2, float p3) {
    return (1 - t) * (1 - t) * (1 - t) * p0 + 3 * (1 - t) * (1 - t) * t * p1 + 3 * (1 - t) * t * t * p2 + t * t * t * p3;
}

float deltaBezier(float t, float p0, float p1, float p2, float p3) {
    return 3 * (1 - t) * (1 - t) * (p1 - p0) + 6 * (1 - t) * t * (p2 - p1) + 3 * t * t * (p3 - p2);
}

float applyBezier(float t, float height1, float height2, float height3, float height4) {
    float d2 = (height3 - height1) / 60.0;
    float d3 = (height4 - height2) / 60.0;
    float control2 = height2 + 10.0 * d2;
    float control3 = height3 - 10.0 * d3;

    return bezier(t, height2, control2, control3, height3);
}

float applyDeltaBezier(float t, float height1, float height2, float height3, float height4) {
    float d2 = (height3 - height1) / 60.0;
    float d3 = (height4 - height2) / 60.0;
    float control2 = height2 + 10.0 * d2;
    float control3 = height3 - 10.0 * d3;

    return deltaBezier(t, height2, control2, control3, height3);
}

void main() {
    int quadIndex = gl_VertexIndex / 6;
    int cornerIndex = gl_VertexIndex % 6;
    int column = quadIndex % pushConstants.numColumns;
    int row = quadIndex / pushConstants.numColumns;

    if (cornerIndex > 0 && cornerIndex < 4) column += 1;
    if (cornerIndex < 2 || cornerIndex == 5) row += 1;

    float realOffsetX = column * pushConstants.quadSize;
    float realOffsetZ = row * pushConstants.quadSize;

    vec2 textureCoordinates = pushConstants.textureOffset + vec2(
        realOffsetX / pushConstants.realTextureSize, realOffsetZ / pushConstants.realTextureSize
    );

    int u2 = int(textureCoordinates.x * 3601);
    float ut = textureCoordinates.x * 3601 - u2;
    int u1 = u2 - 1;
    if (u1 < 0) u1 = 0;
    int u3 = u2 + 1;
    int u4 = u3 + 1;
    if (u3 >= 3601) u3 = 3601 - 1;
    if (u4 >= 3601) u4 = 3601 - 1;

    int v2 = int(textureCoordinates.y * 3601);
    float vt = textureCoordinates.y * 3601 - v2;
    int v1 = v2 - 1;
    if (v1 < 0) v1 = 0;
    int v3 = v2 + 1;
    int v4 = v3 + 1;
    if (v3 >= 3601) v3 = 3601 - 1;
    if (v4 >= 3601) v4 = 3601 - 1;

    float height11 = texture(heightMap, ivec2(u1, v1)).r;
    float height12 = texture(heightMap, ivec2(u1, v2)).r;
    float height13 = texture(heightMap, ivec2(u1, v3)).r;
    float height14 = texture(heightMap, ivec2(u1, v4)).r;

    float height21 = texture(heightMap, ivec2(u2, v1)).r;
    float height22 = texture(heightMap, ivec2(u2, v2)).r;
    float height23 = texture(heightMap, ivec2(u2, v3)).r;
    float height24 = texture(heightMap, ivec2(u2, v4)).r;

    float height31 = texture(heightMap, ivec2(u3, v1)).r;
    float height32 = texture(heightMap, ivec2(u3, v2)).r;
    float height33 = texture(heightMap, ivec2(u3, v3)).r;
    float height34 = texture(heightMap, ivec2(u3, v4)).r;

    float height41 = texture(heightMap, ivec2(u4, v1)).r;
    float height42 = texture(heightMap, ivec2(u4, v2)).r;
    float height43 = texture(heightMap, ivec2(u4, v3)).r;
    float height44 = texture(heightMap, ivec2(u4, v4)).r;

    float heightU1 = applyBezier(vt, height11, height12, height13, height14);
    float heightU2 = applyBezier(vt, height21, height22, height23, height24);
    float heightU3 = applyBezier(vt, height31, height32, height33, height34);
    float heightU4 = applyBezier(vt, height41, height42, height43, height44);

    float heightV1 = applyBezier(ut, height11, height21, height31, height41);
    float heightV2 = applyBezier(ut, height12, height22, height32, height42);
    float heightV3 = applyBezier(ut, height13, height23, height33, height43);
    float heightV4 = applyBezier(ut, height14, height24, height34, height44);

    float heightU = applyBezier(ut, heightU1, heightU2, heightU3, heightU4);
    // don't bother computing heightV because it's identical to heightU (aside from FP rounding errors)

    float deltaX = applyDeltaBezier(ut, heightU1, heightU2, heightU3, heightU4);
    float deltaZ = applyDeltaBezier(vt, heightV1, heightV2, heightV3, heightV4);
    deltaX /= 30.0;
    deltaZ /= 30.0;

    float realOffsetY = pushConstants.heightScale * heightU;
    vec3 worldPosition = pushConstants.base + vec3(realOffsetX, realOffsetY, realOffsetZ);

    passPosition = worldPosition;
    gl_Position = camera.matrix * vec4(worldPosition, 1.0);

    deltaHeight = vec3(deltaX, 0.0, deltaZ);
}
