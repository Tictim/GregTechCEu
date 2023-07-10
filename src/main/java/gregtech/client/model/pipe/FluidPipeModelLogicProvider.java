package gregtech.client.model.pipe;

import gregtech.client.model.component.*;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

import static net.minecraft.util.EnumFacing.*;

@ParametersAreNonnullByDefault
public class FluidPipeModelLogicProvider extends PipeModelLogicProvider {

    protected static final String TEXTURE_RESTRICTED_OVERLAY = "#restricted_overlay";

    protected static final ComponentTexture RESTRICTION_TEXTURE = new ComponentTexture(TEXTURE_RESTRICTED_OVERLAY, TINT_OVERLAY);

    public FluidPipeModelLogicProvider(float thickness) {
        super(thickness);
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping) {
        return new FluidPipeModelLogic(
                registerBaseModels(componentRegister, textureMapping),
                componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, false)),
                componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, true)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, false)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, true)),
                componentRegister.addForEachFacing((f, b) -> registerRestrictedSideModels(f, b, textureMapping)),
                componentRegister.addForEachFacing((f, b) -> registerRestrictedExtrusionModels(f, b, textureMapping)));
    }

    protected void registerRestrictedSideModels(EnumFacing facing, Consumer<Component> consumer, ModelTextureMapping textureMapping) {
        consumer.accept(new Component(
                facing == WEST ? 0 : facing == EAST ? 8 : modelStart,
                facing == DOWN ? 0 : facing == UP ? 8 : modelStart,
                facing == NORTH ? 0 : facing == SOUTH ? 8 : modelStart,
                facing == EAST ? 16 : facing == WEST ? 8 : modelEnd,
                facing == UP ? 16 : facing == DOWN ? 8 : modelEnd,
                facing == SOUTH ? 16 : facing == NORTH ? 8 : modelEnd)
                .addFaces(RESTRICTION_TEXTURE, f -> f.getAxis() != facing.getAxis()));
    }

    protected void registerRestrictedExtrusionModels(EnumFacing facing, Consumer<Component> consumer, ModelTextureMapping textureMapping) {
        consumer.accept(new Component(
                facing == WEST ? 0 - PIPE_EXTRUSION_SIZE : facing == EAST ? 16 : modelStart,
                facing == DOWN ? 0 - PIPE_EXTRUSION_SIZE : facing == UP ? 16 : modelStart,
                facing == NORTH ? 0 - PIPE_EXTRUSION_SIZE : facing == SOUTH ? 16 : modelStart,
                facing == EAST ? 16 + PIPE_EXTRUSION_SIZE : facing == WEST ? 0 : modelEnd,
                facing == UP ? 16 + PIPE_EXTRUSION_SIZE : facing == DOWN ? 0 : modelEnd,
                facing == SOUTH ? 16 + PIPE_EXTRUSION_SIZE : facing == NORTH ? 0 : modelEnd)
                .addFaces(RESTRICTION_TEXTURE, f -> f.getAxis() != facing.getAxis(), facing));
    }
}
