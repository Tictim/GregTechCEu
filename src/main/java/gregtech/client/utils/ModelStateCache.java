package gregtech.client.utils;

import gregtech.client.model.ISpecialBakedModel;
import gregtech.client.model.component.ModelStates;
import gregtech.common.blocks.special.ISpecialState;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;

public final class ModelStateCache {

    private final ISpecialState state;
    private final IdentityHashMap<ISpecialBakedModel, ModelStates> modelStateCache = new IdentityHashMap<>();

    public ModelStateCache(@Nonnull ISpecialState state) {
        this.state = state;
    }

    @Nonnull
    public ModelStates computeCache(@Nonnull ISpecialBakedModel model) {
        return this.modelStateCache.computeIfAbsent(model, m -> m.collectModels(this.state));
    }
}
