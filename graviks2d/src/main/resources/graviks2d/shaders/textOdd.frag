#version 450

layout(input_attachment_index = 0, set = 0, binding = 0) uniform subpassInput inputValue;

layout(location = 0) out float outputValue;

void main() {
    float rawValue = subpassLoad(inputValue).r;
    int numFlips = int(rawValue * 255.0 + 0.5);
    if (numFlips % 2 == 0) {
        outputValue = 0.0;
    } else {
        outputValue = 1.0;
    }
}
