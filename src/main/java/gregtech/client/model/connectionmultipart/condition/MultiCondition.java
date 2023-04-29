package gregtech.client.model.connectionmultipart.condition;

import gregtech.common.blocks.extendedstate.ConnectionState;

import javax.annotation.Nonnull;

final class MultiCondition extends ConnectionVariantCondition {

    private final boolean or;
    private final ConnectionVariantCondition[] variants;

    MultiCondition(boolean or, @Nonnull ConnectionVariantCondition[] variants) {
        this.or = or;
        this.variants = variants;
    }

    @Override
    public boolean matches(@Nonnull ConnectionState state) {
        if (this.or) {
            for (ConnectionVariantCondition v : this.variants) {
                if (v.matches(state)) return true;
            }
            return false;
        } else {
            for (ConnectionVariantCondition v : this.variants) {
                if (!v.matches(state)) return false;
            }
            return true;
        }
    }
}
