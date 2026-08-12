#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

// Unconditional RGBA copy. Pixels whose framebuffer alpha is 0 (e.g. the
// fog-cleared band below the horizon at high altitude) must be written
// verbatim; blending by source alpha would erase them to the clear color.
void main(){
    fragColor = texture(InSampler, texCoord);
}
