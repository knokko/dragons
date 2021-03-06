#version 440
// For some reason, using ccw seems to invert the orientation
layout(triangles, equal_spacing, cw) in;

layout(location = 0) in vec3 inBasePosition[];
layout(location = 1) in vec3 inBaseNormal[];
layout(location = 2) in vec2 inColorTexCoordinates[];
layout(location = 3) in vec2 inHeightTexCoordinates[];
layout(location = 4) in int inMatrixIndex[];
layout(location = 5) in int inMaterialIndex[];
layout(location = 6) in vec2 inDeltaFactor[];
layout(location = 7) in int inColorTextureIndex[];
layout(location = 8) in int inHeightTextureIndex[];

layout(location = 0) out vec3 outWorldPosition;
layout(location = 1) out vec3 outBaseNormal;
layout(location = 2) out vec2 outColorTexCoordinates;
layout(location = 3) out vec2 outHeightTexCoordinates;
layout(location = 4) out int outMatrixIndex;
layout(location = 5) out int outMaterialIndex;
layout(location = 6) out vec2 outDeltaFactor;
layout(location = 7) out int outColorTextureIndex;
layout(location = 8) out int outHeightTextureIndex;

layout(set = 0, binding = 0) uniform Camera {
    mat4 eyeMatrices[2];
} camera;
layout(set = 0, binding = 1) uniform sampler textureSampler;

layout(set = 0, binding = 2) readonly buffer Objects {
    mat4 transformationMatrices[];
} objects;

// Note: this must be identical to BasicPipelineLayout.MAX_NUM_DESCRIPTOR_IMAGES
// Perhaps I can use specialization constants or 'runtime substitiutions' and compile the shader on runtime
layout(set = 1, binding = 1) uniform texture2D heightTextures[100];

layout(push_constant) uniform PushConstants {
    int eyeIndex;
} pushConstants;

vec2 mixVec2(const vec2 vectors[gl_MaxPatchVertices]) {
    return gl_TessCoord.x * vectors[0] + gl_TessCoord.y * vectors[1] + gl_TessCoord.z * vectors[2];
}

vec3 mixVec3(const vec3 vectors[gl_MaxPatchVertices]) {
    return gl_TessCoord.x * vectors[0] + gl_TessCoord.y * vectors[1] + gl_TessCoord.z * vectors[2];
}

void main() {
    vec3 basePosition = mixVec3(inBasePosition);
    vec3 baseNormal = mixVec3(inBaseNormal);
    vec2 colorTexCoordinates = mixVec2(inColorTexCoordinates);
    vec2 heightTexCoordinates = mixVec2(inHeightTexCoordinates);
    // For now, we will assume each vertex in the same patch will have the same matrix index and texture indices
    int matrixIndex = inMatrixIndex[0];
    int colorTextureIndex = inColorTextureIndex[0];
    int heightTextureIndex = inHeightTextureIndex[0];
    // I can't think of any reason why the materialIndex or deltaFactor should differ per vertex
    int materialIndex = inMaterialIndex[0];
    vec2 deltaFactor = inDeltaFactor[0];

    mat4 transformationMatrix = objects.transformationMatrices[matrixIndex];
    float extraHeight = texture(sampler2D(heightTextures[heightTextureIndex], textureSampler), heightTexCoordinates).r;
    vec3 improvedPosition = basePosition + baseNormal * extraHeight;
    vec4 transformedPosition = transformationMatrix * vec4(improvedPosition, 1.0);

    gl_Position = camera.eyeMatrices[pushConstants.eyeIndex] * transformedPosition;

    outWorldPosition = transformedPosition.xyz;
    outBaseNormal = baseNormal;
    outColorTexCoordinates = colorTexCoordinates;
    outHeightTexCoordinates = heightTexCoordinates;
    outMatrixIndex = matrixIndex;
    outMaterialIndex = materialIndex;
    outDeltaFactor = deltaFactor;
    outColorTextureIndex = colorTextureIndex;
    outHeightTextureIndex = heightTextureIndex;
}
