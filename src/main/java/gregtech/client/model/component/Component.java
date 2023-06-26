package gregtech.client.model.component;

import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class Component {

    private final ComponentShape shape;

    private final EnumMap<EnumFacing, ComponentFace> faces = new EnumMap<>(EnumFacing.class);

    private boolean locked;

    public Component(@Nonnull ComponentShape shape) {
        this.shape = shape;
    }

    public void lock() {
        if (this.locked) {
            throw new IllegalStateException("Already locked");
        }
        this.locked = true;
    }

    @Nonnull
    public ComponentShape getShape() {
        return shape;
    }

    @Nonnull
    public Map<EnumFacing, ComponentFace> getFaces() {
        return Collections.unmodifiableMap(faces);
    }

    public Component addAllFaces(@Nonnull ComponentTexture texture, boolean cullFace) {
        for (EnumFacing side : EnumFacing.VALUES) {
            addFace(side, new ComponentFace(texture, cullFace && shouldFaceBeCulled(side) ? side : null));
        }
        return this;
    }

    public Component addAllFaces(@Nonnull ComponentTexture texture, @Nullable EnumFacing cullFace) {
        for (EnumFacing side : EnumFacing.VALUES) {
            addFace(side, new ComponentFace(texture, cullFace));
        }
        return this;
    }

    public Component addFace(@Nonnull ComponentTexture texture, @Nonnull EnumFacing side) {
        addFace(side, new ComponentFace(texture, shouldFaceBeCulled(side) ? side : null));
        return this;
    }

    public Component addFace(@Nonnull ComponentTexture texture, @Nonnull EnumFacing side, @Nullable EnumFacing cullFace) {
        addFace(side, new ComponentFace(texture, cullFace));
        return this;
    }

    private void addFace(@Nonnull EnumFacing side, @Nonnull ComponentFace face) {
        if (this.locked) {
            throw new IllegalStateException("Cannot modify part instance after initial registration");
        }
        if (!this.shape.faceExists(side)) {
            throw new IllegalStateException("Shape " + this.shape + " doesn't have " + side + " face");
        }
        if (this.faces.putIfAbsent(side, face) != null) {
            throw new IllegalStateException("Face at side '" + side + "' already set");
        }
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
