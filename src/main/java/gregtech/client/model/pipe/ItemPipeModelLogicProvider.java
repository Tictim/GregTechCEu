package gregtech.client.model.pipe;

import gregtech.client.model.special.IModelLogic;
import gregtech.client.model.special.part.ModelPartRegistry;

import javax.annotation.Nonnull;

public class ItemPipeModelLogicProvider extends PipeModelLogicProvider {

    private final boolean restrictive;

    public ItemPipeModelLogicProvider(float thickness, boolean restrictive) {
        super(thickness);
        this.restrictive = restrictive;
    }

    @Nonnull
    @Override
    public IModelLogic createLogic(@Nonnull ModelPartRegistry registry) {
        return new ItemPipeModelLogic(
                registerBaseModels(registry),
                registerClosedEndModels(registry),
                registerOpenEndModels(registry),
                registerOpenExtrusionModels(registry),
                registerOpenExtrusionModels(registry),
                restrictive ? registerRestrictedOverlayModels(registry) : null);
    }
}
