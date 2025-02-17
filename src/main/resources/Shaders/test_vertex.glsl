#version 330 core
layout(location = 0) in vec3 vertexPosition; // Position
layout(location = 1) in vec2 texCoords;      // Texture coordinates

out vec2 TexCoord; // Pass texture coordinates to fragment shader

void main() {
    gl_Position = vec4(vertexPosition, 1.0); // Transform to clip-space
    TexCoord = texCoords; // Pass texture coordinates through
}
