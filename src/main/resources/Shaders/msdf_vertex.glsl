#version 330 core
layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec4 vertexColor;
out vec2 TexCoord;
out vec4 fragColor;
uniform mat4 MVP;
void main()
{
    gl_Position = MVP * vec4(position, 1);
    TexCoord = texCoord;
    fragColor = vertexColor;
}