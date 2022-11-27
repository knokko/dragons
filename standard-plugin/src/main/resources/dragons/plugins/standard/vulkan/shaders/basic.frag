#version 450

layout(constant_id = 0) const int MAX_NUM_DESCRIPTOR_IMAGES = 100;

layout(location = 0) in vec3 worldPosition;
layout(location = 1) in vec3 baseNormal;
layout(location = 2) in vec2 colorTexCoordinates;
layout(location = 3) in vec2 heightTexCoordinates;
layout(location = 4) flat in int matrixIndex;
layout(location = 5) flat in int materialIndex;
layout(location = 6) in vec2 deltaFactor;
layout(location = 7) flat in int colorTextureIndex;
layout(location = 8) flat in int heightTextureIndex;

layout(location = 0) out vec4 outColor;

layout(set = 0, binding = 0) uniform Camera {
    mat4 eyeMatrices[2];
} camera;
layout(set = 0, binding = 1) uniform sampler textureSampler;

layout(set = 0, binding = 2) readonly buffer Objects {
    mat4 transformationMatrices[];
} objects;

layout(set = 1, binding = 0) uniform texture2D colorTextures[MAX_NUM_DESCRIPTOR_IMAGES];
layout(set = 1, binding = 1) uniform texture2D heightTextures[MAX_NUM_DESCRIPTOR_IMAGES];

struct MaterialProperties {
    float ambientValue;

    float diffuseMaterialFactor;
    float diffuseLightFactor;

    float specularMaterialFactor;
    float specularLightFactor;
    float shininess;
} MATERIALS[] = {
    // The first material is terrain material. This doesn't have any specular lighting
    {
        0.2, // ambientValue
        0.8, // diffuseMaterialFactor
        0.0, // diffuseLightFactor
        0.0, // specularMaterialFactor
        0.0, // specularLightFactor
        0.0 // shininess
    },
    // The second material is plastic material. This has both diffuse and specular lighting
    {
        0.1, // ambientValue
        0.6, // diffuseMaterialFactor
        0.0, // diffuseLightFactor
        0.0, // specularMaterialFactor
        0.3, // specularLightFactor
        20.0 // shininess
    },
    // The third material is metal. This has mostly specular lighting
    {
        0.1, // ambientValue
        0.3, // diffuseMaterialFactor
        0.0, // diffuseLightFactor
        0.6, // specularMaterialFactor
        0.0, // specularLightFactor
        50.0 // shininess
    }
};

mat3 rotationMatrix(vec3 axis, float angle) {
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;

    return mat3(
        oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,
        oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,
        oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c
    );
}

float sampleHeight(vec2 sampleCoordinates) {
    return texture(sampler2D(heightTextures[heightTextureIndex], textureSampler), sampleCoordinates).r;
}

/**
 * Computes the normal vector that this 'fragment' would get if we only use the height texture to determine the normal
 * vector.
 */
vec3 computeHeightNormal() {

    // The normal vector is basically the derivative of the height. We approximate this by using a small delta x and z
    float delta = 0.001;

    float heightLowX = sampleHeight(heightTexCoordinates - vec2(delta, 0.0));
    float heightHighX = sampleHeight(heightTexCoordinates + vec2(delta, 0.0));
    float heightLowZ = sampleHeight(heightTexCoordinates - vec2(0.0, delta));
    float heightHighZ = sampleHeight(heightTexCoordinates + vec2(0.0, delta));

    float heightDiffX = heightHighX - heightLowX;
    float heightDiffZ = heightHighZ - heightLowZ;
    vec3 heightVecX = vec3(2.0 * delta * deltaFactor.x, heightDiffX, 0.0);
    vec3 heightVecZ = vec3(0.0, heightDiffZ, 2.0 * delta * deltaFactor.y);

    return normalize(cross(heightVecZ, heightVecX));
}

/**
 * The computeHeightNormal() method computes the normal vector ONLY based on the height texture, but we should also
 * take the base normal vector of this 'fragment' into account. (For instance, if the height normal is (0, 1, 0) and
 * the base normal is (1, 0, 0), the combined normal would be (1, 0, 0). This gets much more complex for other height
 * normals.)
 */
vec3 combineBaseWithHeightNormal(vec3 heightNormal) {
    float PI = 3.1415926535897932384626433832795;
    vec3 up = vec3(0.0, 1.0, 0.0);

    float transformAngle = acos(dot(baseNormal, up));
    vec3 combinedNormal = heightNormal;

    // If we need to rotate (nearly) 180 degrees, we should just negate it. Using the general rotate method won't work
    // (well) in this case because the cross product would be (nearly) the zero vector.
    if (transformAngle > PI - 0.01) {
        combinedNormal = -heightNormal;
    } else if (transformAngle > 0.01) {
        vec3 crossHelper = cross(baseNormal, up);

        // With this cross method, I don't know in advance whether I need to rotate clickwise or counter-clockwise
        mat3 matrix1 = rotationMatrix(crossHelper, transformAngle);
        mat3 matrix2 = rotationMatrix(crossHelper, -transformAngle);
        vec3 test = matrix1 * up;
        if (dot(test, baseNormal) > 0.99) {
            combinedNormal = matrix1 * heightNormal;
        } else {
            combinedNormal = matrix2 * heightNormal;
        }
    }

    return combinedNormal;
}

void main() {
    vec3 heightNormal = computeHeightNormal();

    vec3 combinedNormal = combineBaseWithHeightNormal(heightNormal);

    mat4 transformationMatrix = objects.transformationMatrices[matrixIndex];
    vec3 transformedNormal = normalize((transformationMatrix * vec4(combinedNormal, 0.0)).xyz);

    MaterialProperties material = MATERIALS[materialIndex];
    vec3 textureColor = texture(sampler2D(colorTextures[colorTextureIndex], textureSampler), colorTexCoordinates).rgb;
    vec3 lightColor = vec3(1.0, 1.0, 1.0);

    vec3 ambientColor = textureColor * material.ambientValue;
    vec3 diffuseColor = material.diffuseMaterialFactor * textureColor + material.diffuseLightFactor * lightColor;
    vec3 specularColor = material.specularMaterialFactor * textureColor + material.specularLightFactor * lightColor;
    float shininess = material.shininess;

    // TODO Stop hardcoding this
    vec3 toLight = normalize(vec3(100, 120, 10));
    vec3 toView = normalize(-worldPosition);

    float dotLightNormal = dot(toLight, transformedNormal);
    vec3 resultColor = ambientColor;

    if (dotLightNormal > 0.0) {
        resultColor += diffuseColor * dotLightNormal;

        vec3 reflectedLight = normalize(2.0 * dotLightNormal * transformedNormal - toLight);
        float dotReflectedView = dot(reflectedLight, toView);
        if (dotReflectedView > 0.0) {
            resultColor += specularColor * pow(dotReflectedView, shininess);
        }
    }

    outColor = vec4(resultColor, 1.0);
}
