package gregtech.client.model.frame;

import gregtech.client.model.component.*;
import gregtech.client.utils.CubeEdge;
import gregtech.client.utils.CubeVertex;

import javax.annotation.Nonnull;

public final class FrameModelLogicProvider implements IComponentLogicProvider {

    public static final FrameModelLogicProvider INSTANCE = new FrameModelLogicProvider();

    public static final int TINT_OUTER = 1;
    public static final int TINT_INNER = 2;

    private static final ComponentTexture BORDER_TEXTURE = new ComponentTexture("#border", TINT_INNER);

    private FrameModelLogicProvider() {}

    @Nonnull
    @Override
    public IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister, @Nonnull ModelTextureMapping textureMapping) {
        return new FrameModelLogic(
                componentRegister.addForEachFacing((f, b) -> {
                    String texture = switch (f) {
                        case UP -> "#top";
                        case DOWN -> "#bottom";
                        default -> "#side";
                    };

                    b.accept(new Component(
                            f.getXOffset() > 0 ? 15 : 0,
                            f.getYOffset() > 0 ? 15 : 0,
                            f.getZOffset() > 0 ? 15 : 0,
                            f.getXOffset() < 0 ? 1 : 16,
                            f.getYOffset() < 0 ? 1 : 16,
                            f.getZOffset() < 0 ? 1 : 16)
                            .addFace(new ComponentTexture(texture, TINT_OUTER), f, f)
                            .addFace(new ComponentTexture(texture, TINT_INNER), f.getOpposite(), f));
                }),
                componentRegister.addForEachEnum(CubeEdge.class, (e, b) -> b.accept(edgeModel(e, 1))),
                componentRegister.addForEachEnum(CubeEdge.class, (e1, b) -> b.accept(edgeModel(e1, 2))),
                componentRegister.addForEachEnum(CubeVertex.class, (v, b) -> b.accept(vertexModel(v, 1))),
                componentRegister.addForEachEnum(CubeVertex.class, (v1, b) -> b.accept(vertexModel(v1, 2))));
    }

    private static Component edgeModel(CubeEdge edge, float thickness) {
        return new Component(
                edge.getDirection().getX() > 0 ? 16 - thickness : 0,
                edge.getDirection().getY() > 0 ? 16 - thickness : 0,
                edge.getDirection().getZ() > 0 ? 16 - thickness : 0,
                edge.getDirection().getX() < 0 ? thickness : 16,
                edge.getDirection().getY() < 0 ? thickness : 16,
                edge.getDirection().getZ() < 0 ? thickness : 16)
                .addFace(BORDER_TEXTURE, edge.getFacingA().getOpposite())
                .addFace(BORDER_TEXTURE, edge.getFacingB().getOpposite());
    }

    private static Component vertexModel(CubeVertex vertex, float thickness) {
        return new Component(
                vertex.getDirection().getX() > 0 ? 16 - thickness : 0,
                vertex.getDirection().getY() > 0 ? 16 - thickness : 0,
                vertex.getDirection().getZ() > 0 ? 16 - thickness : 0,
                vertex.getDirection().getX() < 0 ? thickness : 16,
                vertex.getDirection().getY() < 0 ? thickness : 16,
                vertex.getDirection().getZ() < 0 ? thickness : 16)
                .addFace(BORDER_TEXTURE, vertex.getFacingX().getOpposite())
                .addFace(BORDER_TEXTURE, vertex.getFacingY().getOpposite())
                .addFace(BORDER_TEXTURE, vertex.getFacingZ().getOpposite());
    }
}
