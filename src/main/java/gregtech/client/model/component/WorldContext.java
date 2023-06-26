package gregtech.client.model.component;

import gregtech.common.blocks.special.ISpecialState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

public final class WorldContext {

    @Nonnull
    public final IBlockAccess world;
    @Nonnull
    public final BlockPos pos;

    private final MutableBlockPos mpos = new MutableBlockPos();

    public WorldContext(@Nonnull ISpecialState state) {
        this.world = state.getWorld();
        this.pos = state.getPos();
    }

    public WorldContext(@Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    /**
     * @return instance of {@link MutableBlockPos} with position set to {@link #pos}
     */
    @Nonnull
    public MutableBlockPos origin() {
        return this.mpos.setPos(this.pos);
    }

    @Override
    public String toString() {
        return "WorldContext{" +
                "world=" + world +
                ", pos=" + pos +
                '}';
    }
}
