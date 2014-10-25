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
    gl_Position = mat * vec4(vec3(position.xy, position.z + zOffsets[gl_VertexID]), 1);
}
