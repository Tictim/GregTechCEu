package gregtech.client.model.component;

import gregtech.client.utils.CubeVertex;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public final class ComponentShape {

    private final int fromX;
    private final int fromY;
    private final int fromZ;
    private final int toX;
    private final int toY;
    private final int toZ;

    @Nullable
    private Matrix4f transformation;
    private boolean shade = true;

    public ComponentShape(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
    }

    public int fromX() {
        return fromX;
    }

    public int fromY() {
        return fromY;
    }

    public int fromZ() {
        return fromZ;
    }

    public int toX() {
        return toX;
    }

    public int toY() {
        return toY;
    }

    public int toZ() {
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
                vertex.getFacingX() == EnumFacing.WEST ? fromX : toX,
                vertex.getFacingX() == EnumFacing.DOWN ? fromY : toY,
                vertex.getFacingX() == EnumFacing.NORTH ? fromZ : toZ
        );
        if (this.transformation != null) {
            Vector4f vec4 = new Vector4f(pos.x - 8, pos.y - 8, pos.z - 8, 1f);
            Matrix4f.transform(this.transformation, vec4, vec4);
            if (Math.abs(vec4.w - 1f) > 1e-5) {
                vec4.scale(1f / vec4.w);
            }
            pos.set(vec4.x + 8, vec4.y + 8, vec4.z + 8);
        }
        return pos;
    }

    @Nonnull
    public Vector3f getFaceDirection(@Nonnull EnumFacing facing) {
        Vector3f pos = new Vector3f(
                facing.getXOffset(),
                facing.getYOffset(),
                facing.getZOffset()
        );
        if (this.transformation != null) {
            Vector4f vec4 = new Vector4f(pos.x, pos.y, pos.z, 1f);
            Matrix4f.transform(this.transformation, vec4, vec4);
            if (Math.abs(vec4.w - 1f) > 1e-5) {
                vec4.scale(1f / vec4.w);
            }
            pos.set(vec4.x, vec4.y, vec4.z);
            if (pos.lengthSquared() != 0) {
                pos.normalise();
            }
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
