#version 330 core

in vec2 TexCoord;  // Texture coordinates from vertex shader
in vec4 Color;    // Final fragment color

out vec4 color;    // Final fragment color

uniform sampler2D textureSampler;  // Texture sampler
uniform float brightness;          // Brightness adjustment
uniform bool isFixedFunction;      // Fixed-function toggle

void main() {
    vec4 texColor = texture(textureSampler, TexCoord);  // Sample the texture

    if (isFixedFunction) {
        // Fixed-function behavior: Just use the texture color directly
        color = texture(textureSampler, TexCoord) * brightness;
    } else {
        // Custom shader behavior: Apply brightness adjustment
        texColor *= brightness;
        color = texColor * Color;  // Modulate texture color with vertex color
    }
}
