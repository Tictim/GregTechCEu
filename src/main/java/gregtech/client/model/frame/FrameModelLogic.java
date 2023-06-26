package gregtech.client.model.frame;

import gregtech.api.unification.material.Material;
import gregtech.client.model.component.EnumIndexedPart;
import gregtech.client.model.special.IModelLogic;
import gregtech.client.model.component.ModelCollector;
import gregtech.client.model.component.WorldContext;
import gregtech.client.utils.CubeEdge;
import gregtech.client.utils.CubeVertex;
import gregtech.common.blocks.BlockFrame;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;

import static net.minecraft.util.EnumFacing.VALUES;

public class FrameModelLogic implements IModelLogic {

    private final EnumIndexedPart<EnumFacing> faces;
    private final EnumIndexedPart<CubeEdge> slimEdges;
    private final EnumIndexedPart<CubeEdge> wideEdges;
    private final EnumIndexedPart<CubeVertex> slimVertices;
    private final EnumIndexedPart<CubeVertex> wideVertices;

    public FrameModelLogic(@Nonnull EnumIndexedPart<EnumFacing> faces,
                           @Nonnull EnumIndexedPart<CubeEdge> slimEdges,
                           @Nonnull EnumIndexedPart<CubeEdge> wideEdges,
                           @Nonnull EnumIndexedPart<CubeVertex> slimVertices,
                           @Nonnull EnumIndexedPart<CubeVertex> wideVertices) {
        this.faces = faces;
        this.slimEdges = slimEdges;
        this.wideEdges = wideEdges;
        this.slimVertices = slimVertices;
        this.wideVertices = wideVertices;
    }

    @Override
    public void collectModels(@Nonnull ModelCollector collector, @Nullable WorldContext ctx) {
        if (ctx == null) {
            for (EnumFacing facing : VALUES) {
                collector.includePart(this.faces.getPart(facing));
            }
            return;
        }

        FrameConnection[] sideConnections = new FrameConnection[EnumFacing.values().length];
        FramePart[] edgeParts = new FramePart[CubeEdge.values().length];

        for (EnumFacing facing : VALUES) {
            FrameConnection sideShown = connection(ctx.world, ctx.pos, facing);
            sideConnections[facing.ordinal()] = sideShown;

            if (sideShown == FrameConnection.NONE) {
                collector.includePart(this.faces.getPart(facing));
            }
        }

        for (CubeEdge edge : CubeEdge.values()) {
            FramePart edgePart = computeEdge(ctx, edge,
                    sideConnections[edge.getFacingA().ordinal()],
                    sideConnections[edge.getFacingB().ordinal()]);
            edgeParts[edge.ordinal()] = edgePart;

            switch (edgePart) {
                case SLIM -> collector.includePart(this.slimEdges.getPart(edge));
                case WIDE -> collector.includePart(this.wideEdges.getPart(edge));
            }
        }

        for (CubeVertex vertex : CubeVertex.values()) {
            FramePart vertexPart = computeVertex(ctx, vertex, sideConnections, edgeParts);

            switch (vertexPart) {
                case SLIM -> collector.includePart(this.slimVertices.getPart(vertex));
                case WIDE -> collector.includePart(this.wideVertices.getPart(vertex));
            }
        }
    }

    // fun stuff ahead, you have been warned
    private static FramePart computeEdge(WorldContext ctx, CubeEdge edge, FrameConnection sideA, FrameConnection sideB) {
        if (sideA == FrameConnection.NONE || sideB == FrameConnection.NONE) {
            if (sideA == FrameConnection.NONE && sideB == FrameConnection.NONE) {
                return FramePart.WIDE;
            }
            return FramePart.HIDDEN;
        }
        if (sideA == FrameConnection.CONNECTED_TO_BLOCK || sideB == FrameConnection.CONNECTED_TO_BLOCK) {
            if (sideA == FrameConnection.CONNECTED_TO_BLOCK && sideB == FrameConnection.CONNECTED_TO_BLOCK) {
                return FramePart.HIDDEN;
            }

            EnumFacing frameSide;
            EnumFacing blockSide;

            if (sideA == FrameConnection.CONNECTED_TO_FRAME) {
                frameSide = edge.getFacingA();
                blockSide = edge.getFacingB();
            } else {
                frameSide = edge.getFacingB();
                blockSide = edge.getFacingA();
            }

            return switch (connection(ctx.world, ctx.origin().move(frameSide), blockSide)) {
                case NONE -> FramePart.SLIM;
                case CONNECTED_TO_BLOCK -> FramePart.HIDDEN;
                case CONNECTED_TO_FRAME -> FrameConnection.NONE == connection(ctx.world, edge.move(ctx.origin()), frameSide.getOpposite()) ?
                        FramePart.SLIM : FramePart.HIDDEN;
            };
        }

        if (FrameConnection.NONE == connection(ctx.world, ctx.origin().move(edge.getFacingA()), edge.getFacingB()) ||
                FrameConnection.NONE == connection(ctx.world, ctx.origin().move(edge.getFacingB()), edge.getFacingA())) {
            return FramePart.SLIM;
        }
        return FramePart.HIDDEN;
    }

    // EVEN MORE FUN STUFF AHEAD
    private static FramePart computeVertex(WorldContext ctx, CubeVertex vertex, FrameConnection[] sideConnections, FramePart[] edgeParts) {
        if (edgeParts[vertex.getEdgeYZ().ordinal()] == FramePart.WIDE ||
                edgeParts[vertex.getEdgeZX().ordinal()] == FramePart.WIDE ||
                edgeParts[vertex.getEdgeYX().ordinal()] == FramePart.WIDE) {
            return FramePart.HIDDEN;
        }

        ArrayDeque<EdgeQuery> edgeQueries = new ArrayDeque<>();

        if (sideConnections[vertex.getFacingY().ordinal()] == FrameConnection.CONNECTED_TO_FRAME) {
            Axis axis = vertex.getFacingY().getAxis();
            edgeQueries.add(new EdgeQuery(vertex.flip(axis), ctx.origin().move(vertex.getFacingY()), vertex.getEdgeZX(),
                    axis != Axis.X,
                    axis != Axis.Y,
                    axis != Axis.Z));
        }

        if (sideConnections[vertex.getFacingZ().ordinal()] == FrameConnection.CONNECTED_TO_FRAME) {
            Axis axis = vertex.getFacingZ().getAxis();
            edgeQueries.add(new EdgeQuery(vertex.flip(axis), ctx.origin().move(vertex.getFacingZ()), vertex.getEdgeYX(),
                    axis != Axis.X,
                    axis != Axis.Y,
                    axis != Axis.Z));
        }

        if (sideConnections[vertex.getFacingX().ordinal()] == FrameConnection.CONNECTED_TO_FRAME) {
            Axis axis = vertex.getFacingX().getAxis();
            edgeQueries.add(new EdgeQuery(vertex.flip(axis), ctx.origin().move(vertex.getFacingX()), vertex.getEdgeYZ(),
                    axis != Axis.X,
                    axis != Axis.Y,
                    axis != Axis.Z));
        }

        boolean shouldSlimEdgeBeVisible = false;

        while (!edgeQueries.isEmpty()) {
            EdgeQuery q = edgeQueries.removeFirst();
            MutableBlockPos pos = ctx.origin().setPos(q.posX, q.posY, q.posZ);

            FrameConnection sideA = connection(ctx.world, pos, q.edge.getFacingA());
            FrameConnection sideB = connection(ctx.world, pos, q.edge.getFacingB());

            FramePart part = computeEdge(new WorldContext(ctx.world, pos), q.edge, sideA, sideB);

            switch (part) {
                case WIDE -> {
                    return FramePart.WIDE;
                }
                case SLIM -> shouldSlimEdgeBeVisible = true;
            }
            boolean continueToX = q.continueToX;
            boolean continueToY = q.continueToY;
            boolean continueToZ = q.continueToZ;

            if (sideA != FrameConnection.CONNECTED_TO_FRAME) {
                switch (q.edge.getFacingA().getAxis()) {
                    case X -> continueToX = false;
                    case Y -> continueToY = false;
                    case Z -> continueToZ = false;
                }
            }
            if (sideB != FrameConnection.CONNECTED_TO_FRAME) {
                switch (q.edge.getFacingB().getAxis()) {
                    case X -> continueToX = false;
                    case Y -> continueToY = false;
                    case Z -> continueToZ = false;
                }
            }

            if (continueToX) {
                CubeVertex vertex2 = q.vertex.flipX();
                edgeQueries.add(new EdgeQuery(
                        vertex2,
                        vertex.getFacingX() == vertex2.getFacingX() ? q.posX : q.posX + vertex.getFacingX().getXOffset(),
                        q.posY,
                        q.posZ,
                        vertex2.getEdgeYZ(),
                        false, q.continueToY, q.continueToZ));
            }

            if (continueToY) {
                CubeVertex vertex2 = q.vertex.flipY();
                edgeQueries.add(new EdgeQuery(
                        vertex2,
                        q.posX,
                        vertex.getFacingY() == vertex2.getFacingY() ? q.posY : q.posY + vertex.getFacingY().getYOffset(),
                        q.posZ,
                        vertex2.getEdgeZX(),
                        q.continueToX, false, q.continueToZ));
            }

            if (continueToZ) {
                CubeVertex vertex2 = q.vertex.flipZ();
                edgeQueries.add(new EdgeQuery(
                        vertex2,
                        q.posX,
                        q.posY,
                        vertex.getFacingZ() == vertex2.getFacingZ() ? q.posZ : q.posZ + vertex.getFacingZ().getZOffset(),
                        vertex2.getEdgeYX(),
                        q.continueToX, q.continueToY, false));
            }
        }

        boolean hasSlimEdge = edgeParts[vertex.getEdgeYZ().ordinal()] == FramePart.SLIM ||
                edgeParts[vertex.getEdgeZX().ordinal()] == FramePart.SLIM ||
                edgeParts[vertex.getEdgeYX().ordinal()] == FramePart.SLIM;

        return !hasSlimEdge && shouldSlimEdgeBeVisible ? FramePart.SLIM : FramePart.HIDDEN;
    }

    private static FrameConnection connection(IBlockAccess world, BlockPos pos, EnumFacing side) {
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

    private enum FrameConnection {
        NONE,
        CONNECTED_TO_BLOCK,
        CONNECTED_TO_FRAME
    }

    private enum FramePart {
        HIDDEN,
        SLIM,
        WIDE
    }

    private static final class EdgeQuery {

        private final CubeVertex vertex;
        private final int posX;
        private final int posY;
        private final int posZ;
        private final CubeEdge edge;
        private final boolean continueToX;
        private final boolean continueToY;
        private final boolean continueToZ;

        private EdgeQuery(CubeVertex vertex, Vec3i pos, CubeEdge edge, boolean continueToX, boolean continueToY, boolean continueToZ) {
            this(vertex, pos.getX(), pos.getY(), pos.getZ(), edge, continueToX, continueToY, continueToZ);
        }

        private EdgeQuery(CubeVertex vertex, int posX, int posY, int posZ, CubeEdge edge, boolean continueToX, boolean continueToY, boolean continueToZ) {
            this.vertex = vertex;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.edge = edge;
            this.continueToX = continueToX;
            this.continueToY = continueToY;
            this.continueToZ = continueToZ;
        }
    }
}
