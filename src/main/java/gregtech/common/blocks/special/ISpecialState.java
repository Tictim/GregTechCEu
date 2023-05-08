package gregtech.common.blocks.special;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public interface ISpecialState extends IExtendedBlockState {

    @Nonnull
    IBlockAccess getWorld();

    @Nonnull
    BlockPos getPos();

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
            return switch (this) {
                case DN, DS, DW, DE -> EnumFacing.DOWN;
                case UN, US, UW, UE -> EnumFacing.UP;
                case NW, NE -> EnumFacing.NORTH;
                case SW, SE -> EnumFacing.SOUTH;
            };
        }

        @Nonnull
        public EnumFacing getFacingB() {
            return switch (this) {
                case DN, UN -> EnumFacing.NORTH;
                case DS, US -> EnumFacing.SOUTH;
                case DW, UW, NW, SW -> EnumFacing.WEST;
                case DE, UE, NE, SE -> EnumFacing.EAST;
            };
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
            return switch (this) {
                case DNW, DNE -> CubeEdge.DN;
                case DSW, DSE -> CubeEdge.DS;
                case UNW, UNE -> CubeEdge.UN;
                case USW, USE -> CubeEdge.US;
            };
        }

        @Nonnull
        public CubeEdge getEdgeBC() {
            return switch (this) {
                case DNW, UNW -> CubeEdge.NW;
                case DNE, UNE -> CubeEdge.NE;
                case DSW, USW -> CubeEdge.SW;
                case DSE, USE -> CubeEdge.SE;
            };
        }

        @Nonnull
        public CubeEdge getEdgeAC() {
            return switch (this) {
                case DNW, DSW -> CubeEdge.DW;
                case DNE, DSE -> CubeEdge.DE;
                case UNW, USW -> CubeEdge.UW;
                case UNE, USE -> CubeEdge.UE;
            };
        }

        @Nonnull
        public EnumFacing getFacingA() {
            return switch (this) {
                case DNW, DNE, DSW, DSE -> EnumFacing.DOWN;
                case UNW, UNE, USW, USE -> EnumFacing.UP;
            };
        }

        @Nonnull
        public EnumFacing getFacingB() {
            return switch (this) {
                case DNW, DNE, UNW, UNE -> EnumFacing.NORTH;
                case DSW, DSE, USW, USE -> EnumFacing.SOUTH;
            };
        }

        @Nonnull
        public EnumFacing getFacingC() {
            return switch (this) {
                case DNW, DSW, UNW, USW -> EnumFacing.WEST;
                case DNE, DSE, UNE, USE -> EnumFacing.EAST;
            };
        }

        public void move(@Nonnull MutableBlockPos pos) {
            pos.add(getDirection().getX(), getDirection().getY(), getDirection().getZ());
        }
    }
}
