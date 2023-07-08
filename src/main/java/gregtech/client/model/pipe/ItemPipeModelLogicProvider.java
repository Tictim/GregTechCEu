package gregtech.client.model.pipe;

import gregtech.client.model.component.*;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

import static net.minecraft.util.EnumFacing.*;

@ParametersAreNonnullByDefault
public class ItemPipeModelLogicProvider extends PipeModelLogicProvider {

    protected static final String TEXTURE_RESTRICTED_OVERLAY = "#restricted_overlay";

    protected static final ComponentTexture RESTRICTION_TEXTURE = new ComponentTexture(TEXTURE_RESTRICTED_OVERLAY, TINT_OVERLAY);

    private final boolean restrictive;

    public ItemPipeModelLogicProvider(float thickness, boolean restrictive) {
        super(thickness);
        this.restrictive = restrictive;
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping) {
        return new ItemPipeModelLogic(
                registerBaseModels(componentRegister, textureMapping),
                componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, false)),
                componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, true)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, false)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, true)));
    }

    @Override
    protected void registerExtrusionModels(EnumFacing facing, Consumer<Component> consumer, ModelTextureMapping textureMapping, boolean closed) {
        super.registerExtrusionModels(facing, consumer, textureMapping, closed);
    }

    @Override
    protected int connectionlessModel(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping) {
        int id = super.connectionlessModel(componentRegister, textureMapping);

        if (this.restrictive) {
            componentRegister.append(id, new Component(
                    modelStart, modelStart, modelStart,
                    modelEnd, modelEnd, modelEnd)
                    .addAllFaces(RESTRICTION_TEXTURE, true));
        }

        return id;
    }

    @Override
    protected int singleBranchModel(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping, EnumFacing connectedSide) {
        int id = super.singleBranchModel(componentRegister, textureMapping, connectedSide);

        if (this.restrictive) {
            componentRegister.append(id, new Component(
                    connectedSide == WEST ? 0 : modelStart,
                    connectedSide == DOWN ? 0 : modelStart,
                    connectedSide == NORTH ? 0 : modelStart,
                    connectedSide == EAST ? 16 : modelEnd,
                    connectedSide == UP ? 16 : modelEnd,
                    connectedSide == SOUTH ? 16 : modelEnd)
                    .addFaces(RESTRICTION_TEXTURE, f -> f.getAxis() != connectedSide.getAxis()));
        }

        return id;
    }

    @Override
    protected int straightModel(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping, EnumFacing.Axis axis) {
        int id = super.straightModel(componentRegister, textureMapping, axis);

        if (this.restrictive) {
            componentRegister.append(id, new Component(
                    axis == Axis.X ? 0 : modelStart,
                    axis == Axis.Y ? 0 : modelStart,
                    axis == Axis.Z ? 0 : modelStart,
                    axis == Axis.X ? 16 : modelEnd,
                    axis == Axis.Y ? 16 : modelEnd,
                    axis == Axis.Z ? 16 : modelEnd)
                    .addFaces(RESTRICTION_TEXTURE, f -> f.getAxis() != axis));
        }

        return id;
    }

    @Override
    protected int complexModel(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping, int blockConnections, boolean jointed) {
        int id = super.complexModel(componentRegister, textureMapping, blockConnections, jointed);

        if (this.restrictive) {
            Component center = null;

            for (EnumFacing side : EnumFacing.VALUES) {
                if (PipeModelLogic.isConnected(blockConnections, side)) {
                    componentRegister.append(id, new Component(
                            side == WEST ? 0 : side == EAST ? modelEnd : modelStart,
                            side == DOWN ? 0 : side == UP ? modelEnd : modelStart,
                            side == NORTH ? 0 : side == SOUTH ? modelEnd : modelStart,
                            side == EAST ? 16 : side == WEST ? modelStart : modelEnd,
                            side == UP ? 16 : side == DOWN ? modelStart : modelEnd,
                            side == SOUTH ? 16 : side == NORTH ? modelStart : modelEnd)
                            .addFaces(RESTRICTION_TEXTURE, f -> f.getAxis() != side.getAxis()));
                } else {
                    if (center == null) {
                        center = new Component(
                                modelStart, modelStart, modelStart,
                                modelEnd, modelEnd, modelEnd);
                        componentRegister.append(id, center);
                    }
                    center.addFace(SIDE_TEXTURE, side);
                }
            }
        }

        return id;
    }
}
