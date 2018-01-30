#version 150

in vec3 aColour;
in vec4 aVertex;

uniform mat4 uModelMatrix;
uniform mat4 uViewProjectionMatrix;

out vec3 vColour;

void main() {
    vec4 modelPosition = uModelMatrix * aVertex;
    gl_Position = uViewProjectionMatrix * modelPosition;
    vColour = aColour;
}
