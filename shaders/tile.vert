#version 330 core

uniform mat4 mat;
uniform vec4 zOffsets;
uniform bool isLandTexture;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texNormal;
layout(location = 2) in vec2 texTexture;

out vec2 texCoords;

void main() {
    if(isLandTexture) {
        texCoords = texTexture;
    } else {
        texCoords = texNormal;
    }
    vec4 fixedPosition = vec4(position, 1);
    fixedPosition.z += zOffsets[gl_VertexID];
    gl_Position = mat * fixedPosition;
}
