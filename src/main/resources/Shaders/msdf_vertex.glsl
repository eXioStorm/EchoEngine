#version 330 core
layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec4 color;
out vec2 TexCoord;
out vec4 VertexColor;
uniform mat4 projectionMatrix = mat4(1.0);
void main()
{
    gl_Position = projectionMatrix * vec4(position, 1.0);
    TexCoord = texCoord;
    VertexColor = color;
}