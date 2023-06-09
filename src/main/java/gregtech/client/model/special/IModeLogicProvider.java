package gregtech.client.model.special;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

@FunctionalInterface
public interface IModeLogicProvider {

    @Nonnull
    IModelLogic createLogic(@Nonnull ModelPartRegistry registry);

    @Nonnull
    default Map<String, String> getDefaultTextureMappings() {
        return Collections.emptyMap();
    }
}
