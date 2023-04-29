package gregtech.client.model.connectionmultipart.condition;

import gregtech.common.blocks.extendedstate.ConnectionState;

import javax.annotation.Nonnull;

final class ConstCondition extends ConnectionVariantCondition {

    static final ConstCondition TRUE = new ConstCondition(true);
    static final ConstCondition FALSE = new ConstCondition(false);

    private final boolean value;

    ConstCondition(boolean value) {
        this.value = value;
    }

    @Override
    public boolean matches(@Nonnull ConnectionState state) {
        return value;
    }
}
