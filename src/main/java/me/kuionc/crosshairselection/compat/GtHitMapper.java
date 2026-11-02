package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.render.GtGridMath;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Maps a click inside our custom 3x3 GT overlay to the hit vector GT uses for
 * determineWrenchingSide(). The hit vector is sent in C08PacketPlayerBlockPlacement, so GT
 * performs the action on the intended face without a server-side mod.
 */
public final class GtHitMapper {

    private static final Class<?> META_BASE_ITEM = classOrNull("gregtech.api.items.MetaBaseItem");
    private static final java.lang.reflect.Method MAP_BEHAVIORS = methodOrNull(META_BASE_ITEM,
            "mapEachBehavior", ItemStack.class, java.util.function.Function.class);

    private GtHitMapper() {
    }

    public static boolean map(EntityPlayer player, World world, ItemStack stack, int x, int y, int z,
                              int side, Vec3 hitVec) {
        if (!GtOverlayConfig.enabled || player == null || world == null || stack == null
                || stack.getItem() == null || hitVec == null) {
            return true;
        }
        if (!GtOverlayCompat.isGtTarget(world, x, y, z, world.getBlock(x, y, z))) {
            return true;
        }
        if (!isGridTool(stack)) {
            return true;
        }

        double u = localU(side, hitVec, x, y, z);
        double v = localV(side, hitVec, x, y, z);
        int[] cell = GtGridMath.cellForHit(u, v, GtOverlayConfig.gridInset,
                0.0F, GtOverlayConfig.gridCellSize);
        if (cell == null) {
            return !GtOverlayConfig.strictGridClicks;
        }
        double[] canonical = GtGridMath.canonicalHit(cell[0], cell[1]);
        setHit(side, hitVec, x, y, z, canonical[0], canonical[1]);
        return true;
    }

    private static double localU(int side, Vec3 hit, int x, int y, int z) {
        switch (side) {
            case 0:
            case 1:
            case 2:
            case 3:
                return hit.xCoord - x;
            case 4:
            case 5:
            default:
                return hit.zCoord - z;
        }
    }

    private static double localV(int side, Vec3 hit, int x, int y, int z) {
        switch (side) {
            case 0:
            case 1:
                return hit.zCoord - z;
            default:
                return hit.yCoord - y;
        }
    }

    private static void setHit(int side, Vec3 hit, int x, int y, int z, double u, double v) {
        double hx;
        double hy;
        double hz;
        switch (side) {
            case 0:
                hx = x + u;
                hy = y;
                hz = z + v;
                break;
            case 1:
                hx = x + u;
                hy = y + 1.0D;
                hz = z + v;
                break;
            case 2:
                hx = x + u;
                hy = y + v;
                hz = z;
                break;
            case 3:
                hx = x + u;
                hy = y + v;
                hz = z + 1.0D;
                break;
            case 4:
                hx = x;
                hy = y + v;
                hz = z + u;
                break;
            case 5:
            default:
                hx = x + 1.0D;
                hy = y + v;
                hz = z + u;
                break;
        }
        hit.xCoord = hx;
        hit.yCoord = hy;
        hit.zCoord = hz;
    }

    private static boolean isGridTool(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        if (META_BASE_ITEM == null || MAP_BEHAVIORS == null || !META_BASE_ITEM.isInstance(stack.getItem())) {
            return false;
        }
        try {
            java.util.function.Function<Object, Object> identity = new java.util.function.Function<Object, Object>() {
                @Override
                public Object apply(Object value) {
                    return value;
                }
            };
            Object result = MAP_BEHAVIORS.invoke(stack.getItem(), stack, identity);
            if (result instanceof Iterable) {
                for (Object behavior : (Iterable<?>) result) {
                    if (behavior == null) {
                        continue;
                    }
                    String name = behavior.getClass().getName();
                    if (name.endsWith("BehaviourWrench")
                            || name.endsWith("BehaviourCoverTool")
                            || name.endsWith("BehaviourWireCutter")
                            || name.endsWith("BehaviourScrewdriver")) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fall through.
        }
        try {
            java.lang.reflect.Method getToolStats = stack.getItem().getClass().getMethod("getToolStats", ItemStack.class);
            Object stats = getToolStats.invoke(stack.getItem(), stack);
            if (stats == null) {
                return false;
            }
            String name = stats.getClass().getName();
            return name.contains("ToolWrench") || name.contains("ToolWireCutter")
                    || name.contains("ToolSolderingIron") || name.contains("ToolScrewdriver");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static java.lang.reflect.Method methodOrNull(Class<?> owner, String name, Class<?>... args) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
