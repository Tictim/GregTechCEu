package gregtech.client.model.component;

import com.google.common.collect.ImmutableList;
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
import java.util.*;
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
        Vector3f[] verts = new Vector3f[CubeVertex.values().length];
        VertexFormat emissiveVertexFormat = null;

        for (ComponentFace face : component.getFaces()) {
            CubeVertex[] faceVertices = FACE_VERTICES[face.side.getIndex()];

            // TODO how the fuck do i do uv lock help

            Vector3f normalVector;
            if (format.hasNormal()) {
                normalVector = component.getShape().getFaceDirection(face.side, false);
                MatrixUtils.transform(mat, normalVector, normalVector);

                if (normalVector.lengthSquared() != 0) {
                    normalVector.normalise();
                }
            } else {
                normalVector = null;
            }
            for (ComponentTexture texture : face.texture) {
                TextureAtlasSprite sprite = model.getTextureMappings().getTextureOrMissing(texture.textureName(), bakedTextureGetter);
                Vector2f[] uvs = texture.getUVs(component.getShape(), face.side);

                VertexFormat fmt;
                if (texture.isBloom()) {
                    if (emissiveVertexFormat == null) {
                        emissiveVertexFormat = getEmissiveFormat(format);
                    }
                    fmt = emissiveVertexFormat;
                } else {
                    fmt = format;
                }

                UnpackedBakedQuad.Builder quad = new UnpackedBakedQuad.Builder(fmt);

                for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                    CubeVertex faceVertex = faceVertices[vertexIndex];
                    if (verts[faceVertex.ordinal()] == null) {
                        verts[faceVertex.ordinal()] = component.getShape().getVertexAt(faceVertex);
                    }

                    for (int e = 0; e < fmt.getElementCount(); e++) {
                        VertexFormatElement element = fmt.getElement(e);
                        switch (element.getUsage()) {
                            case POSITION -> {
                                Vector3f vert = verts[faceVertex.ordinal()];
                                quad.put(e, vert.x, vert.y, vert.z);
                            }
                            case NORMAL -> {
                                Objects.requireNonNull(normalVector);
                                quad.put(e, normalVector.x, normalVector.y, normalVector.z);
                            }
                            case COLOR -> {
                                float shade = component.getShape().shade() && !texture.isBloom() ?
                                        switch (face.side) {
                                            case DOWN -> 0.5f;
                                            case UP -> 1.0f;
                                            case NORTH, SOUTH -> 0.8f;
                                            case WEST, EAST -> 0.6f;
                                        } : 1;
                                quad.put(e, shade, shade, shade, 1); // rgba
                            }
                            case UV -> {
                                if (element.getType() == VertexFormatElement.EnumType.FLOAT) { // UV
                                    quad.put(e, sprite.getInterpolatedU(uvs[vertexIndex].x), sprite.getInterpolatedV(uvs[vertexIndex].y));
                                } else { // lightmap coords
                                    if (texture.isBloom()) {
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

                quad.setQuadTint(texture.tintIndex());
                quad.setQuadOrientation(getFacingFromVertexData(faceVertices, verts));
                quad.setTexture(sprite);
                quad.setApplyDiffuseLighting(model.ambientOcclusion() && !texture.isBloom());

                quads.add(new ModelStates.Quad(
                        quad.build(),
                        face.cullFace,
                        texture.getRenderLayer()));
            }
        }
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

    @Nonnull
    public String prettyPrintComponents() {
        StringBuilder b = new StringBuilder();
        b.append(Arrays.stream(this.quads).mapToInt(l -> l.size()).sum()).append(" quads");
        for (int i = 0; i < quads.length; i++) {
            b.append("\n:::: Part #").append(i).append(": ").append(quads[i].size()).append(" elements");
            for (int j = 0; j < quads[i].size(); j++) {
                b.append("\n[[").append(j).append("]]");
                b.append("\n").append(quads[i].get(j));
            }
        }
        return b.toString();
    }
}
