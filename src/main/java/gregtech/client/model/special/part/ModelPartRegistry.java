package gregtech.client.model.special.part;

import gregtech.client.model.component.EnumIndexedPart;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraftforge.client.model.IModel;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

public final class ModelPartRegistry {

    private final List<ModelPartEntry> parts = new ArrayList<>();
    private boolean ambientOcclusion = true;
    private boolean gui3d = true;
    private boolean uvLock = false;

    @Nonnull
    public List<ModelPartEntry> parts() {
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

    @CheckReturnValue
    public int registerPart(@Nonnull ModelPartEntry entry) {
        this.parts.add(entry);
        return parts.size() - 1;
    }

    @CheckReturnValue
    public int registerPart(@Nonnull IModel model) {
        return registerPart(new ModelPart(model));
    }

    @CheckReturnValue
    public int registerPart(@Nonnull IBakedModel bakedModel) {
        return registerPart(new BakedModelPart(bakedModel));
    }

    @Nonnull
    public OptionalPartRegister<Integer> ifTextureExists(@Nonnull String optionalTexture) {
        return new OptionalPartRegister<>(Collections.singleton(optionalTexture), this::registerPart);
    }

    @Nonnull
    public OptionalPartRegister<Integer> ifTextureExists(@Nonnull String... optionalTextures) {
        if (optionalTextures.length == 0) throw new IllegalArgumentException("optionalTextures.length == 0");
        return new OptionalPartRegister<>(new ObjectOpenHashSet<>(optionalTextures), this::registerPart);
    }

    @CheckReturnValue
    public <E extends Enum<E>> EnumIndexedPart<E> registerParts(@Nonnull Class<E> enumClass,
                                                                @Nonnull ToIntBiFunction<SubRegistry, E> factory) {
        int start = this.parts.size();
        for (E e : enumClass.getEnumConstants()) {
            factory.applyAsInt(new SubRegistry(this::registerPart), e);
            if (this.parts.size() != start + e.ordinal() + 1) {
                throw new IllegalStateException("Factory for registerPart() must register one model per enum element");
            }
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

    public static final class SubRegistry {

        private final ToIntFunction<ModelPartEntry> register;

        public SubRegistry(@Nonnull ToIntFunction<ModelPartEntry> register) {
            this.register = register;
        }

        public int registerPart(@Nonnull IModel model) {
            return register.applyAsInt(new ModelPart(model));
        }

        public int registerPart(@Nonnull IBakedModel bakedModel) {
            return register.applyAsInt(new BakedModelPart(bakedModel));
        }

        @Nonnull
        public OptionalPartRegister<Integer> ifTextureExists(@Nonnull String optionalTexture) {
            return new OptionalPartRegister<>(Collections.singleton(optionalTexture), register::applyAsInt);
        }

        @Nonnull
        public OptionalPartRegister<Integer> ifTextureExists(@Nonnull String... optionalTextures) {
            if (optionalTextures.length == 0) throw new IllegalArgumentException("optionalTextures.length == 0");
            return new OptionalPartRegister<>(new ObjectOpenHashSet<>(optionalTextures), register::applyAsInt);
        }
    }
}
