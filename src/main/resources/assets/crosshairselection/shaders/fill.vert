#version 120

// 选框内部填充的顶点着色器。
//
// 顶点是相机相对坐标（与描边一致），这里只做标准变换，
// 并把世界坐标传给片段阶段做色带计算，避免走动视角时颜色跟着屏幕漂移。

uniform vec3 uCamera;

varying vec3 vWorld;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    vWorld = gl_Vertex.xyz + uCamera;
}
