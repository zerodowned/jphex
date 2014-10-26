#version 330 core

uniform mat4 mat;
uniform vec4 zOffsets;
uniform int textureType; // 0 = land, 1 = textured land, 2 = static

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texNormal;
layout(location = 2) in vec2 texTexture;

out vec2 texCoords;

void main() {
    if(textureType == 0) {
        // normal land
        switch(gl_VertexID) {
            case 0: texCoords = vec2(0.5, 0.0); break;
            case 1: texCoords = vec2(0.0, 0.5); break;
            case 2: texCoords = vec2(0.5, 1.0); break;
            case 3: texCoords = vec2(1.0, 0.5); break;
        }
    } else if(textureType == 1) {
        // land texture
        switch(gl_VertexID) {
            case 0: texCoords = vec2(0.0, 0.0); break;
            case 1: texCoords = vec2(0.0, 1.0); break;
            case 2: texCoords = vec2(1.0, 1.0); break;
            case 3: texCoords = vec2(1.0, 0.0); break;
        }
    } else if(textureType == 2) {
        // static
        switch(gl_VertexID) {
            case 0: texCoords = vec2(0.0, 0.0); break;
            case 1: texCoords = vec2(0.0, 1.0); break;
            case 2: texCoords = vec2(1.0, 1.0); break;
            case 3: texCoords = vec2(1.0, 0.0); break;
        }
    }
    vec4 fixedPosition = vec4(position, 1);
    fixedPosition.z += zOffsets[gl_VertexID];
    gl_Position = mat * fixedPosition;
}
