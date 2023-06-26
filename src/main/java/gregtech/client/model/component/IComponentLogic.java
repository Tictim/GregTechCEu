package gregtech.client.model.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionalInterface
public interface IComponentLogic {

    void collectModels(@Nonnull ModelCollector collector, @Nullable WorldContext ctx);

    default boolean isBloomActive() {
        return true;
    }
}
