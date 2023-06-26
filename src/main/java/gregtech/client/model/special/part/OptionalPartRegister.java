package gregtech.client.model.special.part;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraftforge.client.model.IModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class OptionalPartRegister<R> {

    private final Set<String> requiredTextures;
    private final Function<ModelPartEntry, R> register;

    @Nullable
    private ModelPartEntry optional;
    @Nullable
    private ModelPartEntry alternative;

    private boolean error;

    public OptionalPartRegister(@Nonnull Set<String> requiredTextures,
                                @Nonnull Function<ModelPartEntry, R> register) {
        this.requiredTextures = requiredTextures;
        this.register = register;

        if (this.requiredTextures.contains(null)) {
            throw new IllegalArgumentException("Cannot set null as required texture");
        }
    }

    @Nonnull
    public OptionalPartRegister<R> registerPart(@Nonnull ModelPartEntry entry) {
        this.optional = Objects.requireNonNull(entry, "entry == null");
        return this;
    }

    @Nonnull
    public OptionalPartRegister<R> registerPart(@Nonnull IModel model) {
        this.optional = new ModelPart(model);
        return this;
    }

    @Nonnull
    public OptionalPartRegister<R> registerPart(@Nonnull IBakedModel model) {
        this.optional = new BakedModelPart(model);
        return this;
    }

    @Nonnull
    public OptionalPartRegister<R> elseThenRegisterPart(@Nonnull ModelPartEntry entry) {
        this.alternative = Objects.requireNonNull(entry, "entry == null");
        return this;
    }

    @Nonnull
    public OptionalPartRegister<R> elseThenRegisterPart(@Nonnull IModel model) {
        this.alternative = new ModelPart(model);
        return this;
    }

    @Nonnull
    public OptionalPartRegister<R> elseThenRegisterPart(@Nonnull IBakedModel model) {
        this.alternative = new BakedModelPart(model);
        return this;
    }

    @Nonnull
    public OptionalPartRegister<OptionalPartRegister<R>> elseIfTextureExists(@Nonnull String optionalTexture) {
        return new OptionalPartRegister<>(Collections.singleton(optionalTexture), this::elseThenRegisterPart);
    }

    @Nonnull
    public OptionalPartRegister<OptionalPartRegister<R>> elseIfTextureExists(@Nonnull String... optionalTextures) {
        if (optionalTextures.length == 0) throw new IllegalArgumentException("optionalTextures.length == 0");
        return new OptionalPartRegister<>(new ObjectOpenHashSet<>(optionalTextures), this::elseThenRegisterPart);
    }

    @Nonnull
    public OptionalPartRegister<R> elseThrowError() {
        this.error = true;
        return this;
    }

    @Nonnull
    public R endIf() {
        if (this.optional == null && this.alternative == null) {
            throw new IllegalStateException("Needs either optional part or alternative part");
        }
        if (this.error && this.alternative != null) {
            throw new IllegalStateException("Alternative part is redundant if it throws error");
        }
        return this.register.apply(new OptionalPart(
                this.requiredTextures,
                this.optional,
                this.alternative,
                this.error
        ));
    }
}
