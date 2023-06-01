package gregtech.client.model.special.frame;

import gregtech.client.model.special.IModelLogic;
import gregtech.client.model.special.SimpleModel;
import gregtech.client.model.special.SpecialModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IModel;

import javax.annotation.Nonnull;

import static net.minecraft.util.EnumFacing.VALUES;

public class FrameModel extends SpecialModel {

    public FrameModel() {}

    protected FrameModel(@Nonnull FrameModel orig) {
        super(orig);
    }

    @Nonnull
    @Override
    protected IModelLogic buildModelLogic() {
        int[] faces = new int[EnumFacing.values().length];
        int[] slimEdges = new int[CubeEdge.values().length];
        int[] wideEdges = new int[CubeEdge.values().length];
        int[] slimVertices = new int[CubeVertex.values().length];
        int[] wideVertices = new int[CubeVertex.values().length];

        for (EnumFacing side : VALUES) {
            faces[side.ordinal()] = registerPart(sideModel(side));
        }

        for (CubeEdge edge : CubeEdge.values()) {
            slimEdges[edge.ordinal()] = registerPart(edgeModel(edge, 1));
            wideEdges[edge.ordinal()] = registerPart(edgeModel(edge, 2));
        }

        for (CubeVertex vertex : CubeVertex.values()) {
            slimVertices[vertex.ordinal()] = registerPart(vertexModel(vertex, 1));
            wideVertices[vertex.ordinal()] = registerPart(vertexModel(vertex, 2));
        }

        return new FrameModelLogic(faces, slimEdges, wideEdges, slimVertices, wideVertices);
    }

    @Nonnull
    @Override
    protected FrameModel copy() {
        return new FrameModel(this);
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
        float y2 = edge.getDirection().getY() < 0 ? thickness : 16;
        float z2 = edge.getDirection().getZ() < 0 ? thickness : 16;

        return SimpleModel.builder()
                .beginPart()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .forSide(edge.getFacingA().getOpposite(),
                        edge.getFacingB().getOpposite()).texture("#border").tintIndex(2).finishSide()
                .finishPart()
                .build();
    }

    private static IModel vertexModel(CubeVertex vertex, float thickness) {
        float x1 = vertex.getDirection().getX() > 0 ? 16 - thickness : 0;
        float y1 = vertex.getDirection().getY() > 0 ? 16 - thickness : 0;
        float z1 = vertex.getDirection().getZ() > 0 ? 16 - thickness : 0;
        float x2 = vertex.getDirection().getX() < 0 ? thickness : 16;
        float y2 = vertex.getDirection().getY() < 0 ? thickness : 16;
        float z2 = vertex.getDirection().getZ() < 0 ? thickness : 16;

        return SimpleModel.builder()
                .beginPart()
                .from(x1, y1, z1)
                .to(x2, y2, z2)
                .forSide(vertex.getFacingY().getOpposite(),
                        vertex.getFacingZ().getOpposite(),
                        vertex.getFacingX().getOpposite()).texture("#border").tintIndex(2).finishSide()
                .finishPart()
                .build();
    }

    private static String sideTexture(EnumFacing side) {
        return switch (side) {
            case UP -> "#top";
            case DOWN -> "#bottom";
            default -> "#side";
        };
    }
}
