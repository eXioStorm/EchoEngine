#version 330 core
in vec2 TexCoord;              // Texture coordinates from vertex shader
out vec4 color;                // Final fragment color

uniform sampler2D textureSampler; // Texture sampler

void main() {
    color = texture(textureSampler, TexCoord); // Sample texture
    //color = vec4(1.0, 0.0, 0.0, 1.0); // Red solid color
}
