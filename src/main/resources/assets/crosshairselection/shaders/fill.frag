#version 120

// 选框内部填充的片段着色器。
//
// 和描边共用同一套流光参数（ShaderSupport.uploadFlow），
// 因此填充和边框的彩虹是同一份颜色，不会各跑各的。
// 纯色模式（uFlowMode == 0）直接输出基色。

uniform vec4 uColour;               // 基色，alpha 为整体透明度

uniform float uFlowMode;
uniform vec4 uFlowColour;           // (色相起点, 色相范围, 饱和度, 亮度)
uniform float uFlowPhase;
uniform float uFlowMotion;
uniform float uFlowDensity;
uniform float uFlowProgress;
uniform float uFlowSoftness;
uniform vec3 uSweepAxis;
uniform float uSweepCentreOut;

uniform vec3 uFlowAxis;
uniform vec3 uFlowOrigin;
uniform float uSweepMin;
uniform float uSweepMax;
uniform float uSweepRadius;

varying vec3 vWorld;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// 时间曲线，mode 与 FlowSettings.Motion 的序号一致。
float motionPhase(float x, int mode, vec2 position) {
    if (mode == 1) {
        // 单向流动，速度平滑变化：单调递增的余弦缓动。
        // 以前这里是 0.5 - 0.5cos(2πx)，和三角波一样是往复振荡，观感几乎一样。
        float w = fract(x);
        return w - sin(6.28318530718 * w) / 6.28318530718;
    }
    if (mode == 2) {
        return abs(fract(x) * 2.0 - 1.0);
    }
    if (mode == 3) {
        float t = abs(fract(x) * 2.0 - 1.0) * 8.0;
        return valueNoise(position * 0.05 + vec2(t * 0.8, t * 0.5)) * 0.65
                + valueNoise(position * 0.10 + vec2(t * 1.6 + 17.3, t * 1.1)) * 0.35;
    }
    return fract(x);
}

vec3 hsvToRgb(vec3 c) {
    vec3 p = abs(fract(c.xxx + vec3(1.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);
    return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), clamp(c.y, 0.0, 1.0));
}

float flowCoord(vec3 world) {
    if (uSweepCentreOut > 0.5) {
        return length(world - uFlowOrigin);
    }
    return dot(world - uFlowOrigin, uFlowAxis);
}

vec3 flowSample(vec3 world) {
    float phase = motionPhase(uFlowPhase, int(uFlowMotion), world.xz * 40.0);
    float coord = flowCoord(world);
    vec3 flow = hsvToRgb(vec3(
            fract(uFlowColour.x + fract(coord * uFlowDensity + phase) * uFlowColour.y),
            uFlowColour.z, uFlowColour.w));

    if (uFlowMode > 1.5) {
        float along = uSweepCentreOut > 0.5
                ? coord / max(uSweepRadius, 0.0001)
                : (coord - uSweepMin) / max(uSweepMax - uSweepMin, 0.0001);
        float d = along - uFlowProgress;
        float mask = d <= 0.0 ? 1.0 : 1.0 - smoothstep(0.0, max(uFlowSoftness, 0.001), d);
        flow = mix(uColour.rgb, flow, mask);
    }
    return flow;
}

void main() {
    vec3 rgb = uColour.rgb;

    if (uFlowMode > 0.5) {
        rgb = flowSample(vWorld);
    }

    if (uColour.a <= 0.004) {
        discard;
    }
    gl_FragColor = vec4(rgb, uColour.a);
}
