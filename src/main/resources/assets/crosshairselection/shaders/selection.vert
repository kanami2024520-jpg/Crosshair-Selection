#version 120

// 准星/选框的顶点着色器（GLSL 1.20，兼容 GL2.1 固定管线矩阵）。
//
// 顶点数据由 GTNHLib 的 DirectTessellator 提供，格式为 POSITION_TEXTURE：
//   - 四边形的四个角通过 gl_Vertex 送达；
//   - 纹理坐标只有 2 个分量能用，塞不下「两个三维端点」，
//     所以 UV 里只放这条边的编号，真正的端点由片段着色器从 uniform 数组里查。
//
// 这里不做任何屏幕空间展开。四边形在 Java 端就已经按真实世界尺寸构造好，
// 顶点阶段若再去缩放/拼装，线段一旦与屏幕轴平行就会退化成一条线而整条消失。

varying float vEdge;

void main() {
    // 位置直接由固定管线矩阵算，与光栅化用的是同一套矩阵（矩阵本身在 Java 端也拼了一份给片段阶段用）。
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

    // 边编号随四边形插值，四个角的 UV 相同，因此整片片元的 vEdge 都等于该边的下标。
    vEdge = gl_MultiTexCoord0.x;
}
