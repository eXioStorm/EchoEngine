#version 330 core

// ---- Inputs (match existing shader) ----
in vec2 TexCoord;
in vec4 VertexColor;

// ---- Output (match existing shader) ----
out vec4 FragColor;

// ---- Uniforms (match existing shader names) ----
uniform sampler2D textureSampler;

// Optional controls you already had
uniform float brightness = 1.0;
uniform vec3 colorTint = vec3(1.0, 1.0, 1.0);

// ---- MSDF helpers ----
float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

// Standard MSDF screen pixel range function
float screenPxRange() {
    vec2 unitRange = vec2(1.0) / vec2(textureSize(textureSampler, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(TexCoord);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    // Sample MSDF texture
    vec3 msd = texture(textureSampler, TexCoord).rgb;

    // Compute signed distance
    float sd = median(msd.r, msd.g, msd.b);

    // Convert to screen-space distance
    float screenPxDistance = screenPxRange() * (sd - 0.5);

    // Final alpha
    float alpha = clamp(screenPxDistance + 0.5, 0.0, 1.0);

    // Apply vertex color, brightness, and tint
    vec3 finalColor = VertexColor.rgb * brightness * colorTint;

    FragColor = vec4(finalColor, alpha * VertexColor.a);
}
