package gregtech.client.model;

import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Simple model builder (this is a very helpful documentation)
 */
public final class SimpleModelBuilder {

    @Nonnull
    private float[] defaultUV(@Nonnull EnumFacing side) {
        if (from == null) {
            throw new IllegalStateException("'from' property should be set first");
        } else if (to == null) {
            throw new IllegalStateException("'to' property should be set first");
        }
        return switch (side) {
            case DOWN -> new float[]{from.x, 16.0F - to.z, to.x, 16.0F - from.z};
            case UP -> new float[]{from.x, from.z, to.x, to.z};
            case NORTH -> new float[]{16.0F - to.x, 16.0F - to.y, 16.0F - from.x, 16.0F - from.y};
            case SOUTH -> new float[]{from.x, 16.0F - to.y, to.x, 16.0F - from.y};
            case WEST -> new float[]{from.z, 16.0F - to.y, to.z, 16.0F - from.y};
            case EAST -> new float[]{16.0F - to.z, 16.0F - to.y, 16.0F - from.z, 16.0F - from.y};
        };
    }

    private final List<BlockPart> blockParts = new ArrayList<>();
    private boolean uvLock;
    private boolean gui3d = true;
    private boolean ambientOcclusion = true;

    @Nullable
    private Vector3f from;
    @Nullable
    private Vector3f to;

    private final Map<EnumFacing, BlockPartFace> faces = new EnumMap<>(EnumFacing.class);

    @Nullable
    private BlockPartRotation rotation = null;
    private boolean shade = true;

    public SimpleModelBuilder from(float x, float y, float z) {
        this.from = new Vector3f(x, y, z);
        return this;
    }

    public SimpleModelBuilder to(float x, float y, float z) {
        this.to = new Vector3f(x, y, z);
        return this;
    }

    public FaceBuilder forAllSides() {
        return new FaceBuilder(EnumSet.allOf(EnumFacing.class));
    }

    public FaceBuilder forSide(@Nonnull EnumFacing... sides) {
        if (sides.length == 0) throw new IllegalArgumentException("No side selected");
        return new FaceBuilder(EnumSet.of(sides[0], sides));
    }

    public SimpleModelBuilder setRotation(float originX, float originY, float originZ,
                                          @Nonnull EnumFacing.Axis axis, float rotation) {
        return setRotation(originX, originY, originZ, axis, rotation, false);
    }

    public SimpleModelBuilder setRotation(float originX, float originY, float originZ,
                                          @Nonnull EnumFacing.Axis axis, float rotation,
                                          boolean rescale) {
        this.rotation = new BlockPartRotation(new Vector3f(originX, originY, originZ),
                axis, rotation, rescale);
        return this;
    }

    public SimpleModelBuilder noShade() {
        this.shade = false;
        return this;
    }

    public SimpleModelBuilder addPart() {
        if (this.from == null) throw new IllegalStateException("'from' property not set");
        if (this.to == null) throw new IllegalStateException("'to' property not set");
        if (this.faces.isEmpty()) throw new IllegalStateException("no faces set");

        this.blockParts.add(new BlockPart(this.from, this.to, new EnumMap<>(this.faces), this.rotation, this.shade));
        this.from = null;
        this.to = null;
        this.faces.clear();
        this.rotation = null;
        this.shade = true;
        return this;
    }

    public SimpleModelBuilder setUvLock(boolean uvLock) {
        this.uvLock = uvLock;
        return this;
    }

    public SimpleModelBuilder setGui3d(boolean gui3d) {
        this.gui3d = gui3d;
        return this;
    }

    public SimpleModelBuilder setAmbientOcclusion(boolean ambientOcclusion) {
        this.ambientOcclusion = ambientOcclusion;
        return this;
    }

    public SimpleModel build() {
        return new SimpleModel(this.blockParts, this.uvLock, this.gui3d, this.ambientOcclusion);
    }

    public final class FaceBuilder {

        private final Set<EnumFacing> selectedFaces;
        @Nullable
        private String texture;
        private boolean setCullface;
        @Nullable
        private EnumFacing cullFace;
        @Nullable
        private Integer tintIndex;
        @Nullable
        private float[] uv;
        @Nullable
        private Integer textureRotation;

        public FaceBuilder(@Nonnull Set<EnumFacing> selectedFaces) {
            this.selectedFaces = selectedFaces;
        }

        public FaceBuilder texture(@Nonnull String texture) {
            this.texture = texture;
            return this;
        }

        public FaceBuilder cullFace() {
            this.setCullface = true;
            return this;
        }

        public FaceBuilder cullFace(@Nullable EnumFacing cullFace) {
            this.setCullface = true;
            this.cullFace = cullFace;
            return this;
        }

        public FaceBuilder tintIndex(int tintIndex) {
            this.tintIndex = tintIndex;
            return this;
        }

        public FaceBuilder uv(float uMin, float vMin, float uMax, float vMax) {
            this.uv = new float[]{uMin, vMin, uMax, vMax};
            return this;
        }

        public FaceBuilder textureRotation(int rotation) {
            this.textureRotation = rotation;
            return this;
        }

        public SimpleModelBuilder end() {
            if (this.texture != null || this.cullFace != null || this.tintIndex != null) {
                for (EnumFacing side : this.selectedFaces) {
                    BlockPartFace blockPart = SimpleModelBuilder.this.faces.get(side);

                    String texture;
                    @Nullable
                    EnumFacing cullFace;
                    int tintIndex;
                    BlockFaceUV uv;

                    if (this.texture != null) texture = this.texture;
                    else if (blockPart != null) texture = blockPart.texture;
                    else throw new IllegalStateException("'texture' property not set for " + side + " facing");

                    if (this.setCullface) cullFace = this.cullFace != null ? this.cullFace : side;
                    else cullFace = blockPart != null ? blockPart.cullFace : null;

                    if (this.tintIndex != null) tintIndex = this.tintIndex;
                    else tintIndex = blockPart != null ? blockPart.tintIndex : -1;

                    if (this.uv != null || this.textureRotation != null) {
                        uv = new BlockFaceUV(this.uv != null ? this.uv : blockPart != null ? blockPart.blockFaceUV.uvs : defaultUV(side),
                                this.textureRotation != null ? this.textureRotation : blockPart != null ? blockPart.blockFaceUV.rotation : 0);
                    } else uv = blockPart != null ? blockPart.blockFaceUV : new BlockFaceUV(defaultUV(side), 0);


                    SimpleModelBuilder.this.faces.put(side, new BlockPartFace(cullFace, tintIndex, texture, uv));
                }
            }

            return SimpleModelBuilder.this;
        }
    }
}
