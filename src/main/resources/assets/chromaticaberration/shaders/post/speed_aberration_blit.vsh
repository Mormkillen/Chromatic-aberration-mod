#version 150

in vec4 Position;

uniform mat4 ProjMat;
uniform vec2 OutSize;

out vec2 texCoord;

// 1.21.5+ post vertex convention: Position.xy is already 0..1 UV space;
// scale by OutSize for the ortho projection and pass the UV straight through.
void main(){
    vec4 outPos = ProjMat * vec4(Position.xy * OutSize, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    texCoord = Position.xy;
}
