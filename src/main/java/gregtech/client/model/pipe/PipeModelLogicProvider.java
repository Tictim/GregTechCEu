package gregtech.client.model.pipe;

import gregtech.client.model.SimpleModel;
import gregtech.client.model.SimpleModelBuilder;
import gregtech.client.model.frame.FrameModelLogicProvider;
import gregtech.client.model.component.EnumIndexedPart;
import gregtech.client.model.special.IModeLogicProvider;
import gregtech.client.model.special.part.ModelPartRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import static net.minecraft.util.EnumFacing.*;

public abstract class PipeModelLogicProvider implements IModeLogicProvider {

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

    protected static final String TEXTURE_RESTRICTED_OVERLAY = "#restricted_overlay";

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
    public Map<String, String> getDefaultTextureMappings() {
        Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<>();
        map.put(TEXTURE_ATLAS_JOINTED, TEXTURE_ATLAS);
        map.put(TEXTURE_EXTRUSION, TEXTURE_SIDE);
        return map;
    }

    @Nonnull
    protected int[] registerBaseModels(@Nonnull ModelPartRegistry registry) {
        int[] models = new int[1 << 6];
        for (int connection = 0; connection < models.length; connection++) {
            if (connection == 0) { // open end on all 6 sides
                models[connection] = connectionlessModel(registry);
            } else {
                EnumSet<EnumFacing> sides = PipeModelLogic.getConnectedSides(connection);
                if (sides.size() == 1) {
                    models[connection] = singleBranchModel(registry, sides.iterator().next());
                } else if (sides.size() == 2) {
                    Iterator<EnumFacing> it = sides.iterator();
                    EnumFacing f1 = it.next();
                    EnumFacing f2 = it.next();
                    if (f1.getAxis() == f2.getAxis()) {
                        models[connection] = straightModel(registry, f1.getAxis());
                    } else {
                        models[connection] = complexModel(registry, connection, false);
                    }
                } else {
                    models[connection] = complexModel(registry, connection, true);
                }
            }
        }

        return models;
    }

    @Nonnull
    protected int[] registerRestrictedOverlayModels(@Nonnull ModelPartRegistry registry) {
        int[] models = new int[1 << 6];
        for (int connection = 0; connection < models.length; connection++) {
            if (connection == 0) { // open end on all 6 sides
                models[connection] = connectionlessRestrictedOverlay(registry);
            } else {
                EnumSet<EnumFacing> sides = PipeModelLogic.getConnectedSides(connection);
                if (sides.size() == 1) {
                    models[connection] = singleBranchRestrictedOverlay(registry, sides.iterator().next());
                } else if (sides.size() == 2) {
                    Iterator<EnumFacing> it = sides.iterator();
                    EnumFacing f1 = it.next();
                    EnumFacing f2 = it.next();
                    if (f1.getAxis() == f2.getAxis()) {
                        models[connection] = straightRestrictedOverlay(registry, f1.getAxis());
                    } else {
                        models[connection] = complexRestrictedOverlay(registry, connection);
                    }
                } else {
                    models[connection] = complexRestrictedOverlay(registry, connection);
                }
            }
        }

        return models;
    }

    @Nonnull
    protected EnumIndexedPart<EnumFacing> registerClosedEndModels(@Nonnull ModelPartRegistry registry) {
        return registry.registerParts(EnumFacing.class,
                (r, f) -> r.ifTextureExists(TEXTURE_ATLAS)
                        .registerPart(setAtlasTexture(
                                SimpleModel.builder()
                                        .beginPart()
                                        .from(
                                                f == WEST ? 0 : modelStart,
                                                f == DOWN ? 0 : modelStart,
                                                f == NORTH ? 0 : modelStart)
                                        .to(
                                                f == EAST ? 16 : modelEnd,
                                                f == UP ? 16 : modelEnd,
                                                f == SOUTH ? 16 : modelEnd),
                                0, true, f)
                                .finishPart().build())
                        .elseThenRegisterPart(SimpleModel.builder()
                                .beginPart()
                                .from(
                                        f == WEST ? 0 : modelStart,
                                        f == DOWN ? 0 : modelStart,
                                        f == NORTH ? 0 : modelStart)
                                .to(
                                        f == EAST ? 16 : modelEnd,
                                        f == UP ? 16 : modelEnd,
                                        f == SOUTH ? 16 : modelEnd)
                                .forSide(f).texture(TEXTURE_SIDE).tintIndex(TINT_PIPE).finishSide()
                                .finishPart().build())
                        .endIf());
    }

    @Nonnull
    protected EnumIndexedPart<EnumFacing> registerOpenEndModels(@Nonnull ModelPartRegistry registry) {
        return registry.registerParts(EnumFacing.class,
                (r, f) -> r.registerPart(SimpleModel.builder()
                        .beginPart()
                        .from(
                                f == WEST ? 0 : modelStart,
                                f == DOWN ? 0 : modelStart,
                                f == NORTH ? 0 : modelStart)
                        .to(
                                f == EAST ? 16 : modelEnd,
                                f == UP ? 16 : modelEnd,
                                f == SOUTH ? 16 : modelEnd)
                        .forSide(f).texture(TEXTURE_OPEN).tintIndex(TINT_PIPE).finishSide()
                        .finishPart().build()));
    }

    @Nonnull
    protected EnumIndexedPart<EnumFacing> registerOpenExtrusionModels(@Nonnull ModelPartRegistry registry) {
        return registry.registerParts(EnumFacing.class,
                (r, f) -> r.registerPart(SimpleModel.builder()
                        .beginPart()
                        .from(
                                f == WEST ? 0 - PIPE_EXTRUSION_SIZE : f == EAST ? 16 : modelStart,
                                f == DOWN ? 0 - PIPE_EXTRUSION_SIZE : f == UP ? 16 : modelStart,
                                f == NORTH ? 0 - PIPE_EXTRUSION_SIZE : f == SOUTH ? 16 : modelStart)
                        .to(
                                f == EAST ? 16 + PIPE_EXTRUSION_SIZE : f == WEST ? 0 : modelEnd,
                                f == UP ? 16 + PIPE_EXTRUSION_SIZE : f == DOWN ? 0 : modelEnd,
                                f == SOUTH ? 16 + PIPE_EXTRUSION_SIZE : f == NORTH ? 0 : modelEnd)
                        .forSide(f2 -> f2.getAxis() != f.getAxis()).texture(TEXTURE_EXTRUSION).tintIndex(TINT_PIPE).cullFace(f).finishSide()
                        .forSide(f).texture(TEXTURE_OPEN).tintIndex(TINT_PIPE).cullFace().finishSide()
                        .finishPart().build()));
    }

    @Nonnull
    protected EnumIndexedPart<EnumFacing> registerClosedExtrusionModels(@Nonnull ModelPartRegistry registry) {
        return registry.registerParts(EnumFacing.class,
                (r, f) -> {
                    float x1 = f == WEST ? 0 - PIPE_EXTRUSION_SIZE : f == EAST ? 16 : modelStart;
                    float y1 = f == DOWN ? 0 - PIPE_EXTRUSION_SIZE : f == UP ? 16 : modelStart;
                    float z1 = f == NORTH ? 0 - PIPE_EXTRUSION_SIZE : f == SOUTH ? 16 : modelStart;
                    float x2 = f == EAST ? 16 + PIPE_EXTRUSION_SIZE : f == WEST ? 0 : modelEnd;
                    float y2 = f == UP ? 16 + PIPE_EXTRUSION_SIZE : f == DOWN ? 0 : modelEnd;
                    float z2 = f == SOUTH ? 16 + PIPE_EXTRUSION_SIZE : f == NORTH ? 0 : modelEnd;

                    return r.registerPart(SimpleModel.builder()
                            .beginPart()
                            .from(x1, y1, z1)
                            .to(x2, y2, z2)
                            .forSide(f2 -> f2.getAxis() != f.getAxis()).texture(TEXTURE_EXTRUSION).tintIndex(TINT_PIPE).cullFace(f).finishSide()
                            .forSide(f).texture(TEXTURE_OPEN).tintIndex(TINT_PIPE).cullFace().finishSide()
                            .finishPart().build());
                });
    }

    protected int connectionlessModel(@Nonnull ModelPartRegistry registry) {
        return registry.registerPart(SimpleModel.builder()
                .beginPart()
                .from(modelStart, modelStart, modelStart)
                .to(modelEnd, modelEnd, modelEnd)
                .forAllSides().texture(TEXTURE_OPEN).tintIndex(TINT_PIPE).finishSide()
                .finishPart().build());
    }

    protected int connectionlessRestrictedOverlay(@Nonnull ModelPartRegistry registry) {
        return registry.registerPart(SimpleModel.builder()
                .beginPart()
                .from(modelStart, modelStart, modelStart)
                .to(modelEnd, modelEnd, modelEnd)
                .forAllSides().texture(TEXTURE_OPEN).tintIndex(TINT_PIPE).finishSide()
                .finishPart().build());
    }

    protected int singleBranchModel(@Nonnull ModelPartRegistry registry, @Nonnull EnumFacing connectedSide) {
        float x1 = connectedSide == WEST ? 0 : modelStart;
        float y1 = connectedSide == DOWN ? 0 : modelStart;
        float z1 = connectedSide == NORTH ? 0 : modelStart;
        float x2 = connectedSide == EAST ? 16 : modelEnd;
        float y2 = connectedSide == UP ? 16 : modelEnd;
        float z2 = connectedSide == SOUTH ? 16 : modelEnd;

        return registry.ifTextureExists(TEXTURE_ATLAS)
                .registerPart(setAtlasTexture(SimpleModel.builder().beginPart()
                                .from(x1, y1, z1)
                                .to(x2, y2, z2)
                                .forSide(connectedSide.getOpposite()).texture(TEXTURE_OPEN).tintIndex(TINT_PIPE).finishSide(),
                        PipeModelLogic.getBlockConnection(connectedSide), false,
                        f -> f.getAxis() != connectedSide.getAxis()).finishPart().build())
                .elseIfTextureExists(TEXTURE_SIDE)
                .registerPart(SimpleModel.builder()
                        .beginPart()
                        .from(x1, y1, z1)
                        .to(x2, y2, z2)
                        .forSide(connectedSide.getOpposite()).texture(TEXTURE_OPEN).tintIndex(TINT_PIPE).finishSide()
                        .forSide(f -> f.getAxis() != connectedSide.getAxis()).texture(TEXTURE_SIDE).tintIndex(TINT_PIPE).finishSide()
                        .finishPart().build())
                .elseThrowError()
                .endIf()
                .endIf();
    }

    protected int singleBranchRestrictedOverlay(@Nonnull ModelPartRegistry registry, @Nonnull EnumFacing connectedSide) {
        float x1 = connectedSide == WEST ? 0 : modelStart;
        float y1 = connectedSide == DOWN ? 0 : modelStart;
        float z1 = connectedSide == NORTH ? 0 : modelStart;
        float x2 = connectedSide == EAST ? 16 : modelEnd;
        float y2 = connectedSide == UP ? 16 : modelEnd;
        float z2 = connectedSide == SOUTH ? 16 : modelEnd;

        return registry.registerPart(SimpleModel.builder()
                .beginPart()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .forSide(f -> f.getAxis() != connectedSide.getAxis()).texture(TEXTURE_RESTRICTED_OVERLAY).tintIndex(TINT_OVERLAY).finishSide()
                .finishPart().build());
    }

    protected int straightModel(@Nonnull ModelPartRegistry registry, @Nonnull EnumFacing.Axis axis) {
        float x1 = axis == Axis.X ? 0 : modelStart;
        float y1 = axis == Axis.Y ? 0 : modelStart;
        float z1 = axis == Axis.Z ? 0 : modelStart;
        float x2 = axis == Axis.X ? 16 : modelEnd;
        float y2 = axis == Axis.Y ? 16 : modelEnd;
        float z2 = axis == Axis.Z ? 16 : modelEnd;

        return registry.ifTextureExists(TEXTURE_ATLAS)
                .registerPart(setAtlasTexture(
                        SimpleModel.builder()
                                .beginPart()
                                .from(x1, y1, z1)
                                .to(x2, y2, z2),
                        PipeModelLogic.getBlockConnection(
                                EnumFacing.getFacingFromAxis(AxisDirection.NEGATIVE, axis),
                                EnumFacing.getFacingFromAxis(AxisDirection.POSITIVE, axis)
                        ),
                        false,
                        f -> f.getAxis() != axis)
                        .finishPart().build())
                .elseIfTextureExists(TEXTURE_SIDE)
                .registerPart(SimpleModel.builder()
                        .beginPart()
                        .from(x1, y1, z1)
                        .to(x2, y2, z2)
                        .forSide(f -> f.getAxis() != axis).texture(TEXTURE_SIDE).tintIndex(TINT_PIPE).finishSide()
                        .finishPart().build())
                .elseThrowError()
                .endIf()
                .endIf();
    }

    protected int straightRestrictedOverlay(@Nonnull ModelPartRegistry registry, @Nonnull EnumFacing.Axis axis) {
        float x1 = axis == Axis.X ? 0 : modelStart;
        float y1 = axis == Axis.Y ? 0 : modelStart;
        float z1 = axis == Axis.Z ? 0 : modelStart;
        float x2 = axis == Axis.X ? 16 : modelEnd;
        float y2 = axis == Axis.Y ? 16 : modelEnd;
        float z2 = axis == Axis.Z ? 16 : modelEnd;

        return registry.registerPart(SimpleModel.builder()
                .beginPart()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .forSide(f -> f.getAxis() != axis).texture(TEXTURE_RESTRICTED_OVERLAY).tintIndex(TINT_OVERLAY).finishSide()
                .finishPart().build());
    }

    protected int complexModel(@Nonnull ModelPartRegistry registry, int blockConnections, boolean jointed) {
        SimpleModelBuilder builder = SimpleModel.builder();
        SimpleModelBuilder builder2 = SimpleModel.builder();
        for (EnumFacing side : VALUES) {
            SimpleModelBuilder.PartBuilder partBuilder = builder.beginPart();
            SimpleModelBuilder.PartBuilder partBuilder2 = builder2.beginPart();
            if (PipeModelLogic.isConnected(blockConnections, side)) {
                float x1 = side == WEST ? 0 : side == EAST ? modelEnd : modelStart;
                float y1 = side == DOWN ? 0 : side == UP ? modelEnd : modelStart;
                float z1 = side == NORTH ? 0 : side == SOUTH ? modelEnd : modelStart;
                float x2 = side == EAST ? 16 : side == WEST ? modelStart : modelEnd;
                float y2 = side == UP ? 16 : side == DOWN ? modelStart : modelEnd;
                float z2 = side == SOUTH ? 16 : side == NORTH ? modelStart : modelEnd;

                partBuilder.from(x1, y1, z1).to(x2, y2, z2);
                setAtlasTexture(partBuilder, blockConnections, jointed, f -> f.getAxis() != side.getAxis());

                partBuilder2
                        .from(x1, y1, z1)
                        .to(x2, y2, z2)
                        .forSide(f -> f.getAxis() != side.getAxis()).texture(TEXTURE_SIDE).tintIndex(TINT_PIPE).finishSide();
            } else {
                partBuilder
                        .from(modelStart, modelStart, modelStart)
                        .to(modelStart, modelStart, modelStart);
                setAtlasTexture(partBuilder, blockConnections, jointed, side);

                partBuilder2
                        .from(modelStart, modelStart, modelStart)
                        .to(modelStart, modelStart, modelStart)
                        .forSide(side).texture(TEXTURE_SIDE).tintIndex(TINT_PIPE).finishSide();
            }
            partBuilder.finishPart();
        }
        return registry.registerPart(builder.build());
    }

    protected int complexRestrictedOverlay(@Nonnull ModelPartRegistry registry, int blockConnections) {
        SimpleModelBuilder builder = SimpleModel.builder();
        for (EnumFacing side : VALUES) {
            SimpleModelBuilder.PartBuilder partBuilder = builder.beginPart();
            if (PipeModelLogic.isConnected(blockConnections, side)) {
                float x1 = side == WEST ? 0 : side == EAST ? modelEnd : modelStart;
                float y1 = side == DOWN ? 0 : side == UP ? modelEnd : modelStart;
                float z1 = side == NORTH ? 0 : side == SOUTH ? modelEnd : modelStart;
                float x2 = side == EAST ? 16 : side == WEST ? modelStart : modelEnd;
                float y2 = side == UP ? 16 : side == DOWN ? modelStart : modelEnd;
                float z2 = side == SOUTH ? 16 : side == NORTH ? modelStart : modelEnd;

                partBuilder
                        .from(x1, y1, z1)
                        .to(x2, y2, z2)
                        .forSide(f -> f.getAxis() != side.getAxis()).texture(TEXTURE_RESTRICTED_OVERLAY).tintIndex(TINT_OVERLAY).finishSide();
            } else {
                partBuilder
                        .from(modelStart, modelStart, modelStart)
                        .to(modelStart, modelStart, modelStart)
                        .forSide(side).texture(TEXTURE_RESTRICTED_OVERLAY).tintIndex(TINT_OVERLAY).finishSide();
            }
            partBuilder.finishPart();
        }
        return registry.registerPart(builder.build());
    }

    protected static SimpleModelBuilder.PartBuilder setAtlasTexture(SimpleModelBuilder.PartBuilder builder,
                                                                    int blockConnectionFlag,
                                                                    boolean jointed,
                                                                    Predicate<EnumFacing> facingFilter) {
        for (EnumFacing side : VALUES) {
            if (!facingFilter.test(side)) continue;
            setAtlasTexture(builder, blockConnectionFlag, jointed, side);
        }
        return builder;
    }

    protected static SimpleModelBuilder.PartBuilder setAtlasTexture(SimpleModelBuilder.PartBuilder builder,
                                                                    int blockConnectionFlag,
                                                                    boolean jointed,
                                                                    EnumFacing... sides) {
        for (EnumFacing side : sides) {
            setAtlasTexture(builder, blockConnectionFlag, jointed, side);
        }
        return builder;
    }

    protected static SimpleModelBuilder.PartBuilder setAtlasTexture(SimpleModelBuilder.PartBuilder builder,
                                                                    int blockConnectionFlag,
                                                                    boolean jointed,
                                                                    EnumFacing side) {
        byte sideConnection = PipeModelLogic.getSideConnection(blockConnectionFlag, side);

        SimpleModelBuilder.PartBuilder.FaceBuilder fb = builder.forSide(side)
                .texture(jointed ? TEXTURE_ATLAS_JOINTED : TEXTURE_ATLAS)
                .tintIndex(TINT_PIPE);

        int uindex, vindex;
        if (PipeModelLogic.connectedToLeft(sideConnection)) {
            uindex = PipeModelLogic.connectedToRight(sideConnection) ? 2 : 3;
        } else {
            uindex = PipeModelLogic.connectedToRight(sideConnection) ? 1 : 0;
        }
        if (PipeModelLogic.connectedToUp(sideConnection)) {
            vindex = PipeModelLogic.connectedToDown(sideConnection) ? 2 : 3;
        } else {
            vindex = PipeModelLogic.connectedToDown(sideConnection) ? 1 : 0;
        }

        float[] uv = builder.getDefaultUV(side);

        fb.uv(uv[0] * uindex * .25f,
                uv[1] * vindex * .25f,
                uv[2] * (uindex + 1) * .25f,
                uv[3] * (vindex + 1) * .25f);
        return fb.finishSide();
    }
}
