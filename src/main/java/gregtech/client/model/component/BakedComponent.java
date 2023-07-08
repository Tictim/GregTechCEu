package gregtech.client.model.component;

import com.google.common.collect.ImmutableList;
import gregtech.client.utils.BloomEffectUtil;
import gregtech.client.utils.CubeVertex;
import gregtech.client.utils.MatrixUtils;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static gregtech.client.utils.CubeVertex.*;

@ParametersAreNonnullByDefault
public final class BakedComponent {

    private static final CubeVertex[][] FACE_VERTICES = {
            {DSW, DNW, DNE, DSE},
            {UNW, USW, USE, UNE},
            {UNE, DNE, DNW, UNW},
            {USW, DSW, DSE, USE},
            {UNW, DNW, DSW, USW},
            {USE, DSE, DNE, UNE}
    };

    private static final Map<VertexFormat, VertexFormat> emissiveFormatCache = new Object2ObjectOpenHashMap<>();

    @Nonnull
    private static VertexFormat getEmissiveFormat(VertexFormat format) {
        if (FMLClientHandler.instance().hasOptifine()) {
            return format;
        }
        return emissiveFormatCache.computeIfAbsent(format, vertexFormat -> {
            if (format.getElements().contains(DefaultVertexFormats.TEX_2S)) { // lightmap coords
                return format;
            }
            VertexFormat f2 = new VertexFormat(format);
            f2.addElement(DefaultVertexFormats.TEX_2S);
            return f2;
        });
    }

    public final int size;

    // [part ID] [list of quads]
    private final ImmutableList<ModelStates.Quad>[] quads;

    @SuppressWarnings("unchecked")
    public BakedComponent(ComponentModel model,
                          IModelState state,
                          VertexFormat format,
                          Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        this.size = model.getParts().size();
        this.quads = new ImmutableList[this.size];

        TRSRTransformation transformation = state.apply(Optional.empty())
                .orElse(TRSRTransformation.identity()); // cringe
        javax.vecmath.Matrix4f mat = transformation.getMatrix();

        for (int partID = 0; partID < this.size; partID++) {
            ImmutableList<Component> components = model.getParts().get(partID);
            if (components.isEmpty()) {
                this.quads[partID] = ImmutableList.of();
            } else {
                ImmutableList.Builder<ModelStates.Quad> builder = new ImmutableList.Builder<>();
                for (Component c : components) {
                    bake(model, format, mat, bakedTextureGetter, c, builder);
                }
                this.quads[partID] = builder.build();
            }
        }
    }

    private static void bake(ComponentModel model,
                             VertexFormat format,
                             javax.vecmath.Matrix4f mat,
                             Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                             Component component,
                             ImmutableList.Builder<ModelStates.Quad> quads) {
        Vector3f[] verts = new Vector3f[values().length];
        VertexFormat emissiveVertexFormat = null;

        for (ComponentFace face : component.getFaces()) {
            CubeVertex[] faceVertices = FACE_VERTICES[face.side.getIndex()];

            // TODO how the fuck do i do uv lock help

            TextureAtlasSprite sprite = model.getTextureMappings().getTextureOrMissing(face.texture.texture(), bakedTextureGetter);

            Vector2f[] uvs = face.texture.getUVs(component.getShape(), face.side);

            Vector3f normalVector = null;
            if (format.hasNormal()) {
                normalVector = component.getShape().getFaceDirection(face.side, false);
                MatrixUtils.transform(mat, normalVector, normalVector);

                if (normalVector.lengthSquared() != 0) {
                    normalVector.normalise();
                }
            }

            VertexFormat fmt;
            if (face.texture.isBloom()) {
                if (emissiveVertexFormat == null) {
                    emissiveVertexFormat = getEmissiveFormat(format);
                }
                fmt = emissiveVertexFormat;
            } else {
                fmt = format;
            }

            UnpackedBakedQuad.Builder quad = new UnpackedBakedQuad.Builder(fmt);

            for (int i = 0; i < 4; i++) {
                CubeVertex faceVertex = faceVertices[i];
                Vector3f vert = verts[faceVertex.ordinal()];

                if (vert == null) {
                    verts[faceVertex.ordinal()] = vert = component.getShape().getVertexAt(faceVertex);
                }

                for (int v = 0; v < 4; v++) {
                    for (int e = 0; e < fmt.getElementCount(); e++) {
                        VertexFormatElement element = fmt.getElement(e);
                        switch (element.getUsage()) {
                            case POSITION -> quad.put(e, vert.x, vert.y, vert.z);
                            case NORMAL -> {
                                if (normalVector == null) {
                                    throw new IllegalStateException();
                                }
                                quad.put(e, normalVector.x, normalVector.y, normalVector.z);
                            }
                            case COLOR -> {
                                int shade = component.getShape().shade() && !face.texture.isBloom() ?
                                        getFaceShade(face.side) : -1;
                                quad.put(e, shade, shade, shade, 1); // rgba
                            }
                            case UV -> {
                                if (element.getType() == VertexFormatElement.EnumType.FLOAT) { // UV
                                    quad.put(e, sprite.getInterpolatedU(uvs[i].x), sprite.getInterpolatedV(uvs[i].y));
                                } else { // lightmap coords
                                    if (face.texture.isBloom()) {
                                        quad.put(e, 480f / 0xFFFF, 480f / 0xFFFF);
                                    } else {
                                        quad.put(e);
                                    }
                                }
                            }
                            // MATRIX, BLEND_WEIGHT, GENERIC, PADDING
                            // generic "empty data"
                            default -> quad.put(e);
                        }
                    }
                }
            }

            quad.setQuadTint(face.texture.tintIndex());
            quad.setQuadOrientation(getFacingFromVertexData(faceVertices, verts));
            quad.setTexture(sprite);
            quad.setApplyDiffuseLighting(model.ambientOcclusion() && !face.texture.isBloom());

            quads.add(new ModelStates.Quad(
                    quad.build(),
                    face.cullFace,
                    face.texture.isBloom() ? BloomEffectUtil.BLOOM : null));
        }
    }

    private static int getFaceShade(EnumFacing facing) {
        int shade = (int) (switch (facing) {
            case DOWN -> 0.5f;
            case UP -> 1.0f;
            case NORTH, SOUTH -> 0.8f;
            case WEST, EAST -> 0.6f;
        } * 255f);
        return MathHelper.clamp(shade, 0, 255);
    }

    /**
     * Computes the closest facing to the quad's normal vector.
     *
     * @see FaceBakery#getFacingFromVertexData(int[])
     */
    @Nonnull
    private static EnumFacing getFacingFromVertexData(CubeVertex[] faceVertices, Vector3f[] verts) {
        Vector3f a = verts[faceVertices[0].ordinal()];
        Vector3f b = verts[faceVertices[1].ordinal()];
        Vector3f c = verts[faceVertices[2].ordinal()];
        Vector3f cross = Vector3f.cross(Vector3f.sub(c, b, null), Vector3f.sub(a, b, null), null);
        if (cross.lengthSquared() != 0) {
            cross.normalise();
        }

        EnumFacing facing = null;
        float min = 0.0F;

        for (EnumFacing f : EnumFacing.values()) {
            Vec3i dir = f.getDirectionVec();
            float dot = Vector3f.dot(cross, new Vector3f(dir.getX(), dir.getY(), dir.getZ()));

            if (dot >= 0.0F && dot > min) {
                min = dot;
                facing = f;
            }
        }

        return facing == null ? EnumFacing.UP : facing;
    }

    public void addQuads(Collection<BakedQuad> collection,
                         IntCollection partIDs,
                         @Nullable EnumFacing facing,
                         @Nullable BlockRenderLayer currentLayer,
                         boolean bloomActive) {
        IntIterator it = partIDs.iterator();
        while (it.hasNext()) {
            int i = it.nextInt();
            if (i < 0 || this.size <= i) {
                if (this.size == 0) {
                    throw new IndexOutOfBoundsException("No parts registered");
                } else {
                    throw new IndexOutOfBoundsException("Invalid part ID " + i + "; expected: 0 ~ " + (this.size - 1) + " (inclusive)");
                }
            }
            for (ModelStates.Quad quad : this.quads[i]) {
                if (facing == quad.cullFace && quad.canRenderOnLayer(currentLayer, bloomActive)) {
                    Collections.addAll(collection, quad.quad);
                }
            }
        }
    }
}
