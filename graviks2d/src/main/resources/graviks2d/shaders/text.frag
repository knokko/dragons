#version 450

layout(location = 0) in flat int operation;
layout(location = 1) in vec3 barycentricCoordinates;

layout(location = 0) out float outputValue;

const int OPERATION_INCREMENT = 1;
const int OPERATION_CORRECT_START = 2;
const int OPERATION_CORRECT_CONTROL = 3;
const int OPERATION_CORRECT_END = 4;

const float OUTPUT_ONE = 1.0 / 255.0;

void main() {
    if (operation == OPERATION_INCREMENT) {
        outputValue = OUTPUT_ONE;
    } else if (operation == OPERATION_CORRECT_START || operation == OPERATION_CORRECT_CONTROL || operation == OPERATION_CORRECT_END) {
        float s = barycentricCoordinates.y;
        float t = max(barycentricCoordinates.x, barycentricCoordinates.z);
        if ((s / 2.0 + t) * (s / 2.0 + t) < t) {
            outputValue = OUTPUT_ONE;
        } else {
            outputValue = 0.0;
        }
    } else {
        // This high value can be used to indicate bugs
        outputValue = 0.2;
    }
}
