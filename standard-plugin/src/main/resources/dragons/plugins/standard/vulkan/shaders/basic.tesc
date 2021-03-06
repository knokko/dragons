#version 440
layout(vertices = 3) out;

layout(location = 0) in vec3 inBasePosition[];
layout(location = 1) in vec3 inBaseNormal[];
layout(location = 2) in vec2 inColorTexCoordinates[];
layout(location = 3) in vec2 inHeightTexCoordinates[];
layout(location = 4) in int inMatrixIndex[];
layout(location = 5) in int inMaterialIndex[];
layout(location = 6) in vec2 inDeltaFactor[];
layout(location = 7) in int inColorTextureIndex[];
layout(location = 8) in int inHeightTextureIndex[];

layout(location = 0) out vec3 outBasePosition[];
layout(location = 1) out vec3 outBaseNormal[];
layout(location = 2) out vec2 outColorTexCoordinates[];
layout(location = 3) out vec2 outHeightTexCoordinates[];
layout(location = 4) out int outMatrixIndex[];
layout(location = 5) out int outMaterialIndex[];
layout(location = 6) out vec2 outDeltaFactor[];
layout(location = 7) out int outColorTextureIndex[];
layout(location = 8) out int outHeightTextureIndex[];

void main() {
    outBasePosition[gl_InvocationID] = inBasePosition[gl_InvocationID];
    outBaseNormal[gl_InvocationID] = inBaseNormal[gl_InvocationID];
    outColorTexCoordinates[gl_InvocationID] = inColorTexCoordinates[gl_InvocationID];
    outHeightTexCoordinates[gl_InvocationID] = inHeightTexCoordinates[gl_InvocationID];
    outMatrixIndex[gl_InvocationID] = inMatrixIndex[gl_InvocationID];
    outMaterialIndex[gl_InvocationID] = inMaterialIndex[gl_InvocationID];
    outDeltaFactor[gl_InvocationID] = inDeltaFactor[gl_InvocationID];
    outColorTextureIndex[gl_InvocationID] = inColorTextureIndex[gl_InvocationID];
    outHeightTextureIndex[gl_InvocationID] = inHeightTextureIndex[gl_InvocationID];

    // This only needs to happen once per patch
    if (gl_InvocationID == 0) {
        // TODO Stop hardcoding this

        // Future note: since the average eye position is used as the origin of the scene, the distance to the camera
        // is just the 'length' of the world position.
        gl_TessLevelInner[0] = 10;

        gl_TessLevelOuter[0] = 10;
        gl_TessLevelOuter[1] = 10;
        gl_TessLevelOuter[2] = 10;
    }
}
