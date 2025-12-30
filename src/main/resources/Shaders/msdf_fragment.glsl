#version 330 core

in vec2 TexCoord;
in vec4 fragColor;

out vec4 color;

uniform sampler2D textureSampler;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main()
{
    vec2 uv = vec2(TexCoord.x, 1.0 - TexCoord.y);

    vec3 sample = texture(textureSampler, uv).rgb;

    vec2 texSize = vec2(textureSize(textureSampler, 0));
    vec2 dxdy = vec2(dFdx(uv.x), dFdy(uv.y)) * texSize;
    float toPixels = inversesqrt(dot(dxdy, dxdy));

    float sigDist = (median(sample.r, sample.g, sample.b) - 0.5) * toPixels;
    float alpha = clamp(sigDist + 0.5, 0.0, 1.0);

    color = vec4(1.0, 0.0, 1.0, alpha+1.0);
}
