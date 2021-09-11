#version 440
// For some reason, using ccw seems to invert the orientation
layout(triangles, equal_spacing, cw) in;

layout(location = 0) in vec3 inBasePosition[];
layout(location = 1) in vec3 inBaseNormal[];
layout(location = 2) in vec2 inColorTexCoordinates[];
layout(location = 3) in vec2 inHeightTexCoordinates[];
layout(location = 4) in int inMatrixIndex[];
layout(location = 5) in int inMaterialIndex[];
layout(location = 6) in float inDeltaFactor[];

layout(location = 0) out vec3 outWorldPosition;
layout(location = 1) out vec3 outBaseNormal;
layout(location = 2) out vec2 outColorTexCoordinates;
layout(location = 3) out vec2 outHeightTexCoordinates;
layout(location = 4) out int outMatrixIndex;
layout(location = 5) out int outMaterialIndex;
layout(location = 6) out float outDeltaFactor;

layout(set = 0, binding = 0) uniform Camera {
    mat4 matrix;
    vec3 position;
} camera;
layout(set = 0, binding = 2) uniform sampler2D heightTextureSampler;
layout(set = 0, binding = 3) readonly buffer Objects {
    mat4 transformationMatrices[];
} objects;

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
    // For now, we will assume each vertex in the same patch will have the same matrix index
    int matrixIndex = inMatrixIndex[0];
    // I can't think of any reason why the materialIndex or deltaFactor should be different
    int materialIndex = inMaterialIndex[0];
    float deltaFactor = inDeltaFactor[0];

    mat4 transformationMatrix = objects.transformationMatrices[matrixIndex];
    float extraHeight = texture(heightTextureSampler, heightTexCoordinates).r;
    vec3 improvedPosition = basePosition + baseNormal * extraHeight;
    vec4 transformedPosition = transformationMatrix * vec4(improvedPosition, 1.0);

    // TODO Wait... if we only compute the position here, how does the control shader determine distance to camera?
    gl_Position = camera.matrix * transformedPosition;

    outWorldPosition = transformedPosition.xyz;
    outBaseNormal = baseNormal;
    outColorTexCoordinates = colorTexCoordinates;
    outHeightTexCoordinates = heightTexCoordinates;
    outMatrixIndex = matrixIndex;
    outMaterialIndex = materialIndex;
    outDeltaFactor = deltaFactor;
}
