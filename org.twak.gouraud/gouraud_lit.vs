#version 150

in vec4 aVertex;
in vec3 aNormal;

uniform mat4 uModelMatrix;
uniform mat4 uViewMatrix;
uniform mat4 uProjectionMatrix;

uniform mat3 uNormalMatrix;
uniform vec3 uLightPosition;

out vec4 vColour;

void main() {
    vec4 modelPosition = uModelMatrix * aVertex;
    vec4 viewPosition = uViewMatrix * modelPosition;
    gl_Position = uProjectionMatrix * viewPosition;
    
    vec4 viewDir = normalize ( viewPosition );
    vec vNormal = vec4 ( uNormalMatrix * aNormal, 1);
    
    vec4 diffuse =  dot ( viewDir, vec4 ( aNormal, 1 ) ) * vec4 (1,0,0, 1);
    //vec4 specular = 
    
    LightPosition = vec3(view * vec4(lightPosition, 1.0));
    
    
    vColour = diffuse;// + specular;
}
