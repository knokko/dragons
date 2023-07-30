#version 450

layout(location = 0) in vec2 inNormal;
layout(location = 1) in float inHeightFraction;

layout(location = 0) out vec4 outColor;

void main() {
    vec3 baseColor = vec3(0.2, 0.8, 0.3);
    float heightFactor = 0.1 + 0.9 * inHeightFraction;
    outColor = vec4(baseColor * heightFactor, 1.0);
}
