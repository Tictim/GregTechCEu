package gregtech.client.model.connectionmultipart.condition;

import gregtech.common.blocks.extendedstate.ConnectionState;

import javax.annotation.Nonnull;

public abstract class ConnectionVariantCondition {

    public static ConnectionVariantCondition always() {
        return ConstCondition.TRUE;
    }

    public static ConnectionVariantCondition never() {
        return ConstCondition.FALSE;
    }

    ConnectionVariantCondition() {}

    public abstract boolean matches(@Nonnull ConnectionState state);
}