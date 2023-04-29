package gregtech.common.blocks.extendedstate;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public interface ConnectionState extends IExtendedBlockState {

    /**
     * 0~5: side connection state<br>
     * 6~17: edge connection state<br>
     * 18~25: vertex connection state<br>
     */
    int BIT_FLAG_SIZE = 26;

    int ALL_FLAGS = (1 << BIT_FLAG_SIZE) - 1;

    @Nonnull
    IBlockAccess getWorld();

    @Nonnull
    BlockPos getPos();

    int getComputedFlags();

    int getConnectionFlags();

    void setFlags(int computed, int connection);

    default boolean matchesConnection(int flagMask, int expectedFlagValues) {
        return ConnectionHelper.calculateConnection(this, flagMask, expectedFlagValues);
    }

    default boolean isSideShown(@Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return ConnectionHelper.isSideShown(this, pos, side);
    }

    enum CubeEdge {
        DN, DS, DW, DE, UN, US, UW, UE, NW, NE, SW, SE;

        public final String id = name().toLowerCase(Locale.ROOT);

        @Nullable
        private Vec3i direction;

        @Nonnull
        public Vec3i getDirection() {
            if (direction == null) {
                Vec3i a = getFacingA().getDirectionVec();
                Vec3i b = getFacingB().getDirectionVec();
                direction = new Vec3i(
                        a.getX() + b.getX(),
                        a.getY() + b.getY(),
                        a.getZ() + b.getZ()
                );
            }
            return direction;
        }

        @Nonnull
        public EnumFacing getFacingA() {
            switch (this) {
                case DN: case DS: case DW: case DE: return EnumFacing.DOWN;
                case UN: case US: case UW: case UE: return EnumFacing.UP;
                case NW: case NE: return EnumFacing.NORTH;
                case SW: case SE: return EnumFacing.SOUTH;
                default: throw new IllegalStateException("Unreachable");
            }
        }

        @Nonnull
        public EnumFacing getFacingB() {
            switch (this) {
                case DN: case UN: return EnumFacing.NORTH;
                case DS: case US: return EnumFacing.SOUTH;
                case DW: case UW: case NW: case SW: return EnumFacing.WEST;
                case DE: case UE: case NE: case SE: return EnumFacing.EAST;
                default: throw new IllegalStateException("Unreachable");
            }
        }

        public void move(@Nonnull MutableBlockPos pos) {
            pos.add(getDirection().getX(), getDirection().getY(), getDirection().getZ());
        }
    }

    enum CubeVertex {
        DNW, DNE, DSW, DSE, UNW, UNE, USW, USE;

        public final String id = name().toLowerCase(Locale.ROOT);

        @Nullable
        private Vec3i direction;

        @Nonnull
        public Vec3i getDirection() {
            if (direction == null) {
                Vec3i a = getFacingA().getDirectionVec();
                Vec3i b = getFacingB().getDirectionVec();
                Vec3i c = getFacingC().getDirectionVec();
                direction = new Vec3i(
                        a.getX() + b.getX() + c.getX(),
                        a.getY() + b.getY() + c.getY(),
                        a.getZ() + b.getZ() + c.getZ()
                );
            }
            return direction;
        }

        @Nonnull
        public CubeEdge getEdgeAB() {
            switch (this) {
                case DNW: case DNE: return CubeEdge.DN;
                case DSW: case DSE: return CubeEdge.DS;
                case UNW: case UNE: return CubeEdge.UN;
                case USW: case USE: return CubeEdge.US;
                default: throw new IllegalStateException("Unreachable");
            }
        }

        @Nonnull
        public CubeEdge getEdgeBC() {
            switch (this) {
                case DNW: case UNW: return CubeEdge.NW;
                case DNE: case UNE: return CubeEdge.NE;
                case DSW: case USW: return CubeEdge.SW;
                case DSE: case USE: return CubeEdge.SE;
                default: throw new IllegalStateException("Unreachable");
            }
        }

        @Nonnull
        public CubeEdge getEdgeAC() {
            switch (this) {
                case DNW: case DSW: return CubeEdge.DW;
                case DNE: case DSE: return CubeEdge.DE;
                case UNW: case USW: return CubeEdge.UW;
                case UNE: case USE: return CubeEdge.UE;
                default: throw new IllegalStateException("Unreachable");
            }
        }

        @Nonnull
        public EnumFacing getFacingA() {
            switch (this) {
                case DNW: case DNE: case DSW: case DSE: return EnumFacing.DOWN;
                case UNW: case UNE: case USW: case USE: return EnumFacing.UP;
                default: throw new IllegalStateException("Unreachable");
            }
        }

        @Nonnull
        public EnumFacing getFacingB() {
            switch (this) {
                case DNW: case DNE: case UNW: case UNE: return EnumFacing.NORTH;
                case DSW: case DSE: case USW: case USE: return EnumFacing.SOUTH;
                default: throw new IllegalStateException("Unreachable");
            }
        }

        @Nonnull
        public EnumFacing getFacingC() {
            switch (this) {
                case DNW: case DSW: case UNW: case USW: return EnumFacing.WEST;
                case DNE: case DSE: case UNE: case USE: return EnumFacing.EAST;
                default: throw new IllegalStateException("Unreachable");
            }
        }

        public void move(@Nonnull MutableBlockPos pos) {
            pos.add(getDirection().getX(), getDirection().getY(), getDirection().getZ());
        }
    }
}
