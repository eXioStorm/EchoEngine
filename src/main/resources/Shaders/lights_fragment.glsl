#version 330 core
in vec2 TexCoord;
out vec4 FragColor;
uniform sampler2D textureSampler;
uniform vec2 screenSize;
uniform int numLights;
uniform vec2 lightPositions[10];// Support up to 10 lights
uniform vec3 lightColors[10];
uniform float lightIntensities[10];
uniform float lightRadii[10];
uniform float ambientLight;
void main()
    {
    vec4 texColor = texture(textureSampler, TexCoord);
    vec3 lighting = vec3(ambientLight, ambientLight, ambientLight);
    vec2 pixelPos = TexCoord * screenSize;
        for(int i = 0; i < numLights; i++) {
            float distance = length(pixelPos - lightPositions[i]);
            if(distance < lightRadii[i]) {
                float attenuation = 1.0 - (distance / lightRadii[i]);
                attenuation = pow(attenuation, 2.0) * lightIntensities[i];
                lighting += lightColors[i] * attenuation;
            }
        }
    lighting = clamp(lighting, 0.0, 1.0);
    FragColor = texColor * vec4(lighting, 1.0);
    }