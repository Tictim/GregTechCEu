package gregtech.client.model.component;

import gregtech.client.utils.CubeVertex;
import gregtech.client.utils.MatrixUtils;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public final class ComponentShape {

    private final float fromX;
    private final float fromY;
    private final float fromZ;
    private final float toX;
    private final float toY;
    private final float toZ;

    @Nullable
    private Matrix4f transformation;
    private boolean shade = true;

    public ComponentShape(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
    }

    public float fromX() {
        return fromX;
    }

    public float fromY() {
        return fromY;
    }

    public float fromZ() {
        return fromZ;
    }

    public float toX() {
        return toX;
    }

    public float toY() {
        return toY;
    }

    public float toZ() {
        return toZ;
    }

    @Nullable
    public Matrix4f getTransformation() {
        return transformation;
    }

    public boolean shade() {
        return shade;
    }

    public boolean faceExists(@Nonnull EnumFacing facing) {
        return faceExists(facing.getAxis());
    }

    public boolean faceExists(@Nonnull EnumFacing.Axis axis) {
        return switch (axis) {
            case X -> toY - fromY != 0 && toZ - fromZ != 0;
            case Y -> toX - fromX != 0 && toZ - fromZ != 0;
            case Z -> toX - fromX != 0 && toY - fromY != 0;
        };
    }

    @Nonnull
    public Vector3f getVertexAt(@Nonnull CubeVertex vertex) {
        Vector3f pos = new Vector3f(
                (vertex.getFacingX() == EnumFacing.WEST ? fromX : toX) * (1 / 16f),
                (vertex.getFacingY() == EnumFacing.DOWN ? fromY : toY) * (1 / 16f),
                (vertex.getFacingZ() == EnumFacing.NORTH ? fromZ : toZ) * (1 / 16f)
        );
        if (this.transformation != null) {
            pos.x -= .5f;
            pos.y -= .5f;
            pos.z -= .5f;
            MatrixUtils.transform(this.transformation, pos, pos);
            pos.x += .5f;
            pos.y += .5f;
            pos.z += .5f;
        }
        return pos;
    }

    @Nonnull
    public Vector3f getFaceDirection(@Nonnull EnumFacing facing, boolean normalize) {
        Vector3f pos = new Vector3f(
                facing.getXOffset(),
                facing.getYOffset(),
                facing.getZOffset()
        );
        if (this.transformation != null) {
            MatrixUtils.transform(this.transformation, pos, pos);
        }

        if (normalize && pos.lengthSquared() != 0) {
            pos.normalise();
        }

        return pos;
    }

    @Nonnull
    public ComponentShape setTransformation(@Nonnull Consumer<Matrix4f> function) {
        Matrix4f mat = new Matrix4f();
        Objects.requireNonNull(function, "function == null").accept(mat);
        this.transformation = mat;
        return this;
    }

    @Nonnull
    public ComponentShape setTransformation(@Nonnull Vector3f origin, @Nonnull EnumFacing.Axis axis, float angle) {
        return setTransformation(origin, axis, angle, false);
    }

    @Nonnull
    public ComponentShape setTransformation(@Nonnull Vector3f origin, @Nonnull EnumFacing.Axis axis, float angle, boolean rescale) {
        Objects.requireNonNull(origin, "origin == null");
        Objects.requireNonNull(axis, "axis == null");
        if (angle == 0) return this;

        return setTransformation(mat -> {
            final float factor = 1 / 16f;
            boolean isOriginZero = origin.x == 0 && origin.y == 0 && origin.z == 0;

            if (!isOriginZero) {
                mat.translate(new Vector3f(
                        origin.x * factor,
                        origin.y * factor,
                        origin.z * factor
                ));
            }

            mat.rotate(angle, switch (axis) {
                case X -> new Vector3f(1, 0, 0);
                case Y -> new Vector3f(0, 1, 0);
                case Z -> new Vector3f(0, 0, 1);
            });

            if (rescale) {
                float rescaleAmount = (float) Math.min(
                        Math.abs(1 / Math.cos(angle)),
                        Math.abs(1 / Math.sin(angle))
                ) - 1;

                mat.scale(new Vector3f(
                        axis == EnumFacing.Axis.X ? 0 : rescaleAmount,
                        axis == EnumFacing.Axis.Y ? 0 : rescaleAmount,
                        axis == EnumFacing.Axis.Z ? 0 : rescaleAmount
                ));
            }

            if (!isOriginZero) {
                mat.translate(new Vector3f(
                        -origin.x * factor,
                        -origin.y * factor,
                        -origin.z * factor
                ));
            }
        });
    }

    @Nonnull
    public ComponentShape setShade(boolean value) {
        this.shade = value;
        return this;
    }

    @Override
    public String toString() {
        return "ComponentShape{" +
                "fromX=" + fromX +
                ", fromY=" + fromY +
                ", fromZ=" + fromZ +
                ", toX=" + toX +
                ", toY=" + toY +
                ", toZ=" + toZ +
                ", transformation=" + transformation +
                ", shade=" + shade +
                '}';
    }
}
