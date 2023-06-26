package gregtech.client.model.frame;

import gregtech.client.model.SimpleModel;
import gregtech.client.model.special.IModeLogicProvider;
import gregtech.client.model.special.IModelLogic;
import gregtech.client.model.special.part.ModelPartRegistry;
import gregtech.client.utils.CubeEdge;
import gregtech.client.utils.CubeVertex;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IModel;

import javax.annotation.Nonnull;

public final class FrameModelLogicProvider implements IModeLogicProvider {

    public static final FrameModelLogicProvider INSTANCE = new FrameModelLogicProvider();

    public static final int TINT_OUTER = 1;
    public static final int TINT_INNER = 2;

    private FrameModelLogicProvider() {}

    @Nonnull
    @Override
    public IModelLogic createLogic(@Nonnull ModelPartRegistry registry) {
        return new FrameModelLogic(
                registry.registerParts(EnumFacing.class, (r, f) -> r.registerPart(sideModel(f))),
                registry.registerParts(CubeEdge.class, (r, e) -> r.registerPart(edgeModel(e, 1))),
                registry.registerParts(CubeEdge.class, (r, e1) -> r.registerPart(edgeModel(e1, 2))),
                registry.registerParts(CubeVertex.class, (r, v) -> r.registerPart(vertexModel(v, 1))),
                registry.registerParts(CubeVertex.class, (r, v1) -> r.registerPart(vertexModel(v1, 2))));
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
                .forSide(side).texture(sideTexture(side)).cullFace(side).tintIndex(TINT_OUTER).finishSide()
                .forSide(side.getOpposite()).texture(sideTexture(side)).cullFace(side).tintIndex(TINT_INNER).finishSide()
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
                        edge.getFacingB().getOpposite()).texture("#border").tintIndex(TINT_INNER).finishSide()
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
                        vertex.getFacingX().getOpposite()).texture("#border").tintIndex(TINT_INNER).finishSide()
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
