#version 460

layout(location = 0) in vec3 inBasePosition;
layout(location = 1) in vec3 inBaseNormal;
layout(location = 2) in vec2 inColorTexCoordinates;
layout(location = 3) in vec2 inHeightTexCoordinates;
layout(location = 4) in int inMatrixIndex;
layout(location = 5) in int inMaterialIndex;
layout(location = 6) in vec2 inDeltaFactor;
layout(location = 7) in int inColorTextureIndex;
layout(location = 8) in int inHeightTextureIndex;

layout(location = 0) out vec3 outBasePosition;
layout(location = 1) out vec3 outBaseNormal;
layout(location = 2) out vec2 outColorTexCoordinates;
layout(location = 3) out vec2 outHeightTexCoordinates;
layout(location = 4) out int outMatrixIndex;
layout(location = 5) out int outMaterialIndex;
layout(location = 6) out vec2 outDeltaFactor;
layout(location = 7) out int outColorTextureIndex;
layout(location = 8) out int outHeightTextureIndex;

void main() {
    outBasePosition = inBasePosition;
    outBaseNormal = inBaseNormal;
    outColorTexCoordinates = inColorTexCoordinates;
    outHeightTexCoordinates = inHeightTexCoordinates;
    outMatrixIndex = inMatrixIndex + gl_BaseInstance;
    outMaterialIndex = inMaterialIndex;
    outDeltaFactor = inDeltaFactor;
    outColorTextureIndex = inColorTextureIndex;
    outHeightTextureIndex = inHeightTextureIndex;
}
