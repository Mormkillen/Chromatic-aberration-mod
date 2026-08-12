#version 150

uniform sampler2D InSampler;

layout(std140) uniform AberrationConfig {
    float Intensity;
    float UseDeadzone;
    float DebugMode;
};

in vec2 texCoord;
out vec4 fragColor;

// The aberration is only allowed in the OUTER half of the view:
// everything from the crosshair out to halfway to the screen border
// stays perfectly clean (EFFECT_START), then the effect ramps in
// smoothly until it reaches full strength (EFFECT_FULL).
const float EFFECT_START = 0.25;
const float EFFECT_FULL  = 0.50;

void main(){
    int mode = int(DebugMode + 0.5);

    // DebugMode 1: pure passthrough copy (bisects shader-math vs pipeline issues)
    if (mode == 1) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    // DebugMode 3: visualize the framebuffer alpha channel as grayscale
    if (mode == 3) {
        float a = texture(InSampler, texCoord).a;
        fragColor = vec4(a, a, a, 1.0);
        return;
    }

    vec2 centered = texCoord - vec2(0.5);
    float dist = length(centered);

    // Radial deadzone: 0 inside EFFECT_START, 1 at EFFECT_FULL and beyond.
    // UseDeadzone = 0 disables the clean center zone (config option).
    float mask = UseDeadzone > 0.5 ? smoothstep(EFFECT_START, EFFECT_FULL, dist) : 1.0;

    // Radial chromatic aberration: the RGB channels are sampled at slightly
    // different offsets along the direction away from the screen center.
    vec2 offset = centered * (dist * Intensity * 0.12 * mask);

    // Clamp so the shifted samples can never leave the framebuffer.
    vec2 uvR = clamp(texCoord + offset, vec2(0.0), vec2(1.0));
    vec2 uvB = clamp(texCoord - offset, vec2(0.0), vec2(1.0));

    float r = texture(InSampler, uvR).r;
    vec4  g = texture(InSampler, texCoord);
    float b = texture(InSampler, uvB).b;

    // DebugMode 2: force opaque alpha (tests alpha-channel corruption)
    float alpha = (mode == 2) ? 1.0 : g.a;

    fragColor = vec4(r, g.g, b, alpha);
}
