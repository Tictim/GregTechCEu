package gregtech.client.model;

import com.google.common.collect.ImmutableMap;
import gregtech.client.model.special.ModelTextureMapping;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
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

    private final ModelTextureMapping textureMappings;

    SimpleModel(@Nonnull List<BlockPart> blockParts, boolean uvLock, boolean gui3d, boolean ambientOcclusion) {
        this(blockParts, uvLock, gui3d, ambientOcclusion, ModelTextureMapping.EMPTY);
    }

    private SimpleModel(@Nonnull List<BlockPart> blockParts, boolean uvLock, boolean gui3d, boolean ambientOcclusion, @Nonnull ModelTextureMapping textureMappings) {
        this.blockParts = blockParts;
        this.uvLock = uvLock;
        this.gui3d = gui3d;
        this.ambientOcclusion = ambientOcclusion;
        this.textureMappings = textureMappings;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        Set<ResourceLocation> tex = new ObjectOpenHashSet<>();
        for (BlockPart part : blockParts) {
            for (BlockPartFace face : part.mapFaces.values()) {
                ResourceLocation texture = textureMappings.get(face.texture);
                if (texture != null) tex.add(texture);
            }
        }
        ResourceLocation particleTexture = this.textureMappings.get("#particle");
        if (particleTexture != null) tex.add(particleTexture);
        return tex;
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
        return new SimpleModel(this.blockParts, this.uvLock, this.gui3d, this.ambientOcclusion,
                new ModelTextureMapping(this.textureMappings, textures));
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
                TextureAtlasSprite sprite = spriteCache.computeIfAbsent(face.texture, t ->
                        this.textureMappings.getTextureOrMissing(t, bakedTextureGetter));

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
                generalQuads != null ? generalQuads : Collections.emptyList(), faceQuads, this.ambientOcclusion, this.gui3d,
                this.textureMappings.getTextureOrMissing("#particle", bakedTextureGetter),
                ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
    }

    @Nonnull
    private static BakedQuad makeBakedQuad(BlockPart part, BlockPartFace face, TextureAtlasSprite sprite,
                                           EnumFacing facing, TRSRTransformation transformation, boolean uvLock) {
        return ModelFactory.getBakery().makeBakedQuad(part.positionFrom, part.positionTo, face, sprite, facing,
                transformation, part.partRotation == null ? DEFAULT_ROTATION : part.partRotation, uvLock, part.shade);
    }
}
