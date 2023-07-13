package gregtech.client.model.pipe;

import gregtech.client.model.component.*;
import gregtech.client.model.frame.FrameModelLogicProvider;
import gregtech.client.utils.MatrixUtils;
import net.minecraft.util.EnumFacing;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import static net.minecraft.util.EnumFacing.*;

@ParametersAreNonnullByDefault
public abstract class PipeModelLogicProvider implements IComponentLogicProvider {

    public static final int TINT_PIPE = 0;
    public static final int TINT_FRAME = FrameModelLogicProvider.TINT_OUTER;
    public static final int TINT_FRAME_INNER = FrameModelLogicProvider.TINT_INNER;
    public static final int TINT_OVERLAY = 3;

    public static final float PIPE_EXTRUSION_SIZE = 1;

    public static final String TEXTURE_ATLAS = "#atlas";
    public static final String TEXTURE_ATLAS_JOINTED = "#atlas_jointed";
    public static final String TEXTURE_SIDE = "#side";
    public static final String TEXTURE_IN = "#in";
    public static final String TEXTURE_EXTRUSION = "#extrusion";

    public static final PipeModelTexture DEFAULT_TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(TEXTURE_ATLAS, TINT_PIPE),
            new PipeSideAtlasTexture(TEXTURE_ATLAS_JOINTED, TINT_PIPE),
            new ComponentTexture(TEXTURE_SIDE, TINT_PIPE),
            new ComponentTexture(TEXTURE_IN, TINT_PIPE),
            new ComponentTexture(TEXTURE_EXTRUSION, TINT_PIPE)
    );

    protected final float thickness;
    protected final float modelStart;
    protected final float modelEnd;

    public PipeModelLogicProvider(float thickness) {
        this.thickness = thickness;
        this.modelStart = 8.0f - thickness * 8.0f;
        this.modelEnd = 8.0f + thickness * 8.0f;
    }

    @Nonnull
    @Override
    public ModelTextureMapping getDefaultTextureMappings() {
        return ModelTextureMapping.builder()
                .add(TEXTURE_ATLAS_JOINTED, TEXTURE_ATLAS)
                .add(TEXTURE_EXTRUSION, TEXTURE_SIDE)
                .add(TEXTURE_PARTICLE, TEXTURE_SIDE)
                .build();
    }

    protected PipeModelTexture getModelTextures() {
        return DEFAULT_TEXTURES;
    }

    @Nonnull
    @CheckReturnValue
    protected int[] defaultBaseModels(ComponentModel.Register componentRegister,
                                      ModelTextureMapping textureMapping) {
        return registerBaseModels(componentRegister, textureMapping, getModelTextures());
    }

    @Nonnull
    @CheckReturnValue
    protected EnumIndexedPart<EnumFacing> defaultEndModels(ComponentModel.Register componentRegister,
                                                           ModelTextureMapping textureMapping,
                                                           boolean closed) {
        return componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, getModelTextures(), closed));
    }

    @Nonnull
    @CheckReturnValue
    protected EnumIndexedPart<EnumFacing> defaultExtrusionModels(ComponentModel.Register componentRegister,
                                                                 ModelTextureMapping textureMapping,
                                                                 boolean closed) {
        return componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, getModelTextures(), closed));
    }

    @Nonnull
    @CheckReturnValue
    protected int[] registerBaseModels(ComponentModel.Register componentRegister,
                                       ModelTextureMapping textureMapping,
                                       PipeModelTexture textures) {
        int[] models = new int[1 << 6];
        for (int connection = 0; connection < models.length; connection++) {
            if (connection == 0) { // open end on all 6 sides
                models[connection] = connectionlessModel(componentRegister, textureMapping, textures);
            } else {
                EnumSet<EnumFacing> sides = PipeModelLogic.getConnectedSides(connection);
                if (sides.size() == 1) {
                    models[connection] = singleBranchModel(componentRegister, textureMapping, textures, sides.iterator().next());
                } else if (sides.size() == 2) {
                    Iterator<EnumFacing> it = sides.iterator();
                    EnumFacing f1 = it.next();
                    EnumFacing f2 = it.next();
                    if (f1.getAxis() == f2.getAxis()) {
                        models[connection] = straightModel(componentRegister, textureMapping, textures, f1.getAxis());
                    } else {
                        models[connection] = complexModel(componentRegister, textureMapping, textures, connection, false);
                    }
                } else {
                    models[connection] = complexModel(componentRegister, textureMapping, textures, connection, true);
                }
            }
        }

        return models;
    }

    protected void registerEndModels(EnumFacing facing,
                                     Consumer<Component> consumer,
                                     ModelTextureMapping textureMapping,
                                     PipeModelTexture textures,
                                     boolean closed) {
        Component c = new Component(
                facing == WEST ? 0 : modelStart,
                facing == DOWN ? 0 : modelStart,
                facing == NORTH ? 0 : modelStart,
                facing == EAST ? 16 : modelEnd,
                facing == UP ? 16 : modelEnd,
                facing == SOUTH ? 16 : modelEnd);
        if (closed) {
            PipeSideAtlasTexture sideAtlas = textures.sideAtlas(true);
            if (sideAtlas != null && textureMapping.has(sideAtlas.getTextureName())) {
                sideAtlas.setAtlasTexture(c, 0, facing);
            } else if (textures.side != null) {
                c.addFace(textures.side, facing);
            }
        } else {
            if (textures.in != null) {
                c.addFace(textures.in, facing);
            }
        }
        consumer.accept(c);
    }

    protected void registerExtrusionModels(EnumFacing facing,
                                           Consumer<Component> consumer,
                                           ModelTextureMapping textureMapping,
                                           PipeModelTexture textures,
                                           boolean closed) {
        Component c = new Component(
                facing == WEST ? 0 - PIPE_EXTRUSION_SIZE : facing == EAST ? 16 : modelStart,
                facing == DOWN ? 0 - PIPE_EXTRUSION_SIZE : facing == UP ? 16 : modelStart,
                facing == NORTH ? 0 - PIPE_EXTRUSION_SIZE : facing == SOUTH ? 16 : modelStart,
                facing == EAST ? 16 + PIPE_EXTRUSION_SIZE : facing == WEST ? 0 : modelEnd,
                facing == UP ? 16 + PIPE_EXTRUSION_SIZE : facing == DOWN ? 0 : modelEnd,
                facing == SOUTH ? 16 + PIPE_EXTRUSION_SIZE : facing == NORTH ? 0 : modelEnd);
        if (textures.extrusion != null) {
            c.addFaces(textures.extrusion, f -> f.getAxis() != facing.getAxis(), facing);
        }
        if (closed) {
            PipeSideAtlasTexture sideAtlas = textures.sideAtlas(true);
            if (sideAtlas != null && textureMapping.has(sideAtlas.getTextureName())) {
                sideAtlas.setAtlasTexture(c, 0, facing);
            } else if (textures.side != null) {
                c.addFace(textures.side, facing, facing);
            }
        } else if (textures.in != null) {
            c.addFace(textures.in, facing, facing);
        }
        consumer.accept(c);
    }

    protected int connectionlessModel(ComponentModel.Register componentRegister,
                                      ModelTextureMapping textureMapping,
                                      PipeModelTexture textures) {
        Component c = new Component(
                modelStart, modelStart, modelStart,
                modelEnd, modelEnd, modelEnd);
        if (textures.in != null) {
            c.addAllFaces(textures.in, true);
        }
        return componentRegister.add(c);
    }

    protected int singleBranchModel(ComponentModel.Register componentRegister,
                                    ModelTextureMapping textureMapping,
                                    PipeModelTexture textures,
                                    EnumFacing connectedSide) {
        Component c = new Component(
                connectedSide == WEST ? 0 : modelStart,
                connectedSide == DOWN ? 0 : modelStart,
                connectedSide == NORTH ? 0 : modelStart,
                connectedSide == EAST ? 16 : modelEnd,
                connectedSide == UP ? 16 : modelEnd,
                connectedSide == SOUTH ? 16 : modelEnd);
        PipeSideAtlasTexture sideAtlas = textures.sideAtlas(false);
        if (sideAtlas != null && textureMapping.has(sideAtlas.getTextureName())) {
            sideAtlas.setAtlasTexture(c,
                    PipeModelLogic.getBlockConnection(connectedSide),
                    f -> f.getAxis() != connectedSide.getAxis());
        } else if (textures.side != null) {
            c.addFaces(textures.side, f -> f.getAxis() != connectedSide.getAxis());
        }
        if (textures.in != null) {
            c.addFace(textures.in, connectedSide.getOpposite());
        }
        return componentRegister.add(c);
    }

    protected int straightModel(ComponentModel.Register componentRegister,
                                ModelTextureMapping textureMapping,
                                PipeModelTexture textures,
                                Axis axis) {
        Component c = new Component(
                axis == Axis.X ? 0 : modelStart,
                axis == Axis.Y ? 0 : modelStart,
                axis == Axis.Z ? 0 : modelStart,
                axis == Axis.X ? 16 : modelEnd,
                axis == Axis.Y ? 16 : modelEnd,
                axis == Axis.Z ? 16 : modelEnd);
        PipeSideAtlasTexture sideAtlas = textures.sideAtlas(false);
        if (sideAtlas != null && textureMapping.has(sideAtlas.getTextureName())) {
            sideAtlas.setAtlasTexture(c, PipeModelLogic.getBlockConnection(
                    EnumFacing.getFacingFromAxis(AxisDirection.NEGATIVE, axis),
                    EnumFacing.getFacingFromAxis(AxisDirection.POSITIVE, axis)
            ), f -> f.getAxis() != axis);
        } else if (textures.side != null) {
            c.addFaces(textures.side, f -> f.getAxis() != axis);
        }
        return componentRegister.add(c);
    }

    protected int complexModel(ComponentModel.Register componentRegister,
                               ModelTextureMapping textureMapping,
                               PipeModelTexture textures,
                               int blockConnections,
                               boolean jointed) {
        List<Component> list = new ArrayList<>();
        Component center = null;

        for (EnumFacing side : EnumFacing.VALUES) {
            if (PipeModelLogic.isConnected(blockConnections, side)) {
                Component c = new Component(
                        side == WEST ? 0 : side == EAST ? modelEnd : modelStart,
                        side == DOWN ? 0 : side == UP ? modelEnd : modelStart,
                        side == NORTH ? 0 : side == SOUTH ? modelEnd : modelStart,
                        side == EAST ? 16 : side == WEST ? modelStart : modelEnd,
                        side == UP ? 16 : side == DOWN ? modelStart : modelEnd,
                        side == SOUTH ? 16 : side == NORTH ? modelStart : modelEnd);

                PipeSideAtlasTexture sideAtlas = textures.sideAtlas(jointed);
                if (sideAtlas != null && textureMapping.has(sideAtlas.getTextureName())) {
                    sideAtlas.setAtlasTexture(c, blockConnections, f -> f.getAxis() != side.getAxis());
                } else if (textures.side != null) {
                    c.addFaces(textures.side, f -> f.getAxis() != side.getAxis());
                }

                list.add(c);
            } else {
                if (center == null) {
                    center = new Component(
                            modelStart, modelStart, modelStart,
                            modelEnd, modelEnd, modelEnd);
                    list.add(center);
                }

                PipeSideAtlasTexture sideAtlas = textures.sideAtlas(false);
                if (sideAtlas != null && textureMapping.has(sideAtlas.getTextureName())) {
                    sideAtlas.setAtlasTexture(center, blockConnections, side);
                } else if (textures.side != null) {
                    center.addFace(textures.side, side);
                }
            }
        }

        return componentRegister.add(list);
    }

    public static final class PipeModelTexture {

        @Nullable
        public final PipeSideAtlasTexture atlas;
        @Nullable
        public final PipeSideAtlasTexture jointedAtlas;

        @Nullable
        public final ComponentTexture side;
        @Nullable
        public final ComponentTexture in;
        @Nullable
        public final ComponentTexture extrusion;

        public PipeModelTexture(@Nullable PipeSideAtlasTexture atlas,
                                @Nullable PipeSideAtlasTexture jointedAtlas,
                                @Nullable ComponentTexture side,
                                @Nullable ComponentTexture in,
                                @Nullable ComponentTexture extrusion) {
            this.atlas = atlas;
            this.jointedAtlas = jointedAtlas;
            this.side = side;
            this.in = in;
            this.extrusion = extrusion;
        }

        @Nonnull
        public PipeSideAtlasTexture expectAtlas() {
            return Objects.requireNonNull(atlas, "atlas == null");
        }

        @Nonnull
        public PipeSideAtlasTexture expectJointedAtlas() {
            return Objects.requireNonNull(jointedAtlas, "jointedAtlas == null");
        }

        @Nonnull
        public ComponentTexture expectSide() {
            return Objects.requireNonNull(side, "side == null");
        }

        @Nonnull
        public ComponentTexture expectIn() {
            return Objects.requireNonNull(in, "in == null");
        }

        @Nonnull
        public ComponentTexture expectExtrusion() {
            return Objects.requireNonNull(extrusion, "extrusion == null");
        }

        @Nullable
        public PipeSideAtlasTexture sideAtlas(boolean jointed) {
            return jointed ?
                    this.jointedAtlas != null ? this.jointedAtlas : this.atlas :
                    this.atlas != null ? this.atlas : this.jointedAtlas;
        }

        @Override
        public String toString() {
            return "PipeModelTexture{" +
                    "atlas=" + atlas +
                    ", jointedAtlas=" + jointedAtlas +
                    ", side=" + side +
                    ", in=" + in +
                    ", extrusion=" + extrusion +
                    '}';
        }
    }

    public static final class PipeSideAtlasTexture {

        private final ComponentTexture[] textures = new ComponentTexture[16];
        private final String textureName;

        public PipeSideAtlasTexture(String textureName, int tintIndex) {
            this(false, null, textureName, tintIndex);
        }

        public PipeSideAtlasTexture(@Nullable ComponentTexture baseTexture, String textureName, int tintIndex) {
            this(false, baseTexture == null ? null : i -> baseTexture, textureName, tintIndex);
        }

        public PipeSideAtlasTexture(@Nullable PipeSideAtlasTexture baseAtlas, String textureName, int tintIndex) {
            this(false, baseAtlas == null ? null : i -> baseAtlas.get(i), textureName, tintIndex);
        }

        public PipeSideAtlasTexture(String textureName, IntFunction<ComponentTexture> atlasTextureFactory) {
            this.textureName = Objects.requireNonNull(textureName, "textureName == null");
            for (byte i = 0; i < this.textures.length; i++) {
                this.textures[i] = Objects.requireNonNull(atlasTextureFactory.apply(i),
                        "side atlas texture for connection " + Integer.toUnsignedString(i, 2) + " is null!");
            }
        }

        private PipeSideAtlasTexture(@SuppressWarnings("unused") boolean internal,
                                     @Nullable IntFunction<ComponentTexture> baseTextureGetter,
                                     String textureName,
                                     int tintIndex) {
            this.textureName = Objects.requireNonNull(textureName, "textureName == null");
            for (byte i = 0; i < this.textures.length; i++) {
                int x, y;
                if (PipeModelLogic.connectedToLeft(i)) {
                    x = PipeModelLogic.connectedToRight(i) ? 2 : 3;
                } else {
                    x = PipeModelLogic.connectedToRight(i) ? 1 : 0;
                }
                if (PipeModelLogic.connectedToUp(i)) {
                    y = PipeModelLogic.connectedToDown(i) ? 2 : 3;
                } else {
                    y = PipeModelLogic.connectedToDown(i) ? 1 : 0;
                }
                this.textures[i] = new ComponentTexture(
                        baseTextureGetter == null ? null : baseTextureGetter.apply(i),
                        textureName, tintIndex)
                        .setUVTransformation(m -> {
                            MatrixUtils.scale(m, 4);
                            MatrixUtils.translate(m, 16 * x, 16 * y);
                        });
            }
        }

        @Nonnull
        public String getTextureName() {
            return textureName;
        }

        @Nonnull
        public ComponentTexture get(int sideConnection) {
            return this.textures[sideConnection];
        }

        public void setAtlasTexture(Component component,
                                    int blockConnectionFlag,
                                    Predicate<EnumFacing> facingFilter) {
            for (EnumFacing side : EnumFacing.VALUES) {
                if (!facingFilter.test(side)) continue;
                setAtlasTexture(component, blockConnectionFlag, side);
            }
        }

        public void setAtlasTexture(Component component,
                                    int blockConnectionFlag,
                                    EnumFacing... sides) {
            for (EnumFacing side : sides) {
                setAtlasTexture(component, blockConnectionFlag, side);
            }
        }

        public void setAtlasTexture(Component component,
                                    int blockConnectionFlag,
                                    EnumFacing side) {
            ComponentTexture texture = get(PipeModelLogic.getSideConnection(blockConnectionFlag, side));
            component.addFace(texture, side);
        }

        @Override
        public String toString() {
            return "PipeSideAtlasTexture{" +
                    "textures=" + Arrays.toString(textures) +
                    ", textureName='" + textureName + '\'' +
                    '}';
        }
    }
}
