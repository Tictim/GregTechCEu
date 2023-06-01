package gregtech.client.model.special.frame;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public enum CubeVertex {
    DNW, DNE, DSW, DSE, UNW, UNE, USW, USE;

    public final String id = name().toLowerCase(Locale.ROOT);

    @Nullable
    private Vec3i direction;

    @Nonnull
    public Vec3i getDirection() {
        if (direction == null) {
            Vec3i a = getFacingY().getDirectionVec();
            Vec3i b = getFacingZ().getDirectionVec();
            Vec3i c = getFacingX().getDirectionVec();
            direction = new Vec3i(
                    a.getX() + b.getX() + c.getX(),
                    a.getY() + b.getY() + c.getY(),
                    a.getZ() + b.getZ() + c.getZ()
            );
        }
        return direction;
    }

    @Nonnull
    public CubeEdge getEdgeYZ() {
        return switch (this) {
            case DNW, DNE -> CubeEdge.DN;
            case DSW, DSE -> CubeEdge.DS;
            case UNW, UNE -> CubeEdge.UN;
            case USW, USE -> CubeEdge.US;
        };
    }

    @Nonnull
    public CubeEdge getEdgeZX() {
        return switch (this) {
            case DNW, UNW -> CubeEdge.NW;
            case DNE, UNE -> CubeEdge.NE;
            case DSW, USW -> CubeEdge.SW;
            case DSE, USE -> CubeEdge.SE;
        };
    }

    @Nonnull
    public CubeEdge getEdgeYX() {
        return switch (this) {
            case DNW, DSW -> CubeEdge.DW;
            case DNE, DSE -> CubeEdge.DE;
            case UNW, USW -> CubeEdge.UW;
            case UNE, USE -> CubeEdge.UE;
        };
    }

    @Nonnull
    public EnumFacing getFacingY() {
        return switch (this) {
            case DNW, DNE, DSW, DSE -> EnumFacing.DOWN;
            case UNW, UNE, USW, USE -> EnumFacing.UP;
        };
    }

    @Nonnull
    public EnumFacing getFacingZ() {
        return switch (this) {
            case DNW, DNE, UNW, UNE -> EnumFacing.NORTH;
            case DSW, DSE, USW, USE -> EnumFacing.SOUTH;
        };
    }

    @Nonnull
    public EnumFacing getFacingX() {
        return switch (this) {
            case DNW, DSW, UNW, USW -> EnumFacing.WEST;
            case DNE, DSE, UNE, USE -> EnumFacing.EAST;
        };
    }

    @Nonnull
    public CubeVertex flipX() {
        return switch (this) {
            case DNW -> DNE;
            case DNE -> DNW;
            case DSW -> DSE;
            case DSE -> DSW;
            case UNW -> UNE;
            case UNE -> UNW;
            case USW -> USE;
            case USE -> USW;
        };
    }

    @Nonnull
    public CubeVertex flipY() {
        return switch (this) {
            case DNW -> UNW;
            case DNE -> UNE;
            case DSW -> USW;
            case DSE -> USE;
            case UNW -> DNW;
            case UNE -> DNE;
            case USW -> DSW;
            case USE -> DSE;
        };
    }

    @Nonnull
    public CubeVertex flipZ() {
        return switch (this) {
            case DNW -> DSW;
            case DNE -> DSE;
            case DSW -> DNW;
            case DSE -> DNE;
            case UNW -> USW;
            case UNE -> USE;
            case USW -> UNW;
            case USE -> UNE;
        };
    }

    @Nonnull
    public CubeVertex flip(EnumFacing.Axis axis) {
        return switch (axis) {
            case X -> flipX();
            case Y -> flipY();
            case Z -> flipZ();
        };
    }

    @Nonnull
    public BlockPos.MutableBlockPos move(@Nonnull BlockPos.MutableBlockPos pos) {
        return pos.setPos(pos.getX() + getDirection().getX(),
                pos.getY() + getDirection().getY(),
                pos.getZ() + getDirection().getZ());
    }
}
