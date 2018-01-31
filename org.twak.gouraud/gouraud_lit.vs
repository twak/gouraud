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
    vec4 vNormal = vec4 ( uNormalMatrix * aNormal, 1);
    vec4 lightPosition = uModelMatrix * vec4 ( uLightPosition, 1);
    vec4 lightDirection = normalize( lightPosition - viewPosition);
    float diffuseCo = dot ( lightDirection, vNormal );
    vec4 diffuse = diffuseCo * vec4 (1,0,0, 1);
    
    vec3 H = normalize (lightDirection.xyz + viewDir.xyz);
    float specularCo = pow(max(0.0, dot(vNormal.xyz, H)), 3);
    vec4 specular = specularCo * vec4 (1,1,1,1);
    
    vColour = diffuse + specular;
}
