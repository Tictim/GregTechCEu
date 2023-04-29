package gregtech.common.blocks.extendedstate;

import gregtech.common.blocks.extendedstate.ConnectionState.CubeEdge;
import gregtech.common.blocks.extendedstate.ConnectionState.CubeVertex;
import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

// internal helper methods
class ConnectionHelper {

    static boolean calculateConnection(@Nonnull ConnectionState state, int flagMask, int expectedFlagValues) {
        int computedFlags = state.getComputedFlags() & ConnectionState.ALL_FLAGS;
        int connectionFlags = state.getConnectionFlags() & computedFlags;

        flagMask &= ConnectionState.ALL_FLAGS;
        if ((computedFlags & flagMask) == flagMask) { // All necessary flags computed
            return (connectionFlags & flagMask) == (expectedFlagValues & flagMask);
        }

        boolean modified = false;
        MutableBlockPos mpos = null;

        for (int bitFlagIndex = 0; bitFlagIndex < ConnectionState.BIT_FLAG_SIZE; bitFlagIndex++) {
            int bit = 1 << bitFlagIndex;
            if ((bit & flagMask) == 0) continue;

            boolean connected;
            if ((bit & computedFlags) == 0) {
                if (mpos == null) {
                    mpos = new MutableBlockPos();
                }
                mpos.setPos(state.getPos());

                if (bitFlagIndex < 6) {
                    mpos.move(EnumFacing.byIndex(bitFlagIndex));
                } else if (bitFlagIndex < 18) {
                    CubeEdge.values()[bitFlagIndex - 6].move(mpos);
                } else {
                    CubeVertex.values()[bitFlagIndex - 18].move(mpos);
                }

                if (isLoaded(state, mpos)) {
                    connected = isConnectedTo(state, mpos);
                    computedFlags |= bit;
                    if (connected) connectionFlags |= bit;
                    modified = true;
                } else {
                    connected = false;
                }
            } else {
                connected = (bit & connectionFlags) != 0;
            }
            boolean expected = (bit & expectedFlagValues) != 0;
            if (connected != expected) {
                if (modified) {
                    state.setFlags(computedFlags, connectionFlags);
                }
                return false;
            }
        }
        if (modified) {
            state.setFlags(computedFlags, connectionFlags);
        }
        return true;
    }

    private static boolean isLoaded(@Nonnull ConnectionState state, @Nonnull MutableBlockPos mpos) {
        IBlockAccess world = state.getWorld();
        return !(world instanceof World) || ((World) world).isBlockLoaded(mpos, !((World) world).isRemote);
    }

    private static boolean isConnectedTo(@Nonnull ConnectionState state, @Nonnull MutableBlockPos mpos) {
        Block block = state.getBlock();
        return block instanceof ConnectionChecker && ((ConnectionChecker) block).isConnectedToNeighbor(
                state.getWorld(), state.getClean(), state.getPos(), mpos);
    }

    static boolean isSideShown(@Nonnull ConnectionState state, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return state.shouldSideBeRendered(state.getWorld(), pos, side);
    }
}
