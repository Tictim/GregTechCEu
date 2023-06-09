package gregtech.client.model.special.pipe;

import gregtech.client.model.SimpleModel;
import gregtech.client.model.SimpleModelBuilder;
import gregtech.client.model.special.EnumIndexedPart;
import gregtech.client.model.special.IModeLogicProvider;
import gregtech.client.model.special.IModelLogic;
import gregtech.client.model.special.ModelPartRegistry;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IModel;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import static net.minecraft.util.EnumFacing.*;

public class PipeModelLogicProvider implements IModeLogicProvider {

    public static final int TINT_PIPE = 0;
    public static final int TINT_FRAME = 1;
    public static final int TINT_FRAME_INNER = 2;
    public static final int TINT_INSULATION = 3;

    protected static final float PIPE_EXTRUSION_SIZE = 1 / 16f;

    protected static final String TEXTURE_ATLAS = "#atlas";
    protected static final String TEXTURE_ATLAS_JOINTED = "#atlas_jointed";
    protected static final String TEXTURE_OPEN = "#open";
    protected static final String TEXTURE_EXTRUSION = "#extrusion";

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
    public IModelLogic createLogic(@Nonnull ModelPartRegistry registry) {
        int[] models = new int[1 << 6];
        for (int connection = 0; connection < models.length; connection++) {
            // Special cases
            if (connection == 0) { // open end on all 6 sides
                models[connection] = registry.registerPart(createConnectionlessModel());
            } else {
                EnumSet<EnumFacing> sides = PipeModelLogic.getConnectedSides(connection);
                if (sides.size() == 1) {
                    models[connection] = registry.registerPart(createSingleBranchModel(sides.iterator().next()));
                } else if (sides.size() == 2) {
                    Iterator<EnumFacing> it = sides.iterator();
                    EnumFacing f1 = it.next();
                    EnumFacing f2 = it.next();
                    if (f1.getAxis() == f2.getAxis()) {
                        models[connection] = registry.registerPart(createStraightModel(f1.getAxis()));
                    } else {
                        models[connection] = registry.registerPart(createComplexModel(connection, false));
                    }
                } else {
                    models[connection] = registry.registerPart(createComplexModel(connection, true));
                }
            }
        }

        EnumIndexedPart<EnumFacing> closedEnd = registry.registerPart(EnumFacing.class, f -> SimpleModel.builder()
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
                .finishPart().build());

        EnumIndexedPart<EnumFacing> openEnd = registry.registerPart(EnumFacing.class, f -> SimpleModel.builder()
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
                .finishPart().build());

        EnumIndexedPart<EnumFacing> extrusion = registry.registerPart(EnumFacing.class, f -> SimpleModel.builder()
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
                .finishPart().build());

        return new PipeModelLogic(models, closedEnd, openEnd, extrusion);
    }

    @Nonnull
    @Override
    public Map<String, String> getDefaultTextureMappings() {
        return Collections.singletonMap(TEXTURE_ATLAS_JOINTED, TEXTURE_ATLAS);
    }

    @Nonnull
    protected IModel createConnectionlessModel() {
        return SimpleModel.builder()
                .beginPart()
                .from(modelStart, modelStart, modelStart)
                .to(modelEnd, modelEnd, modelEnd)
                .forAllSides()
                .texture(TEXTURE_OPEN).tintIndex(TINT_PIPE)
                .finishSide()
                .finishPart()
                .build();
    }

    @Nonnull
    protected IModel createSingleBranchModel(@Nonnull EnumFacing connectedSide) {
        SimpleModelBuilder.PartBuilder builder = SimpleModel.builder()
                .beginPart()
                .from(
                        connectedSide == WEST ? 0 : modelStart,
                        connectedSide == DOWN ? 0 : modelStart,
                        connectedSide == NORTH ? 0 : modelStart)
                .to(
                        connectedSide == EAST ? 16 : modelEnd,
                        connectedSide == UP ? 16 : modelEnd,
                        connectedSide == SOUTH ? 16 : modelEnd);
        builder.forSide(connectedSide.getOpposite()).texture(TEXTURE_OPEN).tintIndex(TINT_PIPE).finishSide();
        setAtlasTexture(builder,
                PipeModelLogic.getBlockConnection(connectedSide),
                false,
                f -> f.getAxis() != connectedSide.getAxis());
        return builder.finishPart().build();
    }

    @Nonnull
    protected IModel createStraightModel(@Nonnull EnumFacing.Axis axis) {
        SimpleModelBuilder.PartBuilder builder = SimpleModel.builder()
                .beginPart()
                .from(
                        axis == Axis.X ? 0 : modelStart,
                        axis == Axis.Y ? 0 : modelStart,
                        axis == Axis.Z ? 0 : modelStart)
                .to(
                        axis == Axis.X ? 16 : modelEnd,
                        axis == Axis.Y ? 16 : modelEnd,
                        axis == Axis.Z ? 16 : modelEnd);
        setAtlasTexture(builder,
                PipeModelLogic.getBlockConnection(
                        EnumFacing.getFacingFromAxis(AxisDirection.NEGATIVE, axis),
                        EnumFacing.getFacingFromAxis(AxisDirection.POSITIVE, axis)
                ),
                false,
                f -> f.getAxis() != axis);
        return builder.finishPart().build();
    }

    @Nonnull
    protected IModel createComplexModel(int blockConnections, boolean jointed) {
        SimpleModelBuilder builder = SimpleModel.builder();
        for (EnumFacing side : VALUES) {
            SimpleModelBuilder.PartBuilder partBuilder = builder.beginPart();
            if (PipeModelLogic.isConnected(blockConnections, side)) {
                partBuilder.from(
                                side == WEST ? 0 : side == EAST ? modelEnd : modelStart,
                                side == DOWN ? 0 : side == UP ? modelEnd : modelStart,
                                side == NORTH ? 0 : side == SOUTH ? modelEnd : modelStart)
                        .to(
                                side == EAST ? 16 : side == WEST ? modelStart : modelEnd,
                                side == UP ? 16 : side == DOWN ? modelStart : modelEnd,
                                side == SOUTH ? 16 : side == NORTH ? modelStart : modelEnd);
                setAtlasTexture(partBuilder, blockConnections, jointed, f -> f.getAxis() != side.getAxis());
            } else {
                partBuilder.from(modelStart, modelStart, modelStart)
                        .to(modelStart, modelStart, modelStart);
                setAtlasTexture(partBuilder, blockConnections, jointed, side);
            }
            partBuilder.finishPart();
        }
        return builder.build();
    }

    protected void setAtlasTexture(SimpleModelBuilder.PartBuilder builder,
                                   int blockConnectionFlag,
                                   boolean jointed,
                                   Predicate<EnumFacing> facingFilter) {
        for (EnumFacing side : VALUES) {
            if (!facingFilter.test(side)) continue;
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

            fb.uv(uindex * .25f, vindex * .25f, (uindex + 1) * .25f, (vindex + 1) * .25f);
            fb.finishSide();
        }
    }

    protected static void setAtlasTexture(SimpleModelBuilder.PartBuilder builder,
                                          int blockConnectionFlag,
                                          boolean jointed,
                                          EnumFacing... sides) {
        for (EnumFacing side : sides) {
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

            fb.uv(uindex * .25f, vindex * .25f, (uindex + 1) * .25f, (vindex + 1) * .25f);
            fb.finishSide();
        }
    }
}
