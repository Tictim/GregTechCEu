package gregtech.client.model.special;

import net.minecraftforge.client.model.IModel;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ModelPartRegistry {

    private final List<IModel> parts = new ArrayList<>();
    private boolean ambientOcclusion = true;
    private boolean gui3d = true;
    private boolean uvLock = false;

    @Nonnull
    public List<IModel> parts() {
        return parts;
    }

    public boolean ambientOcclusion() {
        return ambientOcclusion;
    }

    public boolean gui3d() {
        return gui3d;
    }

    public boolean uvLock() {
        return uvLock;
    }

    public void registerPart(int expectedIndex, @Nonnull IModel model) {
        int i = parts.size();
        if (i != expectedIndex) {
            throw new IllegalStateException("Wrong ID expected for part, expected: " + expectedIndex + ", actual: " + i);
        } else {
            this.parts.add(Objects.requireNonNull(model, "model == null"));
        }
    }

    @CheckReturnValue
    public int registerPart(@Nonnull IModel model) {
        this.parts.add(Objects.requireNonNull(model, "model == null"));
        return parts.size() - 1;
    }

    @CheckReturnValue
    public <E extends Enum<E>> EnumIndexedPart<E> registerPart(@Nonnull Class<E> enumClass, @Nonnull Function<E, IModel> factory) {
        int start = this.parts.size();
        for (E e : enumClass.getEnumConstants()) {
            registerPart(start, factory.apply(e));
        }
        return new EnumIndexedPart<>(start);
    }

    public void setAmbientOcclusion(boolean ambientOcclusion) {
        this.ambientOcclusion = ambientOcclusion;
    }

    public void setGui3d(boolean gui3d) {
        this.gui3d = gui3d;
    }

    public void setUvLock(boolean uvLock) {
        this.uvLock = uvLock;
    }
}
