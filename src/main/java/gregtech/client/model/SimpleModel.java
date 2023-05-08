package gregtech.client.model;

import com.google.common.collect.ImmutableMap;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimpleModel implements IModel {

    private static final BlockPartRotation DEFAULT_ROTATION = new BlockPartRotation(
            new Vector3f(),
            EnumFacing.Axis.Y,
            0,
            false);

    public static SimpleModelBuilder builder() {
        return new SimpleModelBuilder();
    }

    private final List<BlockPart> blockParts;
    private final boolean uvLock;
    private final boolean gui3d;
    private final boolean ambientOcclusion;

    SimpleModel(@Nonnull List<BlockPart> blockParts, boolean uvLock, boolean gui3d, boolean ambientOcclusion) {
        this.blockParts = blockParts;
        this.uvLock = uvLock;
        this.gui3d = gui3d;
        this.ambientOcclusion = ambientOcclusion;
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
            tex2.add(new ResourceLocation(s));
        }
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
        return new SimpleModel(this.blockParts.stream()
                .map(p -> {
                    if (!shouldReplaceTexture(p, textures)) {
                        return p;
                    }
                    EnumMap<EnumFacing, BlockPartFace> faces = new EnumMap<>(p.mapFaces);
                    faces.replaceAll((side, face) -> {
                        String newTexture = textures.get(face.texture);
                        return newTexture == null ? face :
                                new BlockPartFace(face.cullFace,
                                        face.tintIndex,
                                        newTexture,
                                        face.blockFaceUV);
                    });
                    return new BlockPart(
                            new Vector3f(p.positionFrom),
                            new Vector3f(p.positionTo),
                            faces,
                            p.partRotation == null ? null :
                                    new BlockPartRotation(p.partRotation.origin,
                                            p.partRotation.axis,
                                            p.partRotation.angle,
                                            p.partRotation.rescale),
                            p.shade);
                }).collect(Collectors.toList()),
                this.uvLock, this.gui3d, this.ambientOcclusion);
    }

    private static boolean shouldReplaceTexture(@Nonnull BlockPart part, @Nonnull Map<String, String> textures) {
        for (BlockPartFace face : part.mapFaces.values()) {
            if (textures.containsKey(face.texture)) return true;
        }
        return false;
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        TRSRTransformation transformation = state.apply(Optional.empty()).orElse(TRSRTransformation.identity()); // cringe

        List<BakedQuad> generalQuads = null;
        Map<EnumFacing, List<BakedQuad>> faceQuads = new EnumMap<>(EnumFacing.class);

        for (BlockPart part : this.blockParts) {
            if (part.partRotation == null) {
                part = new BlockPart(part.positionFrom, part.positionTo, part.mapFaces, DEFAULT_ROTATION, part.shade);
            }
            for (Map.Entry<EnumFacing, BlockPartFace> e : part.mapFaces.entrySet()) {
                BlockPartFace face = e.getValue();
                TextureAtlasSprite sprite = bakedTextureGetter.apply(new ResourceLocation(face.texture));

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
                this.gui3d, bakedTextureGetter.apply(TextureMap.LOCATION_MISSING_TEXTURE),
                ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
    }

    private static BakedQuad makeBakedQuad(BlockPart part, BlockPartFace face, TextureAtlasSprite sprite,
                                           EnumFacing facing, TRSRTransformation transformation, boolean uvLock) {
        return ModelFactory.getBakery().makeBakedQuad(part.positionFrom, part.positionTo, face, sprite, facing,
                transformation, part.partRotation, uvLock, part.shade);
    }
}
