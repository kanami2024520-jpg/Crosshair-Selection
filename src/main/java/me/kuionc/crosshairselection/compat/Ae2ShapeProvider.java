package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import me.kuionc.crosshairselection.util.McRef;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Applied Energistics 电缆总线。
 *
 * <p>一个电缆总线里可以同时有中心线缆、六个方向的部件（ME 面板、终端、总线……）
 * 和伪装板。这里只描<b>准星实际指着的那一个</b>：自己枚举候选，用视线做命中测试，
 * 取入射距离最近的那个。
 *
 * <p>为什么不直接用 {@code TileCableBus.selectPart}：
 * <ul>
 *   <li>它没命中时返回的是字段全 null 的 {@code SelectedPart} 而不是 null，
 *       调用方很容易误以为「选中了但没盒子」；</li>
 *   <li>它受 {@code CableRenderMode.opaqueFacades} 影响，某些模式下根本不检查伪装板；</li>
 *   <li>它的顺序是「先 part 后 facade」，同一格里线缆连接臂和面板板子重叠时
 *       选中的不一定是视觉上最近的那个。</li>
 * </ul>
 * 任何一条都会让调用方退回「整条总线」的并集，表现为线缆和面板被同时描边。
 */
public final class Ae2ShapeProvider implements ShapeProvider {

    private static final Class<?> CABLE_TILE = classOrNull("appeng.tile.networking.TileCableBus");
    private static final Class<?> BUS = classOrNull("appeng.parts.CableBusContainer");
    private static final Class<?> FACADE_CONTAINER = classOrNull("appeng.api.parts.IFacadeContainer");
    private static final Class<?> IPART = classOrNull("appeng.api.parts.IPart");
    private static final Class<?> IFACADE_PART = classOrNull("appeng.api.parts.IFacadePart");
    private static final Class<?> COLLISION_HELPER = classOrNull("appeng.api.parts.IPartCollisionHelper");
    private static final Class<?> BUS_HELPER = classOrNull("appeng.parts.BusCollisionHelper");

    private static final Method GET_BUS = methodOrNull(CABLE_TILE, "getCableBus");
    private static final Method GET_ALL_BOXES = methodOrNull(CABLE_TILE,
            "getSelectedBoundingBoxesFromPool",
            World.class, int.class, int.class, int.class, Entity.class, boolean.class);
    private static final Method GET_PART = methodOrNull(BUS, "getPart", ForgeDirection.class);
    private static final Method GET_FACADE_CONTAINER = methodOrNull(BUS, "getFacadeContainer");
    private static final Method GET_FACADE = methodOrNull(FACADE_CONTAINER, "getFacade",
            ForgeDirection.class);
    private static final Method PART_BOXES = methodOrNull(IPART, "getBoxes", COLLISION_HELPER);
    private static final Method FACADE_BOXES = methodOrNull(IFACADE_PART, "getBoxes",
            COLLISION_HELPER, Entity.class);
    private static final Constructor<?> HELPER_CONSTRUCTOR = constructorOrNull(BUS_HELPER,
            List.class, ForgeDirection.class, Entity.class, boolean.class);

    @Override
    public boolean collect(World world, int x, int y, int z, Block block,
                           MovingObjectPosition target, List<OutlineShape> out) {
        if (CABLE_TILE == null || GET_BUS == null || GET_PART == null
                || HELPER_CONSTRUCTOR == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!CABLE_TILE.isInstance(tile)) {
            return false;
        }
        Object bus;
        try {
            bus = GET_BUS.invoke(tile);
        } catch (Throwable ignored) {
            return false;
        }
        if (bus == null) {
            return false;
        }

        Vec3 localHit = target == null || target.hitVec == null ? null
                : Vec3.createVectorHelper(target.hitVec.xCoord - x,
                target.hitVec.yCoord - y, target.hitVec.zCoord - z);

        Candidate best = null;
        for (ForgeDirection side : ForgeDirection.values()) {
            // 中心线缆（UNKNOWN）也要参与，但优先级低于侧面部件。
            best = better(best, candidateForPart(bus, side, localHit));
            if (side != ForgeDirection.UNKNOWN) {
                best = better(best, candidateForFacade(bus, side, localHit));
            }
        }
        if (best != null) {
            return addOffsetBoxes(best.boxes, x, y, z, out);
        }

        // 什么都没命中（例如正对着空气的边缘）：退回整条总线的形状，
        // 保证至少有一个可见的选框，而不是突然什么都没有。
        return collectAllBoxes(tile, world, x, y, z, out);
    }

    /** 一个候选部件及其（已做命中测试的）盒子。 */
    private static final class Candidate {
        final List<AxisAlignedBB> boxes;
        final double entry;
        final boolean sidePart;

        Candidate(List<AxisAlignedBB> boxes, double entry, boolean sidePart) {
            this.boxes = boxes;
            this.entry = entry;
            this.sidePart = sidePart;
        }
    }

    private static Candidate candidateForPart(Object bus, ForgeDirection side, Vec3 localHit) {
        try {
            Object part = GET_PART.invoke(bus, side);
            if (part == null || PART_BOXES == null) {
                return null;
            }
            List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
            Object helper = HELPER_CONSTRUCTOR.newInstance(boxes, side, null, true);
            PART_BOXES.invoke(part, helper);
            return hitTest(boxes, localHit, side != ForgeDirection.UNKNOWN);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Candidate candidateForFacade(Object bus, ForgeDirection side, Vec3 localHit) {
        if (GET_FACADE_CONTAINER == null || GET_FACADE == null || FACADE_BOXES == null) {
            return null;
        }
        try {
            Object container = GET_FACADE_CONTAINER.invoke(bus);
            if (container == null) {
                return null;
            }
            Object facade = GET_FACADE.invoke(container, side);
            if (facade == null) {
                return null;
            }
            List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
            Object helper = HELPER_CONSTRUCTOR.newInstance(boxes, side, null, true);
            // 传玩家而不是 null：AE2 对 null 实体返回 0.1/16 的碰撞薄片，
            // 传 EntityLivingBase 才是 14/16 的可视板，描边才不会细成一条缝。
            FACADE_BOXES.invoke(facade, helper, player());
            return hitTest(boxes, localHit, true);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 本地玩家；伪装板需要它才会返回 14/16 的可视板而不是碰撞薄片。 */
    private static Entity player() {
        net.minecraft.client.Minecraft minecraft = McRef.get();
        return minecraft == null ? null : minecraft.thePlayer;
    }

    /**
     * 命中评分：取命中点到该部件所有盒子的最小距离（在盒内为 0）。
     *
     * <p>以前要求「命中点必须严格落在盒内」，只要射线端点稍微跑到盒外一点
     * （玻璃线 + 样板终端这种组合常见），所有候选都会被判为未命中，
     * 于是退回整条总线的并集，表现成「面板和线一起被选上」。
     * 现在改为按距离取最近：哪怕点在盒外，也只会选中离它最近的那一个部件。
     */
    private static Candidate hitTest(List<AxisAlignedBB> boxes, Vec3 localHit, boolean sidePart) {
        if (boxes.isEmpty() || localHit == null) {
            return null;
        }
        double nearest = Double.MAX_VALUE;
        for (AxisAlignedBB box : boxes) {
            nearest = Math.min(nearest, distanceToBox(box, localHit));
        }
        return new Candidate(boxes, nearest, sidePart);
    }

    /** 点到 AABB 的距离；点在盒内时为 0。 */
    private static double distanceToBox(AxisAlignedBB box, Vec3 point) {
        double dx = Math.max(0.0D, Math.max(box.minX - point.xCoord, point.xCoord - box.maxX));
        double dy = Math.max(0.0D, Math.max(box.minY - point.yCoord, point.yCoord - box.maxY));
        double dz = Math.max(0.0D, Math.max(box.minZ - point.zCoord, point.zCoord - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 选出更好的候选：先看谁离命中点更近；距离几乎相同时（面板和线缆臂都延伸到方块边界），
     * 优先侧面部件而不是中心线缆。
     */
    private static Candidate better(Candidate current, Candidate next) {
        if (next == null) {
            return current;
        }
        if (current == null) {
            return next;
        }
        if (next.entry < current.entry - 1.0E-4D) {
            return next;
        }
        if (current.entry < next.entry - 1.0E-4D) {
            return current;
        }
        return next.sidePart && !current.sidePart ? next : current;
    }

    private static boolean collectAllBoxes(TileEntity tile, World world, int x, int y, int z,
                                           List<OutlineShape> out) {
        if (GET_ALL_BOXES == null) {
            return false;
        }
        try {
            Object value = GET_ALL_BOXES.invoke(tile, world, x, y, z, null, false);
            return value instanceof Iterable && addOffsetBoxes((Iterable<?>) value, x, y, z, out);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 把一组局部碰撞盒转成世界坐标，并按<b>一个</b>联合外轮廓输出。
     *
     * <p>不能每盒各画一个框：ME 线的碰撞盒本来就是「中心核 + 每条连接臂」，
     * 逐个画会在交界面上多出完整的 12 条边，看起来是「3 截框 + 2 处交叉」。
     * {@link OutlineShape#union} 只会保留法线发生变化的外表面，内部接缝自动消失，
     * 和 GT 管道、FMP、ProjectRed 等部件的处理方式一致。
     */
    private static boolean addOffsetBoxes(Iterable<?> boxes, int x, int y, int z,
                                          List<OutlineShape> out) {
        List<AxisAlignedBB> world = new ArrayList<AxisAlignedBB>();
        for (Object object : boxes) {
            if (!(object instanceof AxisAlignedBB)) {
                continue;
            }
            AxisAlignedBB box = (AxisAlignedBB) object;
            if (box.maxX - box.minX <= 0.0001D || box.maxY - box.minY <= 0.0001D
                    || box.maxZ - box.minZ <= 0.0001D) {
                continue;
            }
            world.add(AxisAlignedBB.getBoundingBox(
                    box.minX + x, box.minY + y, box.minZ + z,
                    box.maxX + x, box.maxY + y, box.maxZ + z));
        }
        if (world.isEmpty()) {
            return false;
        }
        out.add(OutlineShape.union(world));
        return true;
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Constructor<?> constructorOrNull(Class<?> owner, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getConstructor(params);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
