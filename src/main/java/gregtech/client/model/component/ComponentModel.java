package gregtech.client.model.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gregtech.api.GTValues;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ComponentModel implements IModel {

    private final IComponentLogicProvider logicProvider;

    private final boolean ambientOcclusion;
    private final boolean gui3d;
    private final boolean uvLock;

    private final ModelTextureMapping textureMappings;

    @Nullable
    private ImmutableList<ImmutableList<Component>> parts;

    @Nullable
    private IComponentLogic logic;

    public ComponentModel(@Nonnull IComponentLogicProvider logicProvider) {
        this(logicProvider,
                logicProvider.defaultAmbientOcclusion(),
                logicProvider.defaultGui3d(),
                logicProvider.defaultUVLock(),
                logicProvider.getDefaultTextureMappings());
    }

    public ComponentModel(@Nonnull IComponentLogicProvider logicProvider,
                          boolean ambientOcclusion,
                          boolean gui3d,
                          boolean uvLock,
                          @Nonnull ModelTextureMapping textureMappings) {
        this.logicProvider = Objects.requireNonNull(logicProvider);
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;
        this.uvLock = uvLock;
        this.textureMappings = Objects.requireNonNull(textureMappings);
    }

    @Nonnull
    public final IComponentLogicProvider getLogicProvider() {
        return logicProvider;
    }

    public final boolean ambientOcclusion() {
        return ambientOcclusion;
    }

    public final boolean gui3d() {
        return gui3d;
    }

    public final boolean uvLock() {
        return uvLock;
    }

    @Nonnull
    public final ModelTextureMapping getTextureMappings() {
        return textureMappings;
    }

    private void buildLogic() {
        Register register = new Register();
        this.logic = Objects.requireNonNull(this.logicProvider.buildLogic(register, this.textureMappings),
                "Logic provider returned null");
        this.parts = register.builder.build();
        for (var parts : this.parts) {
            for (Component part : parts) {
                part.lock();
            }
        }
    }

    @Nonnull
    public final ImmutableList<ImmutableList<Component>> getParts() {
        if (this.parts == null) {
            buildLogic();
        }
        return this.parts;
    }

    @Nonnull
    public final IComponentLogic getLogic() {
        if (this.logic == null) {
            buildLogic();
        }
        return this.logic;
    }

    @Nonnull
    @Override
    public Collection<ResourceLocation> getTextures() {
        Set<ResourceLocation> textures = new ObjectOpenHashSet<>();
        for (var parts : getParts()) {
            for (Component part : parts) {
                for (ComponentFace face : part.getFaces().values()) {
                    ResourceLocation texture = this.textureMappings.get(face.texture.texture());
                    if (texture != null) {
                        textures.add(texture);
                    }
                }
            }
        }
        ResourceLocation particleTexture = this.textureMappings.get("#particle");
        if (particleTexture != null) {
            textures.add(particleTexture);
        }
        return textures;
    }

    @Nonnull
    @Override
    public ComponentModel process(ImmutableMap<String, String> customData) {
        return this;
    }

    @Nonnull
    @Override
    public ComponentModel smoothLighting(boolean value) {
        if (this.ambientOcclusion == value) return this;
        return new ComponentModel(this.logicProvider, value, this.gui3d, this.uvLock, this.textureMappings);
    }

    @Nonnull
    @Override
    public ComponentModel gui3d(boolean value) {
        if (this.gui3d == value) return this;
        return new ComponentModel(this.logicProvider, this.ambientOcclusion, value, this.uvLock, this.textureMappings);
    }

    @Nonnull
    @Override
    public ComponentModel uvlock(boolean value) {
        if (this.uvLock == value) return this;
        return new ComponentModel(this.logicProvider, this.ambientOcclusion, this.gui3d, value, this.textureMappings);
    }

    @Nonnull
    @Override
    public ComponentModel retexture(@Nonnull ImmutableMap<String, String> textures) {
        if (textures.isEmpty()) return this;
        return new ComponentModel(this.logicProvider, this.ambientOcclusion, this.gui3d, this.uvLock, new ModelTextureMapping(textures, this.textureMappings));
    }

    @Nonnull
    @Override
    public IBakedModel bake(@Nonnull IModelState state,
                            @Nonnull VertexFormat format,
                            @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        if (Loader.isModLoaded(GTValues.MODID_CTM)) {
            IBakedModel baked = SpecialConnectedModel.bakeConnectedTextureModel(this, state, format, bakedTextureGetter,
                    this.textureMappings.getTextureOrMissing("#particle", bakedTextureGetter),
                    this.ambientOcclusion,
                    this.gui3d
            );
            if (baked != null) return baked;
        }
        return new BakedComponentModel(
                new BakedComponent(this, state, format, bakedTextureGetter),
                this.getLogic(),
                this.textureMappings.getTextureOrMissing("#particle", bakedTextureGetter),
                this.ambientOcclusion,
                this.gui3d);
    }

    public static final class Register {

        private final ImmutableList.Builder<ImmutableList<Component>> builder = ImmutableList.builder();
        private int id;

        public int add(@Nonnull Component... components) {
            Objects.requireNonNull(components, "part == null");
            if (components.length == 0) {
                throw new IllegalArgumentException("part.length == 0");
            }
            for (Component component : components) {
                Objects.requireNonNull(component, "one of the parts is null");
            }

            this.builder.add(ImmutableList.copyOf(components));
            return this.id++;
        }

        @Nonnull
        public <E extends Enum<E>> EnumIndexedPart<E> addForEachEnum(@Nonnull Class<E> enumClass, @Nonnull BiConsumer<E, ImmutableList.Builder<Component>> factory) {
            E[] enumConstants = enumClass.getEnumConstants();
            EnumIndexedPart<E> parts = new EnumIndexedPart<>(this.id);
            int expectedIndex = this.id;
            for (E e : enumConstants) {
                ImmutableList.Builder<Component> r = new ImmutableList.Builder<>();
                factory.accept(e, r);
                ImmutableList<Component> components = r.build();
                for (Component component : components) {
                    Objects.requireNonNull(component, "one of the parts is null");
                }
                if (expectedIndex != this.id) {
                    throw new ConcurrentModificationException();
                }
                this.builder.add(components);
                this.id++;
                expectedIndex++;
            }
            return parts;
        }
    }
}
