#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

// Replacement-copy blit. Vanilla's "blit" program blends by source alpha,
// which erases pixels whose framebuffer alpha is 0 (e.g. the fog-cleared
// band below the horizon at high altitude) to the framebuffer clear color.
// This blit writes RGBA unconditionally, preserving those pixels.
void main(){
    fragColor = texture(DiffuseSampler, texCoord);
}
