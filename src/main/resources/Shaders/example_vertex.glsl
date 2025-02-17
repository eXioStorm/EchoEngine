#version 330 core

layout(location = 0) in vec3 vertexPosition; // Position of the vertex
layout(location = 1) in vec2 texCoords;      // Texture coordinates
layout(location = 2) in vec4 vertexColor;    // Color (if needed)

out vec2 TexCoord;  // Pass texture coordinates to fragment shader
out vec4 Color;     // Pass vertex color to fragment shader (optional)

uniform bool isFixedFunction;

void main() {
    if (isFixedFunction) {
        gl_Position = gl_ModelViewProjectionMatrix * vec4(vertexPosition, 1.0);
        TexCoord = gl_MultiTexCoord0.xy;
        Color = vec4(1.0); // Default color
    } else {
        gl_Position = vec4(vertexPosition, 1.0);
        TexCoord = texCoords;
        Color = vertexColor;
    }
}