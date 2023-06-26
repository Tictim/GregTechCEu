package gregtech.client.model.component;

import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class ComponentFace {

    @Nonnull
    public final ComponentTexture texture;
    @Nullable
    public final EnumFacing cullFace;

    ComponentFace(@Nonnull ComponentTexture texture, @Nullable EnumFacing cullFace) {
        this.texture = Objects.requireNonNull(texture, "texture == null");
        this.cullFace = cullFace;
    }

    @Override
    public String toString() {
        return "ComponentFace{" +
                "texture=" + texture +
                ", cullFace=" + cullFace +
                '}';
    }
}
