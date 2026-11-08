#version 120

// 准星的片段着色器。
//
// 整条准星在物理像素空间里用有向距离场（SDF）求值，所以任意旋转角度、
// 甚至亚像素线宽都有精确覆盖率；几何只是一个覆盖准星包围盒的四边形。
//
// 注意事项：
//   - 这里不用纹理采样，顶点数据的 UV 也不参与，几何完全由顶点着色器根据 uniform 摆放；
//   - 所有距离都以「物理像素」为单位，GUI 缩放已在 Java 端乘好；
//   - 反馈与蓄力的数值都是 0..1，由 CrosshairFeedback / TicCrosshairCompat 提供。

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

// 时间曲线，mode 与 FlowSettings.Motion 的序号一致：0 线性 / 1 余弦 / 2 三角 / 3 噪声
float motionPhase(float x, int mode, vec2 position) {
    if (mode == 1) {
        // 单向流动，速度平滑变化：单调递增的余弦缓动。
        float w = fract(x);
        return w - sin(6.28318530718 * w) / 6.28318530718;
    }
    if (mode == 2) {
        return abs(fract(x) * 2.0 - 1.0);
    }
    if (mode == 3) {
        // 噪声沿自己的空间走一条缓慢对角线；把 0..1 相位直接乘进坐标会让每个周期只跨几个噪声格，
        // 看起来是整格跳变。三角波让相位在回绕处保持连续。
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

/**
 * 流光取色。
 *
 * @param phase        已按时间与运动曲线推进的 0..1 相位
 * @param position     用于生成空间渐变的坐标（相对准星中心）
 * @param colourParams (色相起点, 色相范围, 饱和度, 亮度)
 * @param density      每 100 物理像素的色带数
 */
vec3 flowColour(float phase, vec2 position, vec4 colourParams, float density) {
    float band = dot(position, vec2(0.01)) * density;
    float t = colourParams.x + fract(band + phase) * colourParams.y;
    return hsvToRgb(vec3(fract(t), colourParams.z, colourParams.w));
}

// 准星的屏幕位置与屏幕尺寸，单位均为物理像素。
uniform vec2 uCentre;
uniform vec2 uScreen;
// 臂厚的一半、中心间隙、横/纵臂长、描边厚度、旋转弧度。
uniform float uHalf;
uniform float uGap;
uniform float uArmX;
uniform float uArmY;
uniform float uOutline;
uniform float uRotation;
uniform vec4 uColour;
uniform vec4 uOutlineColour;
uniform vec4 uDotColour;
uniform float uDot;
uniform float uFlowMode;
uniform vec4 uFlowColour;
uniform float uFlowPhase;
uniform float uFlowMotion;
uniform float uFlowDensity;
uniform float uFlowProgress;
uniform float uFlowSoftness;
uniform float uFlowOpacity;
uniform float uGlintStrength;
uniform float uGlintRadius;
uniform float uGlintStar;
uniform vec2 uGlintScreen;
// 交互反馈。所有样式共用同一个 0..1 的连续状态，由 CrosshairFeedback 按速度积分得出，
// 因此连续点击会从当前状态继续，而不是每次重置。
uniform float uPunch;        // 冲击位移量
uniform float uGapBoost;     // 额外的臂间距（物理像素）
uniform float uWaveScale;    // 反馈用的整体缩放（只影响臂到中心的距离）
uniform float uBowOpen;      // 拉弓带来的额外间隙，单位物理像素
uniform float uPlacePulse;   // 放置时的扩散光环

vec2 rotate(vec2 p) {
    float c = cos(uRotation);
    float s = sin(uRotation);
    return vec2(p.x * c - p.y * s, p.x * s + p.y * c);
}

// 矩形 SDF：内部为负，外部为正。
float sdBox(vec2 p, vec2 halfSize) {
    vec2 d = abs(p) - halfSize;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

/**
 * 准星 SDF。
 *
 * <p>拉弓只改变「间隙」（四条臂离中心多远），臂长始终不变：
 * 臂长变化会被看成线条被拉长/缩短，而间隙变化才是准星开合的直觉。
 */
float crossDistance(vec2 p, float armHalf, float wave) {
    // gap 叠加三部分：玩家设置的间隙、反馈位移、拉弓的张开量。
    // 臂长不参与任何缩放，始终等于玩家设置值。
    float gap = (uGap + uGapBoost) * wave + uBowOpen;
    float armY = uArmY;
    float armX = uArmX;
    float verticalTop = sdBox(p - vec2(0.0, -(gap + armY * 0.5)), vec2(armHalf, armY * 0.5));
    float verticalBottom = sdBox(p - vec2(0.0, gap + armY * 0.5), vec2(armHalf, armY * 0.5));
    float horizontalLeft = sdBox(p - vec2(-(gap + armX * 0.5), 0.0), vec2(armX * 0.5, armHalf));
    float horizontalRight = sdBox(p - vec2(gap + armX * 0.5, 0.0), vec2(armX * 0.5, armHalf));
    return min(min(verticalTop, verticalBottom), min(horizontalLeft, horizontalRight));
}

void main() {
    vec2 screen = gl_FragCoord.xy;
    vec2 p = rotate(screen - uCentre);

    // 范围裁剪：顶点是全屏四边形，离准星很远的地方直接丢弃。
    // 距离上限把「臂长 × 最大放大 + 间隙 + 描边 + 反馈位移」全部算进去，
    // 并且在任何旋转角度下都成立（这里用的是到中心的半径，与旋转无关）。
    float reach = (max(uArmX, uArmY) + uGap) * uWaveScale + uBowOpen + uHalf + uOutline
            + uGapBoost + 16.0;
    if (length(p) > reach) {
        discard;
    }

    // 冲击：把 SDF 整体收缩，等效于臂向外偏移。
    // 距离以物理像素计，所以 1.5 的系数在任意 GUI 缩放下观感一致。
    float kick = uPunch * 1.5;
    float d = crossDistance(p, uHalf, uWaveScale);
    d -= kick;
    float aa = max(fwidth(d), 1.0 / 255.0);
    float coverage = 1.0 - smoothstep(-aa, aa, d);
    float body = coverage;

    vec3 rgb = uColour.rgb;
    float alpha = uColour.a * body;

    if (uFlowMode > 0.5) {
        float phase = motionPhase(uFlowPhase, int(uFlowMotion), screen * 0.01);
        vec3 flow = flowColour(phase, screen - uCentre, uFlowColour, uFlowDensity);

        if (uFlowMode > 1.5 && uFlowMode < 2.5) {
            float height01 = screen.y / max(uScreen.y, 1.0);
            float sweep = height01 - uFlowProgress;
            float mask = sweep <= 0.0 ? 1.0 : 1.0 - smoothstep(0.0, max(uFlowSoftness, 0.001), sweep);
            flow = mix(uColour.rgb, flow, mask);
        }

        if (uFlowMode > 2.5) {
            // 星芒（旧模式，保留兼容）：以准星中心为原点的柔光核与十字星。
            float radius = max(uGlintRadius, 1.0);
            vec2 delta = screen - uGlintScreen;
            float r = length(delta);
            float glow = exp(-(r * r) / (2.0 * radius * radius));
            float star = uGlintStar * exp(-r * 0.06)
                    * (0.5 + 0.5 * cos(atan(delta.y, delta.x) * 4.0));
            float intensity = (glow + star) * uGlintStrength;
            flow = min(flow + vec3(intensity), vec3(1.0));
        }

        rgb = flow;
        alpha = uColour.a * body * uFlowOpacity;
    }

    if (uOutline > 0.0) {
        float outer = 1.0 - smoothstep(-aa, aa, d - uOutline);
        float ring = max(outer - coverage, 0.0);
        rgb = mix(rgb, uOutlineColour.rgb, ring * uOutlineColour.a);
        alpha = max(alpha, uOutlineColour.a * ring);
    }

    if (uPlacePulse > 0.001) {
        // 放置光环：半径随状态扩张、透明度同步衰减。
        float radius = 2.0 + uPlacePulse * 10.0;
        float ring = abs(length(p) - radius);
        float ringCoverage = 1.0 - smoothstep(0.0, 1.2, ring);
        float ringAlpha = ringCoverage * uPlacePulse;
        rgb = mix(rgb, vec3(1.0), ringAlpha * 0.9);
        alpha = max(alpha, ringAlpha * 0.9);
    }

    if (uDot > 0.5) {
        float dotCoverage = 1.0 - smoothstep(-aa, aa, sdBox(p, vec2(uHalf)));
        rgb = mix(rgb, uDotColour.rgb, dotCoverage * uDotColour.a);
        alpha = max(alpha, uDotColour.a * dotCoverage);
    }

    if (alpha <= 0.004) {
        discard;
    }
    gl_FragColor = vec4(rgb, alpha);
}
