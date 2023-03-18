package gregtech.client.utils;

import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import gregtech.api.util.GTUtility;
import gregtech.client.model.pipeline.VertexLighterFlatSpecial;
import gregtech.client.model.pipeline.VertexLighterSmoothAoSpecial;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class PipelineUtil {

    public static final ThreadLocal<VertexLighterFlat> LIGHTER_FLAT = ThreadLocal.withInitial(() -> new VertexLighterFlatSpecial(Minecraft.getMinecraft().getBlockColors()));
    public static final ThreadLocal<VertexLighterFlat> LIGHTER_SMOOTH = ThreadLocal.withInitial(() -> new VertexLighterSmoothAoSpecial(Minecraft.getMinecraft().getBlockColors()));

    @Nonnull
    public static IVertexOperation[] color(@Nonnull IVertexOperation[] ops, int rgbColor) {
        return ArrayUtils.add(ops, new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(rgbColor)));
    }

    /**
     * Create a new array of operations with {@code op} as first element and {@code ops} following after it.
     *
     * @param op  First operation
     * @param ops Operations
     * @return New array of operations
     */
    @Nonnull
    public static IVertexOperation[] concat(@Nonnull IVertexOperation op, @Nonnull IVertexOperation[] ops) {
        IVertexOperation[] array = new IVertexOperation[ops.length + 1];
        array[0] = op;
        System.arraycopy(ops, 0, array, 1, ops.length);
        return array;
    }

    /**
     * Create a new array of operations with {@code op} as last element and {@code ops} preceding before it.
     *
     * @param ops Operations
     * @param op  Last operation
     * @return New array of operations
     */
    @Nonnull
    public static IVertexOperation[] concat(@Nonnull IVertexOperation[] ops, @Nonnull IVertexOperation op) {
        IVertexOperation[] array = new IVertexOperation[ops.length + 1];
        array[array.length - 1] = op;
        System.arraycopy(ops, 0, array, 0, ops.length);
        return array;
    }

    @Nonnull
    public static VertexLighterFlat getVertexLighter(@Nonnull IVertexConsumer parent, boolean enableAmbientOcclusion) {
        boolean renderAO = Minecraft.isAmbientOcclusionEnabled() && enableAmbientOcclusion;
        VertexLighterFlat lighter = renderAO ? LIGHTER_SMOOTH.get() : LIGHTER_FLAT.get();

        lighter.setParent(parent);
        return lighter;
    }
}
