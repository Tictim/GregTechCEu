package gregtech.client.model;

import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Simple model builder (TODO this is a very helpful documentation)
 */
public final class SimpleModelBuilder {

    private final List<BlockPart> blockParts = new ArrayList<>();
    private boolean uvLock;
    private boolean gui3d = true;
    private boolean ambientOcclusion = true;

    public SimpleModelBuilder uvLock(boolean uvLock) {
        this.uvLock = uvLock;
        return this;
    }

    public SimpleModelBuilder gui3d(boolean gui3d) {
        this.gui3d = gui3d;
        return this;
    }

    public SimpleModelBuilder ambientOcclusion(boolean ambientOcclusion) {
        this.ambientOcclusion = ambientOcclusion;
        return this;
    }

    public PartBuilder beginPart() {
        return new PartBuilder();
    }

    public SimpleModel build() {
        return new SimpleModel(this.blockParts, this.uvLock, this.gui3d, this.ambientOcclusion);
    }

    public final class PartBuilder {

        @Nullable
        private Vector3f from;
        @Nullable
        private Vector3f to;

        private final Map<EnumFacing, BlockPartFace> faces = new EnumMap<>(EnumFacing.class);

        @Nullable
        private BlockPartRotation rotation = null;
        private boolean shade = true;

        @Nonnull
        public Vector3f from() {
            if (from == null) throw new IllegalStateException("Property 'to' not set");
            return from;
        }

        @Nonnull
        public Vector3f to() {
            if (to == null) throw new IllegalStateException("Property 'to' not set");
            return to;
        }

        public PartBuilder from(float x, float y, float z) {
            this.from = new Vector3f(x, y, z);
            return this;
        }

        public PartBuilder to(float x, float y, float z) {
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

        public FaceBuilder forSide(@Nonnull Predicate<EnumFacing> facingPredicate) {
            EnumSet<EnumFacing> facings = EnumSet.noneOf(EnumFacing.class);
            for (EnumFacing facing : EnumFacing.VALUES) {
                if (facingPredicate.test(facing)) facings.add(facing);
            }
            if (facings.isEmpty()) throw new IllegalArgumentException("None of the facings match given condition");
            return new FaceBuilder(facings);
        }

        public PartBuilder setRotation(float originX, float originY, float originZ,
                                       @Nonnull EnumFacing.Axis axis, float rotation) {
            return setRotation(originX, originY, originZ, axis, rotation, false);
        }

        public PartBuilder setRotation(float originX, float originY, float originZ,
                                       @Nonnull EnumFacing.Axis axis, float rotation,
                                       boolean rescale) {
            this.rotation = new BlockPartRotation(new Vector3f(originX, originY, originZ),
                    axis, rotation, rescale);
            return this;
        }

        public PartBuilder noShade() {
            this.shade = false;
            return this;
        }

        public SimpleModelBuilder finishPart() {
            if (this.from == null) throw new IllegalStateException("'from' property not set");
            if (this.to == null) throw new IllegalStateException("'to' property not set");
            if (this.faces.isEmpty()) throw new IllegalStateException("no faces set");

            SimpleModelBuilder.this.blockParts.add(new BlockPart(this.from, this.to, new EnumMap<>(this.faces), this.rotation, this.shade));
            return SimpleModelBuilder.this;
        }

        @Nonnull
        public float[] getDefaultUV(@Nonnull EnumFacing side) {
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

            public PartBuilder finishSide() {
                if (this.texture == null && !this.setCullface && this.tintIndex == null && uv == null && textureRotation == null) {
                    return PartBuilder.this; // no-op
                }
                for (EnumFacing side : this.selectedFaces) {
                    BlockPartFace blockPart = PartBuilder.this.faces.get(side);

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
                        uv = new BlockFaceUV(this.uv != null ? this.uv : blockPart != null ? blockPart.blockFaceUV.uvs : getDefaultUV(side),
                                this.textureRotation != null ? this.textureRotation : blockPart != null ? blockPart.blockFaceUV.rotation : 0);
                    } else uv = blockPart != null ? blockPart.blockFaceUV : new BlockFaceUV(getDefaultUV(side), 0);


                    PartBuilder.this.faces.put(side, new BlockPartFace(cullFace, tintIndex, texture, uv));
                }

                return PartBuilder.this;
            }
        }
    }
}
