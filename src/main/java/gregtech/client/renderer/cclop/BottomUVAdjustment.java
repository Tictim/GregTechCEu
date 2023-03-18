package gregtech.client.renderer.cclop;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.vec.uv.UV;
import codechicken.lib.vec.uv.UVTransformation;
import net.minecraft.util.EnumFacing;

/**
 * Flips V position of bottom face to match the vanilla texturing.
 */
public final class BottomUVAdjustment extends UVTransformation {

    public static final BottomUVAdjustment INSTANCE = new BottomUVAdjustment();

    private BottomUVAdjustment() {}

    @Override
    public void operate(CCRenderState state) {
        if (state.side == EnumFacing.DOWN.getIndex()) {
            apply(state.vert.uv);
        }
        state.sprite = null;
    }

    @Override
    public void apply(UV uv) {
        uv.v = 1 - uv.v;
    }

    @Override
    public UVTransformation inverse() {
        return this;
    }
}
