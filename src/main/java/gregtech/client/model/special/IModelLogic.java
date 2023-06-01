package gregtech.client.model.special;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionalInterface
public interface IModelLogic {

    void collectModels(@Nonnull ModelCollector collector, @Nullable WorldContext ctx);
}
