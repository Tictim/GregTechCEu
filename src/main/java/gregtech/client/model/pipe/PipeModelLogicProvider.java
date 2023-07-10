package gregtech.client.model.pipe;

import gregtech.client.model.component.*;
import gregtech.client.model.frame.FrameModelLogicProvider;
import gregtech.client.utils.MatrixUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.minecraft.util.EnumFacing.*;

@ParametersAreNonnullByDefault
public abstract class PipeModelLogicProvider implements IComponentLogicProvider {

    public static final int TINT_PIPE = 0;
    public static final int TINT_FRAME = FrameModelLogicProvider.TINT_OUTER;
    public static final int TINT_FRAME_INNER = FrameModelLogicProvider.TINT_INNER;
    public static final int TINT_OVERLAY = 3;

    protected static final float PIPE_EXTRUSION_SIZE = 1 / 16f;

    protected static final String TEXTURE_ATLAS = "#atlas";
    protected static final String TEXTURE_ATLAS_JOINTED = "#atlas_jointed";
    protected static final String TEXTURE_OPEN = "#open";
    protected static final String TEXTURE_EXTRUSION = "#extrusion";
    protected static final String TEXTURE_SIDE = "#side";

    protected static final ComponentTexture[] ATLAS_TEXTURES = generateAtlasTextures(TEXTURE_ATLAS, TINT_PIPE);
    protected static final ComponentTexture[] JOINTED_ATLAS_TEXTURES = generateAtlasTextures(TEXTURE_ATLAS_JOINTED, TINT_PIPE);

    protected static final ComponentTexture SIDE_TEXTURE = new ComponentTexture(TEXTURE_SIDE, TINT_PIPE);
    protected static final ComponentTexture OPEN_TEXTURE = new ComponentTexture(TEXTURE_OPEN, TINT_PIPE);
    protected static final ComponentTexture EXTRUSION_TEXTURE = new ComponentTexture(TEXTURE_OPEN, TINT_PIPE);

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
        Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<>();
        map.put(TEXTURE_ATLAS_JOINTED, TEXTURE_ATLAS);
        map.put(TEXTURE_EXTRUSION, TEXTURE_SIDE);
        return new ModelTextureMapping(map);
    }

    protected ComponentTexture sideTexture() {
        return SIDE_TEXTURE;
    }

    protected ComponentTexture openEndTexture() {
        return OPEN_TEXTURE;
    }

    protected ComponentTexture extrusionTexture() {
        return EXTRUSION_TEXTURE;
    }

    protected ComponentTexture[] sideAtlasTextures(boolean jointed) {
        return jointed ? JOINTED_ATLAS_TEXTURES : ATLAS_TEXTURES;
    }

    @Nonnull
    protected int[] registerBaseModels(ComponentModel.Register componentRegister,
                                       ModelTextureMapping textureMapping) {
        int[] models = new int[1 << 6];
        for (int connection = 0; connection < models.length; connection++) {
            if (connection == 0) { // open end on all 6 sides
                models[connection] = connectionlessModel(componentRegister, textureMapping);
            } else {
                EnumSet<EnumFacing> sides = PipeModelLogic.getConnectedSides(connection);
                if (sides.size() == 1) {
                    models[connection] = singleBranchModel(componentRegister, textureMapping, sides.iterator().next());
                } else if (sides.size() == 2) {
                    Iterator<EnumFacing> it = sides.iterator();
                    EnumFacing f1 = it.next();
                    EnumFacing f2 = it.next();
                    if (f1.getAxis() == f2.getAxis()) {
                        models[connection] = straightModel(componentRegister, textureMapping, f1.getAxis());
                    } else {
                        models[connection] = complexModel(componentRegister, textureMapping, connection, false);
                    }
                } else {
                    models[connection] = complexModel(componentRegister, textureMapping, connection, true);
                }
            }
        }

        return models;
    }

    protected void registerEndModels(EnumFacing facing,
                                     Consumer<Component> consumer,
                                     ModelTextureMapping textureMapping,
                                     boolean closed) {
        Component c = new Component(
                facing == WEST ? 0 : modelStart,
                facing == DOWN ? 0 : modelStart,
                facing == NORTH ? 0 : modelStart,
                facing == EAST ? 16 : modelEnd,
                facing == UP ? 16 : modelEnd,
                facing == SOUTH ? 16 : modelEnd);
        if (closed) {
            if (textureMapping.has(sideAtlasTextures(false)[0])) {
                setAtlasTexture(c, 0, sideAtlasTextures(true), facing);
            } else {
                c.addFace(sideTexture(), facing);
            }
        } else {
            c.addFace(openEndTexture(), facing);
        }
        consumer.accept(c);
    }

    protected void registerExtrusionModels(EnumFacing facing,
                                           Consumer<Component> consumer,
                                           ModelTextureMapping textureMapping,
                                           boolean closed) {
        Component c = new Component(
                facing == WEST ? 0 - PIPE_EXTRUSION_SIZE : facing == EAST ? 16 : modelStart,
                facing == DOWN ? 0 - PIPE_EXTRUSION_SIZE : facing == UP ? 16 : modelStart,
                facing == NORTH ? 0 - PIPE_EXTRUSION_SIZE : facing == SOUTH ? 16 : modelStart,
                facing == EAST ? 16 + PIPE_EXTRUSION_SIZE : facing == WEST ? 0 : modelEnd,
                facing == UP ? 16 + PIPE_EXTRUSION_SIZE : facing == DOWN ? 0 : modelEnd,
                facing == SOUTH ? 16 + PIPE_EXTRUSION_SIZE : facing == NORTH ? 0 : modelEnd);
        c.addFaces(extrusionTexture(), f -> f.getAxis() != facing.getAxis(), facing);
        if (closed) {
            if (textureMapping.has(sideAtlasTextures(false)[0])) {
                setAtlasTexture(c, 0, sideAtlasTextures(true), facing);
            } else {
                c.addFace(sideTexture(), facing, facing);
            }
        } else {
            c.addFace(openEndTexture(), facing, facing);
        }
        consumer.accept(c);
    }

    protected int connectionlessModel(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping) {
        return componentRegister.add(new Component(
                modelStart, modelStart, modelStart,
                modelEnd, modelEnd, modelEnd)
                .addAllFaces(openEndTexture(), true));
    }

    protected int singleBranchModel(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping, EnumFacing connectedSide) {
        Component c = new Component(
                connectedSide == WEST ? 0 : modelStart,
                connectedSide == DOWN ? 0 : modelStart,
                connectedSide == NORTH ? 0 : modelStart,
                connectedSide == EAST ? 16 : modelEnd,
                connectedSide == UP ? 16 : modelEnd,
                connectedSide == SOUTH ? 16 : modelEnd);
        if (textureMapping.has(sideAtlasTextures(false)[0])) {
            setAtlasTexture(c, PipeModelLogic.getBlockConnection(connectedSide), sideAtlasTextures(false), f -> f.getAxis() != connectedSide.getAxis());
        } else {
            c.addFaces(sideTexture(), f -> f.getAxis() != connectedSide.getAxis());
        }
        c.addFace(openEndTexture(), connectedSide.getOpposite());
        return componentRegister.add(c);
    }

    protected int straightModel(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping, EnumFacing.Axis axis) {
        Component c = new Component(
                axis == Axis.X ? 0 : modelStart,
                axis == Axis.Y ? 0 : modelStart,
                axis == Axis.Z ? 0 : modelStart,
                axis == Axis.X ? 16 : modelEnd,
                axis == Axis.Y ? 16 : modelEnd,
                axis == Axis.Z ? 16 : modelEnd);
        if (textureMapping.has(sideAtlasTextures(false)[0])) {
            setAtlasTexture(c, PipeModelLogic.getBlockConnection(
                    EnumFacing.getFacingFromAxis(AxisDirection.NEGATIVE, axis),
                    EnumFacing.getFacingFromAxis(AxisDirection.POSITIVE, axis)
            ), sideAtlasTextures(false), f -> f.getAxis() != axis);
        } else {
            c.addFaces(sideTexture(), f -> f.getAxis() != axis);
        }
        return componentRegister.add(c);
    }

    protected int complexModel(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping, int blockConnections, boolean jointed) {
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

                if (textureMapping.has(sideAtlasTextures(false)[0])) {
                    setAtlasTexture(c,
                            blockConnections,
                            sideAtlasTextures(jointed),
                            f -> f.getAxis() != side.getAxis());
                } else {
                    c.addFaces(sideTexture(), f -> f.getAxis() != side.getAxis());
                }

                list.add(c);
            } else {
                if (center == null) {
                    center = new Component(
                            modelStart, modelStart, modelStart,
                            modelEnd, modelEnd, modelEnd);
                    list.add(center);
                }

                if (textureMapping.has(sideAtlasTextures(false)[0])) {
                    setAtlasTexture(center,
                            blockConnections,
                            sideAtlasTextures(jointed),
                            side);
                } else {
                    center.addFace(sideTexture(), side);
                }
            }
        }

        return componentRegister.add(list);
    }

    @Nonnull
    public static ComponentTexture[] generateAtlasTextures(String texture, int tintIndex) {
        return generateAtlasTextures(null, texture, tintIndex);
    }

    @Nonnull
    public static ComponentTexture[] generateAtlasTextures(@Nullable ComponentTexture[] baseAtlas, String texture, int tintIndex) {
        ComponentTexture[] textures = new ComponentTexture[16];
        for (byte i = 0; i < textures.length; i++) {
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
            textures[i] = new ComponentTexture(baseAtlas == null ? null : baseAtlas[i], texture, tintIndex)
                    .setUVTransformation(m -> {
                        MatrixUtils.scale(m, .25f, .25f);
                        MatrixUtils.translate(m, 4 * x, 4 * y);
                    });
        }
        return textures;
    }

    public static void setAtlasTexture(Component component,
                                       int blockConnectionFlag,
                                       ComponentTexture[] atlasTextures,
                                       Predicate<EnumFacing> facingFilter) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (!facingFilter.test(side)) continue;
            setAtlasTexture(component, blockConnectionFlag, atlasTextures, side);
        }
    }

    public static void setAtlasTexture(Component component,
                                       int blockConnectionFlag,
                                       ComponentTexture[] atlasTextures,
                                       EnumFacing... sides) {
        for (EnumFacing side : sides) {
            setAtlasTexture(component, blockConnectionFlag, atlasTextures, side);
        }
    }

    public static void setAtlasTexture(Component component,
                                       int blockConnectionFlag,
                                       ComponentTexture[] atlasTextures,
                                       EnumFacing side) {
        component.addFace(atlasTextures[PipeModelLogic.getSideConnection(blockConnectionFlag, side)], side);
    }
}
