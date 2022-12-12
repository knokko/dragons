#version 440
// For some reason, using ccw seems to invert the orientation
layout(triangles, equal_spacing, cw) in;

layout(constant_id = 0) const int MAX_NUM_DESCRIPTOR_IMAGES = 100;

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
layout(location = 4) out mat4 outTransformationMatrix;
layout(location = 8) out int outMaterialIndex;
layout(location = 9) out vec2 outDeltaFactor;
layout(location = 10) out int outColorTextureIndex;
layout(location = 11) out int outHeightTextureIndex;

layout(set = 0, binding = 0) uniform Camera {
    mat4 eyeMatrices[2];
} camera;
layout(set = 0, binding = 1) uniform sampler textureSampler;

layout(set = 0, binding = 2) readonly buffer Objects {
    mat4 transformationMatrices[];
} objects;

layout(set = 1, binding = 1) uniform texture2D heightTextures[MAX_NUM_DESCRIPTOR_IMAGES];

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

    // I can't think of any reason why the texture indices, materialIndex, or deltaFactor should differ per vertex
    int colorTextureIndex = inColorTextureIndex[0];
    int heightTextureIndex = inHeightTextureIndex[0];
    int materialIndex = inMaterialIndex[0];
    vec2 deltaFactor = inDeltaFactor[0];

    mat4 transformationMatrix = gl_TessCoord.x * objects.transformationMatrices[inMatrixIndex[0]]
            + gl_TessCoord.y * objects.transformationMatrices[inMatrixIndex[1]]
            + gl_TessCoord.z * objects.transformationMatrices[inMatrixIndex[2]];
    float extraHeight = texture(sampler2D(heightTextures[heightTextureIndex], textureSampler), heightTexCoordinates).r;
    vec3 improvedPosition = basePosition + baseNormal * extraHeight;
    vec4 transformedPosition = transformationMatrix * vec4(improvedPosition, 1.0);

    gl_Position = camera.eyeMatrices[pushConstants.eyeIndex] * transformedPosition;

    outWorldPosition = transformedPosition.xyz;
    outBaseNormal = baseNormal;
    outColorTexCoordinates = colorTexCoordinates;
    outHeightTexCoordinates = heightTexCoordinates;
    outTransformationMatrix = transformationMatrix;
    outMaterialIndex = materialIndex;
    outDeltaFactor = deltaFactor;
    outColorTextureIndex = colorTextureIndex;
    outHeightTextureIndex = heightTextureIndex;
}
