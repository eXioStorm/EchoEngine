#version 330 core
in vec2 TexCoord;
in vec4 VertexColor;
out vec4 FragColor;
uniform sampler2D textureSampler;
uniform float brightness = 1.0;
uniform vec3 colorTint = vec3(1.0, 1.0, 1.0);
void main()
    {
    vec4 texColor = texture(textureSampler, TexCoord) * VertexColor;
    // Apply brightness and color tint
    vec3 finalColor = texColor.rgb * brightness * colorTint;
    FragColor = vec4(finalColor, texColor.a);
    }