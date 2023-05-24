package gregtech.client.model;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public final class SimpleModel implements IModel {

    private static final BlockPartRotation DEFAULT_ROTATION = new BlockPartRotation(
            new Vector3f(),
            EnumFacing.Axis.Y,
            0,
            false);

    @Nonnull
    public static SimpleModelBuilder builder() {
        return new SimpleModelBuilder();
    }

    private final List<BlockPart> blockParts;
    private final boolean uvLock;
    private final boolean gui3d;
    private final boolean ambientOcclusion;

    private final Map<String, String> textureMappings;

    SimpleModel(@Nonnull List<BlockPart> blockParts, boolean uvLock, boolean gui3d, boolean ambientOcclusion) {
        this(blockParts, uvLock, gui3d, ambientOcclusion, Collections.emptyMap());
    }

    private SimpleModel(@Nonnull List<BlockPart> blockParts, boolean uvLock, boolean gui3d, boolean ambientOcclusion, @Nonnull Map<String, String> textureMappings) {
        this.blockParts = blockParts;
        this.uvLock = uvLock;
        this.gui3d = gui3d;
        this.ambientOcclusion = ambientOcclusion;
        this.textureMappings = textureMappings;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        Set<String> tex = new ObjectOpenHashSet<>();
        for (BlockPart part : blockParts) {
            for (BlockPartFace face : part.mapFaces.values()) {
                tex.add(face.texture);
            }
        }
        Set<ResourceLocation> tex2 = new ObjectOpenHashSet<>();
        for (String s : tex) {
            tex2.add(new ResourceLocation(this.textureMappings.getOrDefault(s, s)));
        }
        tex2.add(new ResourceLocation(this.textureMappings.getOrDefault("particle", "particle")));
        return tex2;
    }

    @Override
    public SimpleModel uvlock(boolean value) {
        if (this.uvLock == value) return this;
        return new SimpleModel(this.blockParts, value, this.gui3d, this.ambientOcclusion);
    }

    @Override
    public SimpleModel smoothLighting(boolean value) {
        if (this.ambientOcclusion == value) return this;
        return new SimpleModel(this.blockParts, this.uvLock, this.gui3d, value);
    }

    @Override
    public SimpleModel gui3d(boolean value) {
        if (this.gui3d == value) return this;
        return new SimpleModel(this.blockParts, this.uvLock, value, this.ambientOcclusion);
    }

    @Override
    public SimpleModel retexture(@Nonnull ImmutableMap<String, String> textures) {
        if (textures.isEmpty()) return this;
        Map<String, String> newTextureMap = new Object2ObjectOpenHashMap<>(this.textureMappings);
        newTextureMap.putAll(textures);
        return new SimpleModel(this.blockParts, this.uvLock, this.gui3d, this.ambientOcclusion, newTextureMap);
    }

    @Nonnull
    @Override
    public IBakedModel bake(@Nonnull IModelState state, @Nonnull VertexFormat format, @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        TRSRTransformation transformation = state.apply(Optional.empty()).orElse(TRSRTransformation.identity()); // cringe

        List<BakedQuad> generalQuads = null;
        Map<EnumFacing, List<BakedQuad>> faceQuads = new EnumMap<>(EnumFacing.class);
        Map<String, TextureAtlasSprite> spriteCache = new Object2ObjectOpenHashMap<>();

        for (BlockPart part : this.blockParts) {
            for (Map.Entry<EnumFacing, BlockPartFace> e : part.mapFaces.entrySet()) {
                BlockPartFace face = e.getValue();
                TextureAtlasSprite sprite = spriteCache.computeIfAbsent(face.texture, t -> getMappedSprite(bakedTextureGetter, t));

                if (face.cullFace == null) {
                    if (generalQuads == null) {
                        generalQuads = new ArrayList<>();
                    }
                    generalQuads.add(makeBakedQuad(part, face, sprite, e.getKey(), transformation, this.uvLock));
                } else {
                    faceQuads.computeIfAbsent(transformation.rotate(face.cullFace), f -> new ArrayList<>())
                            .add(makeBakedQuad(part, face, sprite, e.getKey(), transformation, this.uvLock));
                }
            }
        }

        for (EnumFacing facing : EnumFacing.VALUES) {
            faceQuads.putIfAbsent(facing, Collections.emptyList());
        }

        return new SimpleBakedModel(
                generalQuads != null ? generalQuads : Collections.emptyList(), faceQuads, this.ambientOcclusion,
                this.gui3d, getMappedSprite(bakedTextureGetter, "particle"),
                ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
    }

    @Nonnull
    private static BakedQuad makeBakedQuad(BlockPart part, BlockPartFace face, TextureAtlasSprite sprite,
                                           EnumFacing facing, TRSRTransformation transformation, boolean uvLock) {
        return ModelFactory.getBakery().makeBakedQuad(part.positionFrom, part.positionTo, face, sprite, facing,
                transformation, part.partRotation == null ? DEFAULT_ROTATION : part.partRotation, uvLock, part.shade);
    }

    @Nonnull
    private TextureAtlasSprite getMappedSprite(@Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                                               @Nullable String texture) {
        ResourceLocation textureLocation = texture == null ?
                TextureMap.LOCATION_MISSING_TEXTURE :
                new ResourceLocation(this.textureMappings.getOrDefault(texture, texture));
        return bakedTextureGetter.apply(textureLocation);
    }
}
