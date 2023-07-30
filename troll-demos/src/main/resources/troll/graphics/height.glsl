#include "bezier.glsl"

vec3 computeHeightAndDeltaXZ(vec2 realOffset, vec2 textureOffset, isampler2D heightMap, bool simplify) {
    vec2 textureCoordinates = textureOffset + realOffset / 108030.0;

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

    if (simplify) {
        //heightU = (1.0 - ut) * ((1.0 - vt) * height22 + vt * height23) + ut * ((1.0 - vt) * height32 + vt * height33);
//        deltaX = (1.0 - vt) * (height32 - height22) + vt * (height33 - height23);
//        deltaX /= 90.0;
//        deltaZ = (1.0 - ut) * (height23 - height22) + ut * (height33 - height32);
//        deltaZ /= 90.0;j
        deltaX = (heightU2 - heightU1) / 30.0;
        deltaZ = (heightV2 - heightV1) / 30.0;
//        float stepSize = 0.001;
//        deltaZ = applyBezier(vt + stepSize, heightV1, heightV2, heightV3, heightV4) - applyBezier(vt - stepSize, heightV1, heightV2, heightV3, heightV4);
//        deltaZ /= 30 * 2 * stepSize;
//        deltaX = applyBezier(ut + stepSize, heightU1, heightU2, heightU3, heightU4) - applyBezier(ut - stepSize, heightU1, heightU2, heightU3, heightU4);
//        deltaX /= 30 * 2 * stepSize;
    }

    return vec3(deltaX, heightU, deltaZ);
}