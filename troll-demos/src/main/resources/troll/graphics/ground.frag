#version 450

layout(location = 0) in vec3 worldPosition;
layout(location = 1) in vec3 deltaHeight;
layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 1) uniform isampler2D heightMap;

void main() {
    bool remainderX = int(floor(worldPosition.x)) % 2 == 0;
    bool remainderZ = int(floor(worldPosition.z)) % 2 == 0;

    vec3 vectorX = normalize(vec3(1.0, deltaHeight.x, 0.0));
    vec3 vectorZ = normalize(vec3(0.0, deltaHeight.z, 1.0));
    vec3 normal = normalize(cross(vectorZ, vectorX));

    float ka = 0.1;
    float kd = 0.7;
    vec3 toLight = normalize(vec3(1.0, 10.0, 0.0));
    vec3 toCamera = normalize(-worldPosition);
    if (sqrt(dot(worldPosition, worldPosition)) < 30 && remainderX != remainderZ) ka += 0.2;
    vec3 terrainColor = vec3(0.4, 0.1, 0.0);

    vec3 outputColor = ka * terrainColor + kd * dot(toLight, normal) * terrainColor;

    outColor = vec4(outputColor, 1.0);
}
