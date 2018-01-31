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
    vec4 normal = vec4 ( uNormalMatrix * aNormal, 1);
    vec4 lightPosition = uModelMatrix * vec4 ( uLightPosition, 1);
    vec4 lightDirection = normalize( lightPosition - modelPosition);
    float diffuseCo = dot ( lightDirection, normal );
    vec4 diffuse = diffuseCo * vec4 (1,0,0, 1);

  	vColour = diffuse;  
}
