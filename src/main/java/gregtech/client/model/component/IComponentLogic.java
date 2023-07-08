package gregtech.client.model.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionalInterface
public interface IComponentLogic {

    void computeStates(@Nonnull ModelStates states, @Nullable WorldContext ctx);

    default boolean isBloomActive() {
        return true;
    }
}
