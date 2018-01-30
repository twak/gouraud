#version 150

in vec3 vColour;

void main() {
    gl_FragColor = vec4 ( vColour, 1);
}