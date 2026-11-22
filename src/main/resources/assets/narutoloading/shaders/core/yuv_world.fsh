#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    float y = texture(Sampler0, texCoord0).r;
    float u = texture(Sampler1, texCoord0).r;
    float v = texture(Sampler2, texCoord0).r;

    float c = y - 16.0 / 255.0;
    float d = u - 128.0 / 255.0;
    float e = v - 128.0 / 255.0;

    float r = 1.164 * c + 1.596 * e;
    float g = 1.164 * c - 0.392 * d - 0.813 * e;
    float b = 1.164 * c + 2.017 * d;

    fragColor = vec4(clamp(vec3(r, g, b), 0.0, 1.0), 1.0);
}
