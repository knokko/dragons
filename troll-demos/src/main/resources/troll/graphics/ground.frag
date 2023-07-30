#version 450

layout(location = 0) in vec3 worldPosition;
//layout(location = 1) in vec3 deltaHeight;
layout(location = 1) in vec2 textureCoordinates;
layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 2) uniform sampler2D normalMap;

void main() {
    bool remainderX = int(floor(worldPosition.x)) % 2 == 0;
    bool remainderZ = int(floor(worldPosition.z)) % 2 == 0;

//    vec3 vectorX = normalize(vec3(1.0, deltaHeight.x, 0.0));
//    vec3 vectorZ = normalize(vec3(0.0, deltaHeight.z, 1.0));
//    vec3 normal = normalize(cross(vectorZ, vectorX));
    vec3 normal = texture(normalMap, textureCoordinates).rgb;

    float ka = 0.1;
    float kd = 0.7;
    vec3 toLight = normalize(vec3(1.0, 10.0, 0.0));
    vec3 toCamera = normalize(-worldPosition);
    if (sqrt(worldPosition.x * worldPosition.x + worldPosition.z * worldPosition.z) < 30 && remainderX != remainderZ) ka += 0.2;
    vec3 terrainColor = vec3(1.0, 1.0, 1.0);

    vec3 outputColor = ka * terrainColor + kd * dot(toLight, normal) * terrainColor;

    outColor = vec4(outputColor, 1.0);
    //outColor = vec4(0.0, 0.0, length(normal), 1.0);
    //outColor = vec4(abs(normal.x), abs(normal.y), abs(normal.z), 1.0);
    //outColor = vec4(textureCoordinates, 0.0, 1.0);
}
