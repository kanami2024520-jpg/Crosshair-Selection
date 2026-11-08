package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.config.SelectionBoxConfig;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static me.kuionc.crosshairselection.compat.Reflect.*;

/**
 * Applied Energistics 电缆总线。
 *
 * <p>一个电缆总线里可以同时有中心线缆、六个方向的部件（ME 面板、终端、总线……）
 * 和伪装板。这里只描<b>准星实际指着的那一个</b>。
 *
 * <p>选择顺序：
 * <ol>
 *   <li>优先调用 AE 自己的 {@code TileCableBus.selectPart}，它已经处理了部件叠放顺序、
 *       {@code CableRenderMode} 和连接臂；它给出 part / facade 就直接用。</li>
 *   <li>AE 没给出结果时，才自己枚举候选（中心线缆 + 6 个侧面部件 + 6 个伪装板），
 *       按命中点到盒面的距离取最近的一个。</li>
 *   <li>最坏情况只描离命中点最近的那一个盒子。</li>
 * </ol>
 *
 * <p><b>绝不</b>退回「整条总线的并集」：那会把线缆和终端/面板一起描出来，
 * 正是之前「两个都选上」的来源。
 *
 * <h3>踩坑记录：Forge Multipart 会把 AE 总线“吃掉”</h3>
 * AE2 通过 {@code appeng.fmp.CableBusPart} 支持 Forge Multipart。只要同一个方块里
 * 放进任意一个 FMP 部件——最常见的触发方式就是 <b>ProjectRed 的红色合金丝</b>——
 * AE 的 {@code TileCableBus} 就会被转换成 {@code TileMultipart} 里的 {@code CableBusPart}。
 *
 * <p>如果这里只认 {@code TileCableBus}，这个方块会落到 {@link FmpShapeProvider}；
 * 而 {@code CableBusPart.getCollisionBoxes()} 返回的是<b>整条总线</b>的盒子，
 * 于是线缆和终端会被一起描出来，而且<b>拆掉红合金丝也不会恢复</b>——AE 仍然是 FMP 部件，
 * 必须把 AE 部件敲掉重放才会变回普通方块。
 *
 * <p>所以这里必须同时接受两种载体，并用 {@code HitData.partIndex} 找到容器里
 * 命中的那个 {@code CableBusPart}，再走同一套选择流程。
 */
public final class Ae2ShapeProvider implements ShapeProvider {

    private static final Class<?> CABLE_TILE = classOrNull("appeng.tile.networking.TileCableBus");
    /** AE 总线被 Forge Multipart 接管时的载体（和 PR 线等部件同处一格）。 */
    private static final Class<?> MULTIPART_TILE = classOrNull("codechicken.multipart.TileMultipart");
    private static final Class<?> AE_FMP_PART = classOrNull("appeng.fmp.CableBusPart");
    private static final Method PART_LIST = methodOrNull(MULTIPART_TILE, "jPartList");
    private static final Class<?> BUS = classOrNull("appeng.parts.CableBusContainer");
    private static final Class<?> SELECTED_PART = classOrNull("appeng.api.parts.SelectedPart");
    private static final Class<?> FACADE_CONTAINER = classOrNull("appeng.api.parts.IFacadeContainer");
    private static final Class<?> IPART = classOrNull("appeng.api.parts.IPart");
    private static final Class<?> IFACADE_PART = classOrNull("appeng.api.parts.IFacadePart");
    private static final Class<?> COLLISION_HELPER = classOrNull("appeng.api.parts.IPartCollisionHelper");
    private static final Class<?> BUS_HELPER = classOrNull("appeng.parts.BusCollisionHelper");

    private static final Method GET_BUS = methodOrNull(CABLE_TILE, "getCableBus");
    private static final Method SELECT_PART = methodOrNull(CABLE_TILE, "selectPart", Vec3.class);
    private static final Method FMP_GET_BUS = methodOrNull(AE_FMP_PART, "getCableBus");
    private static final Method FMP_SELECT_PART = methodOrNull(AE_FMP_PART, "selectPart", Vec3.class);
    private static final Method GET_PART = methodOrNull(BUS, "getPart", ForgeDirection.class);
    private static final Method GET_FACADE_CONTAINER = methodOrNull(BUS, "getFacadeContainer");
    private static final Method GET_FACADE = methodOrNull(FACADE_CONTAINER, "getFacade",
            ForgeDirection.class);
    private static final Method PART_BOXES = methodOrNull(IPART, "getBoxes", COLLISION_HELPER);
    private static final Method FACADE_BOXES = methodOrNull(IFACADE_PART, "getBoxes",
            COLLISION_HELPER, Entity.class);
    private static final Constructor<?> HELPER_CONSTRUCTOR = constructorOrNull(BUS_HELPER,
            List.class, ForgeDirection.class, Entity.class, boolean.class);

    private static final Field PART_FIELD = fieldOrNull(SELECTED_PART, "part");
    private static final Field FACADE_FIELD = fieldOrNull(SELECTED_PART, "facade");
    private static final Field SIDE_FIELD = fieldOrNull(SELECTED_PART, "side");

    /** 诊断日志去重：同一条消息 3 秒内只打印一次，避免每帧刷屏。 */
    private static String lastLog;
    private static long lastLogAt;

    @Override
    public boolean collect(World world, int x, int y, int z, Block block,
                           MovingObjectPosition target, List<OutlineShape> out) {
        if (CABLE_TILE == null || GET_BUS == null || GET_PART == null
                || HELPER_CONSTRUCTOR == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);

        // AE 总线有两种载体：
        //   - 普通方块：TileCableBus；
        //   - 同一格里还有 Forge Multipart 部件（例如 ProjectRed 红色合金丝）时，
        //     AE 会把它转换成 TileMultipart 里的 CableBusPart。
        // 只认第一种的话，第二种会落到 FmpShapeProvider，而 CableBusPart 的碰撞盒
        // 是整条总线，于是线缆和终端会被一起描出来。
        Object bus = null;
        Object selectionSource = null;
        if (CABLE_TILE != null && CABLE_TILE.isInstance(tile)) {
            selectionSource = tile;
        } else if (MULTIPART_TILE != null && AE_FMP_PART != null && MULTIPART_TILE.isInstance(tile)) {
            selectionSource = hitAeMultipart(tile, target);
        }
        if (selectionSource == null) {
            return false;
        }
        try {
            bus = CABLE_TILE != null && CABLE_TILE.isInstance(selectionSource)
                    ? GET_BUS.invoke(selectionSource)
                    : FMP_GET_BUS.invoke(selectionSource);
        } catch (Throwable ignored) {
            return false;
        }
        if (bus == null) {
            return false;
        }

        Vec3 localHit = target == null || target.hitVec == null ? null
                : Vec3.createVectorHelper(target.hitVec.xCoord - x,
                target.hitVec.yCoord - y, target.hitVec.zCoord - z);

        // 1) AE 自己的判定优先。
        List<AxisAlignedBB> chosen = fromAeSelection(selectionSource, localHit);
        if (chosen != null && addOffsetBoxes(chosen, x, y, z, out)) {
            return true;
        }

        // 2) 自己枚举候选，取距离最近的一个部件。
        Candidate best = scanCandidates(bus, localHit);
        if (best != null && addOffsetBoxes(best.boxes, x, y, z, out)) {
            return true;
        }

        // 3) 最坏情况：只描离命中点最近的那一个盒子，绝不合并整条总线。
        AxisAlignedBB nearest = nearestBox(bus, localHit);
        if (nearest != null) {
            List<AxisAlignedBB> single = new ArrayList<AxisAlignedBB>(1);
            single.add(nearest);
            return addOffsetBoxes(single, x, y, z, out);
        }
        return false;
    }

    /** 在 Forge Multipart 容器里找命中的 AE 总线部件。 */
    private static Object hitAeMultipart(TileEntity tile, MovingObjectPosition target) {
        if (PART_LIST == null || AE_FMP_PART == null) {
            return null;
        }
        try {
            List<?> parts = (List<?>) PART_LIST.invoke(tile);
            if (parts == null || parts.isEmpty()) {
                return null;
            }
            int index = HitData.partIndex(target);
            if (index >= 0 && index < parts.size()) {
                Object part = parts.get(index);
                return AE_FMP_PART.isInstance(part) ? part : null;
            }
            // 射线结果没带 part 序号时，只有在容器里恰好有一个 AE 总线部件才敢接手。
            Object found = null;
            for (Object part : parts) {
                if (!AE_FMP_PART.isInstance(part)) {
                    continue;
                }
                if (found != null) {
                    return null;
                }
                found = part;
            }
            return found;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 调用 AE 的 {@code selectPart}；它给出 part 或 facade 时返回对应盒子。 */
    private static List<AxisAlignedBB> fromAeSelection(Object source, Vec3 localHit) {
        if (SELECTED_PART == null || localHit == null) {
            return null;
        }
        Method selectPart = CABLE_TILE != null && CABLE_TILE.isInstance(source)
                ? SELECT_PART : FMP_SELECT_PART;
        if (selectPart == null) {
            return null;
        }
        try {
            Object selected = selectPart.invoke(source, localHit);
            if (selected == null) {
                return null;
            }
            // 没选中时 AE 返回的是字段全 null 的对象，不是 null。
            Object part = PART_FIELD != null ? PART_FIELD.get(selected) : null;
            Object facade = FACADE_FIELD != null ? FACADE_FIELD.get(selected) : null;
            Object sideValue = SIDE_FIELD != null ? SIDE_FIELD.get(selected) : null;
            ForgeDirection side = sideValue instanceof ForgeDirection
                    ? (ForgeDirection) sideValue : ForgeDirection.UNKNOWN;
            if (part != null) {
                List<AxisAlignedBB> boxes = boxesForPart(part, side);
                log("selectPart -> part " + part.getClass().getSimpleName()
                        + " side=" + side + " boxes=" + size(boxes));
                return boxes;
            }
            if (facade != null) {
                List<AxisAlignedBB> boxes = boxesForFacade(facade, side);
                log("selectPart -> facade " + facade.getClass().getSimpleName()
                        + " side=" + side + " boxes=" + size(boxes));
                return boxes;
            }
            log("selectPart -> empty");
        } catch (Throwable ignored) {
            log("selectPart -> failed");
        }
        return null;
    }

    private static Candidate scanCandidates(Object bus, Vec3 localHit) {
        Candidate best = null;
        for (ForgeDirection side : ForgeDirection.values()) {
            best = better(best, candidateForPart(bus, side, localHit));
            if (side != ForgeDirection.UNKNOWN) {
                best = better(best, candidateForFacade(bus, side, localHit));
            }
        }
        return best;
    }

    private static Candidate candidateForPart(Object bus, ForgeDirection side, Vec3 localHit) {
        try {
            Object part = GET_PART.invoke(bus, side);
            if (part == null || PART_BOXES == null) {
                return null;
            }
            List<AxisAlignedBB> boxes = boxesForPart(part, side);
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
            return hitTest(boxesForFacade(facade, side), localHit, true);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<AxisAlignedBB> boxesForPart(Object part, ForgeDirection side) {
        try {
            List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
            Object helper = HELPER_CONSTRUCTOR.newInstance(boxes, side, null, true);
            PART_BOXES.invoke(part, helper);
            return boxes.isEmpty() ? null : boxes;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<AxisAlignedBB> boxesForFacade(Object facade, ForgeDirection side) {
        if (FACADE_BOXES == null) {
            return null;
        }
        try {
            List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
            Object helper = HELPER_CONSTRUCTOR.newInstance(boxes, side, null, true);
            // 传玩家而不是 null：AE2 对 null 实体返回 0.1/16 的碰撞薄片，
            // 传 EntityLivingBase 才是 14/16 的可视板。
            FACADE_BOXES.invoke(facade, helper, player());
            return boxes.isEmpty() ? null : boxes;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 所有候选盒子里离命中点最近的那一个；只用于最坏情况。 */
    private static AxisAlignedBB nearestBox(Object bus, Vec3 localHit) {
        if (localHit == null) {
            return null;
        }
        AxisAlignedBB nearest = null;
        double best = Double.MAX_VALUE;
        for (ForgeDirection side : ForgeDirection.values()) {
            List<AxisAlignedBB> boxes = null;
            try {
                Object part = GET_PART.invoke(bus, side);
                if (part != null) {
                    boxes = boxesForPart(part, side);
                }
            } catch (Throwable ignored) {
                // 继续看下一个方向。
            }
            if (boxes != null) {
                for (AxisAlignedBB box : boxes) {
                    double distance = distanceToBox(box, localHit);
                    if (distance < best) {
                        best = distance;
                        nearest = box;
                    }
                }
            }
        }
        log("nearest fallback -> " + (nearest == null ? "none" : "one box"));
        return nearest;
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

    /**
     * 命中评分：取命中点到该部件所有盒子的最小距离（在盒内为 0）。
     */
    private static Candidate hitTest(List<AxisAlignedBB> boxes, Vec3 localHit, boolean sidePart) {
        if (boxes == null || boxes.isEmpty() || localHit == null) {
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

    /** 本地玩家；伪装板需要它才会返回 14/16 的可视板而不是碰撞薄片。 */
    private static Entity player() {
        net.minecraft.client.Minecraft minecraft = McRef.get();
        return minecraft == null ? null : minecraft.thePlayer;
    }

    /**
     * 把一组局部碰撞盒转成世界坐标，并按<b>一个</b>联合外轮廓输出。
     *
     * <p>不能每盒各画一个框：ME 线的碰撞盒本来就是「中心核 + 每条连接臂」，
     * 逐个画会在交界面上多出完整的 12 条边，看起来是「3 截框 + 2 处交叉」。
     */
    private static boolean addOffsetBoxes(Iterable<?> boxes, int x, int y, int z,
                                          List<OutlineShape> out) {
        if (boxes == null) {
            return false;
        }
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

    private static int size(List<AxisAlignedBB> boxes) {
        return boxes == null ? 0 : boxes.size();
    }

    /** 诊断：由 {@code selection_box.debugPartSelection} 控制，默认关闭。 */
    private static void log(String message) {
        if (!SelectionBoxConfig.debugPartSelection) {
            return;
        }
        long now = System.currentTimeMillis();
        if (message.equals(lastLog) && now - lastLogAt < 3000L) {
            return;
        }
        lastLog = message;
        lastLogAt = now;
        System.err.println("[Crosshair-Selection][ae] " + message);
    }

}
