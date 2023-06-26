package gregtech.client.utils;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public enum CubeEdge {
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

    @Nonnull
    public BlockPos.MutableBlockPos move(@Nonnull BlockPos.MutableBlockPos pos) {
        return pos.setPos(pos.getX() + getDirection().getX(),
                pos.getY() + getDirection().getY(),
                pos.getZ() + getDirection().getZ());
    }
}
