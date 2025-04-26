#version 330 core
in vec2 TexCoord;
in vec4 VertexColor;
out vec4 FragColor;
uniform sampler2D textureSampler;
uniform float brightness = 0.5;
uniform vec3 colorTint = vec3(0.5, 0.5, 0.5);
void main()
    {
    vec4 texColor = texture(textureSampler, TexCoord) * VertexColor;
    // Apply brightness and color tint
    vec3 finalColor = texColor.rgb * brightness * colorTint;
    FragColor = vec4(finalColor, texColor.a);
    }