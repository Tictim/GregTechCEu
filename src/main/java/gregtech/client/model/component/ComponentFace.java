package gregtech.client.model.component;

import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class ComponentFace {

    @Nonnull
    public final EnumFacing side;
    @Nonnull
    public final ComponentTexture texture;
    @Nullable
    public final EnumFacing cullFace;

    public ComponentFace(@Nonnull EnumFacing side, @Nonnull ComponentTexture texture, @Nullable EnumFacing cullFace) {
        this.side = Objects.requireNonNull(side, "side == null");
        this.texture = Objects.requireNonNull(texture, "texture == null");
        this.cullFace = cullFace;
    }

    @Override
    public String toString() {
        return "ComponentFace{" +
                "side=" + side +
                ", texture=" + texture +
                ", cullFace=" + cullFace +
                '}';
    }
}
