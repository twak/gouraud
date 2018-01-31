#version 150

in vec4 aVertex;

uniform mat4 uModelMatrix;
uniform mat4 uViewMatrix;
uniform mat4 uProjectionMatrix;

void main() {
    vec4 modelPosition = uModelMatrix * aVertex;
    vec4 viewPosition = uViewMatrix * modelPosition;
    gl_Position = uProjectionMatrix * viewPosition;
}
