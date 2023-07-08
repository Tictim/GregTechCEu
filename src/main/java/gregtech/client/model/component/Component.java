package gregtech.client.model.component;

import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class Component {

    private final ComponentShape shape;

    private final List<ComponentFace> faces = new ArrayList<>();

    private boolean locked;

    public Component(@Nonnull ComponentShape shape) {
        this.shape = Objects.requireNonNull(shape, "shape == null");
    }

    public Component(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        this(new ComponentShape(fromX, fromY, fromZ, toX, toY, toZ));
    }

    public void lock() {
        if (this.locked) {
            throw new IllegalStateException("Already locked");
        }
        this.locked = true;
    }

    @Nonnull
    public ComponentShape getShape() {
        return this.shape;
    }

    @Nonnull
    public List<ComponentFace> getFaces() {
        return Collections.unmodifiableList(this.faces);
    }

    @Nonnull
    public Component addAllFaces(@Nonnull ComponentTexture texture) {
        return addAllFaces(texture, true);
    }

    @Nonnull
    public Component addAllFaces(@Nonnull ComponentTexture texture, boolean cullFace) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (!this.shape.faceExists(side)) continue;
            addFace(new ComponentFace(side, texture, cullFace && shouldFaceBeCulled(side) ? side : null));
        }
        return this;
    }

    @Nonnull
    public Component addAllFaces(@Nonnull ComponentTexture texture, @Nullable EnumFacing cullFace) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (!this.shape.faceExists(side)) continue;
            addFace(new ComponentFace(side, texture, cullFace));
        }
        return this;
    }

    @Nonnull
    public Component addFaces(@Nonnull ComponentTexture texture, @Nonnull Predicate<EnumFacing> facingFilter) {
        return addFaces(texture, facingFilter, true);
    }

    @Nonnull
    public Component addFaces(@Nonnull ComponentTexture texture, @Nonnull Predicate<EnumFacing> facingFilter, boolean cullFace) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (!this.shape.faceExists(side) || !facingFilter.test(side)) continue;
            addFace(new ComponentFace(side, texture, cullFace && shouldFaceBeCulled(side) ? side : null));
        }
        return this;
    }

    @Nonnull
    public Component addFaces(@Nonnull ComponentTexture texture, @Nonnull Predicate<EnumFacing> facingFilter, @Nullable EnumFacing cullFace) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (!this.shape.faceExists(side) || !facingFilter.test(side)) continue;
            addFace(new ComponentFace(side, texture, cullFace));
        }
        return this;
    }

    @Nonnull
    public Component addFace(@Nonnull ComponentTexture texture, @Nonnull EnumFacing side) {
        addFace(new ComponentFace(side, texture, shouldFaceBeCulled(side) ? side : null));
        return this;
    }

    @Nonnull
    public Component addFace(@Nonnull ComponentTexture texture, @Nonnull EnumFacing side, @Nullable EnumFacing cullFace) {
        addFace(new ComponentFace(side, texture, cullFace));
        return this;
    }

    private void addFace(@Nonnull ComponentFace face) {
        if (this.locked) {
            throw new IllegalStateException("Cannot modify part instance after initial registration");
        }
        if (!this.shape.faceExists(face.side)) {
            throw new IllegalStateException("Shape " + this.shape + " doesn't have " + face.side + " face");
        }
        this.faces.add(face);
    }

    private boolean shouldFaceBeCulled(@Nonnull EnumFacing facing) {
        return switch (facing) {
            case DOWN -> this.shape.fromY() <= 0;
            case UP -> this.shape.toY() >= 16;
            case NORTH -> this.shape.fromZ() <= 0;
            case SOUTH -> this.shape.toZ() >= 16;
            case WEST -> this.shape.fromX() <= 0;
            case EAST -> this.shape.toX() >= 16;
        };
    }

    @Override
    public String toString() {
        return "Component{" +
                "shape=" + shape +
                ", faces=" + faces +
                ", locked=" + locked +
                '}';
    }
}
