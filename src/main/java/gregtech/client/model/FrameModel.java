package gregtech.client.model;

import gregtech.api.unification.material.Material;
import gregtech.common.blocks.BlockFrame;
import gregtech.common.blocks.special.ISpecialState.CubeEdge;
import gregtech.common.blocks.special.ISpecialState.CubeVertex;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.model.IModel;

import javax.annotation.Nonnull;

import static net.minecraft.util.EnumFacing.VALUES;

public class FrameModel extends SpecialModel {

    private final int[] faces;
    private final int[] slimEdges;
    private final int[] wideEdges;
    private final int[] vertices;

    public FrameModel() {
        this.faces = new int[EnumFacing.values().length];
        this.slimEdges = new int[CubeEdge.values().length];
        this.wideEdges = new int[CubeEdge.values().length];
        this.vertices = new int[CubeVertex.values().length];

        for (EnumFacing side : VALUES) {
            this.faces[side.ordinal()] = registerPart(sideModel(side));
        }

        for (CubeEdge edge : CubeEdge.values()) {
            this.slimEdges[edge.ordinal()] = registerPart(edgeModel(edge, 1));
            this.wideEdges[edge.ordinal()] = registerPart(edgeModel(edge, 2));
        }

        for (CubeVertex vertex : CubeVertex.values()) {
            this.vertices[vertex.ordinal()] = registerPart(vertexModel(vertex, 1));
        }
    }

    public FrameModel(@Nonnull FrameModel orig) {
        super(orig);
        this.faces = orig.faces;
        this.slimEdges = orig.slimEdges;
        this.wideEdges = orig.wideEdges;
        this.vertices = orig.vertices;
    }

    @Override
    protected void collectModels(@Nonnull ModelCollector collector) {
        if (collector.world == null || collector.pos == null) {
            for (EnumFacing facing : VALUES) {
                collector.includePart(this.faces[facing.ordinal()]);
            }
            return;
        }

        FrameConnection[] sideConnections = new FrameConnection[EnumFacing.values().length];
        FrameEdge[] edgeParts = new FrameEdge[CubeEdge.values().length];
        MutableBlockPos mpos = new MutableBlockPos();

        for (EnumFacing facing : VALUES) {
            FrameConnection sideShown = connection(collector.world, collector.pos, facing);
            sideConnections[facing.ordinal()] = sideShown;
            if (sideShown == FrameConnection.NONE) {
                collector.includePart(this.faces[facing.ordinal()]);
            }
        }

        for (CubeEdge edge : CubeEdge.values()) {
            FrameConnection sideA = sideConnections[edge.getFacingA().ordinal()];
            FrameConnection sideB = sideConnections[edge.getFacingB().ordinal()];

            FrameEdge edgePart = switch (sideA) {
                case NONE -> sideB == FrameConnection.NONE ? FrameEdge.WIDE : FrameEdge.HIDDEN;
                case CONNECTED_TO_FRAME -> switch (sideB) {
                    case NONE -> FrameEdge.HIDDEN;
                    case CONNECTED_TO_BLOCK -> FrameEdge.SLIM;
                    case CONNECTED_TO_FRAME -> FrameConnection.NONE == FrameConnection.min(
                            connection(collector.world, mpos.setPos(collector.pos).move(edge.getFacingA()), edge.getFacingB()),
                            connection(collector.world, mpos.setPos(collector.pos).move(edge.getFacingB()), edge.getFacingA())) ?
                            FrameEdge.SLIM : FrameEdge.HIDDEN;
                };
                case CONNECTED_TO_BLOCK -> FrameEdge.SLIM;
            };
            edgeParts[edge.ordinal()] = edgePart;

            switch (edgePart) {
                case SLIM -> collector.includePart(this.slimEdges[edge.ordinal()]);
                case WIDE -> collector.includePart(this.wideEdges[edge.ordinal()]);
            }
        }

        for (CubeVertex vertex : CubeVertex.values()) {
            if (sideConnections[vertex.getFacingA().ordinal()] != FrameConnection.NONE &&
                    sideConnections[vertex.getFacingB().ordinal()] != FrameConnection.NONE &&
                    sideConnections[vertex.getFacingC().ordinal()] != FrameConnection.NONE &&
                    edgeParts[vertex.getEdgeAB().ordinal()] == FrameEdge.HIDDEN &&
                    edgeParts[vertex.getEdgeBC().ordinal()] == FrameEdge.HIDDEN &&
                    edgeParts[vertex.getEdgeAC().ordinal()] == FrameEdge.HIDDEN &&
                    connection(collector.world, mpos.setPos(collector.pos).move(vertex.getFacingA()).move(vertex.getFacingB()), vertex.getFacingC()) == FrameConnection.NONE &&
                    connection(collector.world, mpos.setPos(collector.pos).move(vertex.getFacingB()).move(vertex.getFacingC()), vertex.getFacingA()) == FrameConnection.NONE &&
                    connection(collector.world, mpos.setPos(collector.pos).move(vertex.getFacingC()).move(vertex.getFacingA()), vertex.getFacingB()) == FrameConnection.NONE) {
                collector.includePart(this.vertices[vertex.ordinal()]);
            }
        }
    }

    @Nonnull
    @Override
    protected FrameModel copy() {
        return new FrameModel(this);
    }

    private static FrameConnection connection(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        BlockPos offset = pos.offset(side);
        Material frame1 = BlockFrame.getFrameMaterialAt(world, pos);
        Material frame2 = BlockFrame.getFrameMaterialAt(world, offset);
        if (frame1 == frame2) return FrameConnection.CONNECTED_TO_FRAME;

        IBlockState offState = world.getBlockState(offset);
        if (offState.doesSideBlockRendering(world, offset, side.getOpposite())) {
            return FrameConnection.CONNECTED_TO_BLOCK;
        }
        return FrameConnection.NONE;
    }

    private static IModel sideModel(EnumFacing side) {
        float x1 = side.getXOffset() > 0 ? 15 : 0;
        float y1 = side.getYOffset() > 0 ? 15 : 0;
        float z1 = side.getZOffset() > 0 ? 15 : 0;
        float x2 = side.getXOffset() < 0 ? 1 : 16;
        float y2 = side.getYOffset() < 0 ? 1 : 16;
        float z2 = side.getZOffset() < 0 ? 1 : 16;

        return SimpleModel.builder()
                .beginPart()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .forSide(side).texture(sideTexture(side)).cullFace(side).tintIndex(1).finishSide()
                .forSide(side.getOpposite()).texture(sideTexture(side)).cullFace(side).tintIndex(2).finishSide()
                .finishPart()
                .build();
    }

    private static IModel edgeModel(CubeEdge edge, float thickness) {
        float x1 = edge.getDirection().getX() > 0 ? 16 - thickness : 0;
        float y1 = edge.getDirection().getY() > 0 ? 16 - thickness : 0;
        float z1 = edge.getDirection().getZ() > 0 ? 16 - thickness : 0;
        float x2 = edge.getDirection().getX() < 0 ? thickness : 16;
        float y2 = edge.getDirection().getX() < 0 ? thickness : 16;
        float z2 = edge.getDirection().getX() < 0 ? thickness : 16;

        return SimpleModel.builder()
                .beginPart()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .forSide(edge.getFacingA().getOpposite(), edge.getFacingB().getOpposite()).texture("border").tintIndex(2).finishSide()
                .finishPart()
                .build();
    }

    private static IModel vertexModel(CubeVertex edge, float thickness) {
        float x1 = edge.getDirection().getX() > 0 ? 16 - thickness : 0;
        float y1 = edge.getDirection().getY() > 0 ? 16 - thickness : 0;
        float z1 = edge.getDirection().getZ() > 0 ? 16 - thickness : 0;
        float x2 = edge.getDirection().getX() < 0 ? thickness : 16;
        float y2 = edge.getDirection().getX() < 0 ? thickness : 16;
        float z2 = edge.getDirection().getX() < 0 ? thickness : 16;

        return SimpleModel.builder()
                .beginPart()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .forSide(edge.getFacingA().getOpposite(), edge.getFacingB().getOpposite()).texture("border").tintIndex(2).finishSide()
                .finishPart()
                .build();
    }

    private static String sideTexture(EnumFacing side) {
        return switch (side) {
            case UP -> "top";
            case DOWN -> "bottom";
            default -> "side";
        };
    }

    private enum FrameConnection {
        NONE,
        CONNECTED_TO_BLOCK,
        CONNECTED_TO_FRAME;

        public static FrameConnection min(FrameConnection c1, FrameConnection c2) {
            return c1.ordinal() < c2.ordinal() ? c1 : c2;
        }
    }

    private enum FrameEdge {
        HIDDEN,
        SLIM,
        WIDE
    }
}
