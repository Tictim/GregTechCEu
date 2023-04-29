package gregtech.client.model.connectionmultipart.condition;

import gregtech.common.blocks.extendedstate.ConnectionState;

import javax.annotation.Nonnull;

final class ConnectionCondition extends ConnectionVariantCondition {

    private final int flagMask;
    private final int flagValue;

    ConnectionCondition(int flagMask, int flagValue) {
        this.flagMask = flagMask & ConnectionState.ALL_FLAGS;
        this.flagValue = flagValue & this.flagMask;
    }

    @Override
    public boolean matches(@Nonnull ConnectionState state) {
        return state.matchesConnection(flagMask, flagValue);
    }
}
