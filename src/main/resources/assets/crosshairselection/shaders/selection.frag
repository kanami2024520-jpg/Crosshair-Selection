#version 120

// 选框边框的片段着色器。
//
// 几何是一个朝向相机的四边形，本身已经有正确的宽度与长度；这里只负责把四边形裁剪成
// 一条「胶囊形」的线：到线段的距离决定覆盖率，两端因此是半圆头。
//
// 为什么端点从 uniform 数组取、而不是随顶点一起传：
//   GTNHLib 的 POSITION_TEXTURE 只有 2 个 UV 分量，而一条边需要 6 个浮点（两个三维端点）。
//   顶点阶段因此只传边编号，这里按编号回查端点。
//
// 为什么要自己算屏幕坐标而不是用 vA/vB 插值：
//   四边形是透视投影后的世界几何，直接插值端点会在透视下产生偏差；
//   用「与光栅化相同的矩阵」在片元里重新投影两个端点，得到的线段才与几何严格对齐。

uniform mat4 uModelViewProjection;  // Java 端用 PROJECTION × MODELVIEW 拼好后上传
uniform vec2 uViewport;             // 物理像素尺寸，用于把 NDC 换成 gl_FragCoord 的坐标系

uniform vec3 uEdgeA[64];            // 相机相对坐标的端点，按 vEdge 索引
uniform vec3 uEdgeB[64];            // 数组长度必须与 RibbonBuffer.MAX_SEGMENTS 一致

uniform float uHalfWidthPixels;     // 线宽的一半（物理像素）
uniform vec4 uColour;               // 基色，alpha 为整体透明度

uniform vec3 uCamera;               // 相机世界坐标，用于把端点还原成世界坐标做色带

// 流光参数。GT 覆盖层的边框会把这些值交给「无流光」的默认设置，
// 因此网格本身不会跟着选框一起流动。
uniform float uFlowMode;
uniform vec4 uFlowColour;           // (色相起点, 色相范围, 饱和度, 亮度)
uniform float uFlowPhase;
uniform float uFlowMotion;
uniform float uFlowDensity;
uniform float uFlowProgress;
uniform float uFlowSoftness;
uniform vec3 uSweepAxis;
uniform float uSweepCentreOut;

// 流光/扫光坐标系：有了原点和投影范围，「角到角」才能真正沿选中的两个角走，
// 扫光进度也才能和形状范围对齐（以前 dot(world, axis) 是世界坐标，和 0..1 的进度对不上）。
uniform vec3 uFlowAxis;
uniform vec3 uFlowOrigin;
uniform float uSweepMin;
uniform float uSweepMax;
uniform float uSweepRadius;

uniform float uDebugSolid;          // 预留的调试开关（Java 端会上传，当前不改变输出）

varying float vEdge;

// 便宜的哈希，给柏林噪声用。
float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// 值噪声，用于「柏林噪声」运动模式。
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

// 时间曲线。mode 与 FlowSettings.Motion 的序号一致。
// 噪声模式特意沿对角线缓慢推进并把相位折成三角波：直接把 0..1 相位乘进噪声坐标
// 会让一个周期只跨几个噪声格，观感是整格跳变。
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

/** 流光/扫光的取样坐标：角到角沿方向轴投影，中心向外用半径。 */
float flowCoord(vec3 world) {
    if (uSweepCentreOut > 0.5) {
        return length(world - uFlowOrigin);
    }
    return dot(world - uFlowOrigin, uFlowAxis);
}

/** 给定空间位置返回流光颜色。 */
vec3 flowSample(vec3 world) {
    float phase = motionPhase(uFlowPhase, int(uFlowMotion), world.xz * 40.0);
    float coord = flowCoord(world);
    vec3 flow = hsvToRgb(vec3(
            fract(uFlowColour.x + fract(coord * uFlowDensity + phase) * uFlowColour.y),
            uFlowColour.z, uFlowColour.w));

    if (uFlowMode > 1.5) {
        // 扫光：把投影归一化到 0..1，进度才和形状范围对齐。
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
    int index = int(clamp(vEdge + 0.5, 0.0, 63.0));
    vec3 wa = uEdgeA[index];
    vec3 wb = uEdgeB[index];

    // 用与光栅化相同的矩阵把端点投影到屏幕像素坐标。
    // 透视除法前先取绝对值再乘符号，避免 w 为负时 NaN。
    vec4 clipA = uModelViewProjection * vec4(wa, 1.0);
    vec4 clipB = uModelViewProjection * vec4(wb, 1.0);
    vec2 a = (clipA.xy / max(abs(clipA.w), 1.0E-4) * sign(clipA.w) * 0.5 + 0.5) * uViewport;
    vec2 b = (clipB.xy / max(abs(clipB.w), 1.0E-4) * sign(clipB.w) * 0.5 + 0.5) * uViewport;

    // 胶囊体距离场：把投影参数 clamp 到 0..1，超出端点的部分就锁在端点上，
    // 于是线段两端各得到一个半径 = 线宽一半的圆头。
    // 相邻两边在角点处的圆头会自然重叠，所以 Java 端不需要再把端点外延。
    vec2 ba = b - a;
    vec2 pa = gl_FragCoord.xy - a;
    float lenSq = max(dot(ba, ba), 1.0E-4);
    float t = clamp(dot(pa, ba) / lenSq, 0.0, 1.0);
    float dist = length(pa - ba * t);

    // fwidth 给出屏幕空间变化率，用它在 1 像素内做过渡即为精确抗锯齿。
    // 下限 1/255 防止极细线在 fwidth 为 0 时整片被裁掉。
    float aa = max(fwidth(dist), 1.0 / 255.0);
    float coverage = 1.0 - smoothstep(uHalfWidthPixels - aa, uHalfWidthPixels + aa, dist);
    float body = coverage;
    if (body <= 0.004) {
        discard;
    }

    // 色带按世界坐标算，所以走动视角时颜色不会跟着屏幕漂移。
    vec3 world = mix(wa, wb, t) + uCamera;
    vec3 rgb = uColour.rgb;

    if (uFlowMode > 0.5) {
        rgb = flowSample(world);
    }

    gl_FragColor = vec4(rgb, uColour.a * body);
}
