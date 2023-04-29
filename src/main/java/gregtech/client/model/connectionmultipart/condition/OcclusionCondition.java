package gregtech.client.model.connectionmultipart.condition;

import gregtech.common.blocks.extendedstate.ConnectionState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

import javax.annotation.Nonnull;

final class OcclusionCondition extends ConnectionVariantCondition {

    private final EnumFacing side;
    private final boolean shown;
    private final Vec3i offset;

    OcclusionCondition(@Nonnull EnumFacing side, boolean shown, @Nonnull Vec3i offset) {
        this.side = side;
        this.shown = shown;
        this.offset = offset;
    }

    @Override
    public boolean matches(@Nonnull ConnectionState state) {
        return state.isSideShown(state.getPos().add(this.offset), this.side) == this.shown;
    }
}
