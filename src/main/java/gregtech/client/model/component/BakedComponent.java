package gregtech.client.model.component;

import com.google.common.collect.ImmutableList;
import gregtech.client.utils.CubeVertex;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static gregtech.client.utils.CubeVertex.*;

public final class BakedComponent {

    private static final CubeVertex[][] FACE_VERTICES = {
            {DSW, DNW, DNE, DSE},
            {UNW, USW, USE, UNE},
            {UNE, DNE, DNW, UNW},
            {USW, DSW, DSE, USE},
            {UNW, DNW, DSW, USW},
            {USE, DSE, DNE, UNE}
    };

    public final int size;

    // [part ID] [list of quads]
    private final List<Quad>[] quads;

    @SuppressWarnings("unchecked")
    public BakedComponent(@Nonnull ComponentModel model,
                          @Nonnull IModelState state,
                          @Nonnull VertexFormat format,
                          @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        this.size = model.getParts().size();
        this.quads = new List[this.size];

        TRSRTransformation transformation = state.apply(Optional.empty())
                .orElse(TRSRTransformation.identity()); // cringe
        javax.vecmath.Matrix4f mat = transformation.getMatrix();

        for (int partID = 0; partID < this.size; partID++) {
            ImmutableList<Component> components = model.getParts().get(partID);
            if (components.isEmpty()) {
                this.quads[partID] = Collections.emptyList();
            } else {
                List<Quad> list = new ArrayList<>();
                for (Component c : components) {
                    bake(model, format, mat, bakedTextureGetter, c, list);
                }
                this.quads[partID] = list;
            }
        }
    }

    private static void bake(@Nonnull ComponentModel model,
                             @Nonnull VertexFormat format,
                             @Nonnull javax.vecmath.Matrix4f mat,
                             @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
                             @Nonnull Component component,
                             @Nonnull Collection<Quad> collection) {
        Vector3f[] verts = new Vector3f[values().length];

        for (Map.Entry<EnumFacing, ComponentFace> e : component.getFaces().entrySet()) {
            EnumFacing side = e.getKey();
            ComponentFace face = e.getValue();

            CubeVertex[] faceVertices = FACE_VERTICES[side.getIndex()];

            // TODO how the fuck do i do uv lock help

            // 4 vertices, 7 ints each

            // 0: X Pos (float)
            // 1: Y Pos (float)
            // 2: Z Pos (float)
            // 3: Color (int, ARGB I suppose?)
            // 4: U (float)
            // 5: V (float)
            // 6: Normal (int, ?XYZ)
            int[] data = new int[7 * 4];

            TextureAtlasSprite sprite = model.getTextureMappings().getTextureOrMissing(face.texture.texture(), bakedTextureGetter);

            Vector2f[] uvs = face.texture.getUVs(component.getShape(), side);

            Vector3f normalVector = component.getShape().getFaceDirection(side);
            javax.vecmath.Vector3f fuck = new javax.vecmath.Vector3f(normalVector.x, normalVector.y, normalVector.z);
            mat.transform(fuck);

            int normal = ((byte) Math.round(fuck.x * 127)) & 0xFF |
                    ((byte) Math.round(fuck.y * 127) & 0xFF) << 8 |
                    ((byte) Math.round(fuck.z * 127) & 0xFF) << 16;

            for (int i = 0; i < 4; i++) {
                CubeVertex v = faceVertices[i];
                Vector3f vert = verts[v.ordinal()];

                if (vert == null) {
                    verts[v.ordinal()] = vert = component.getShape().getVertexAt(v);
                }

                data[4 * i] = Float.floatToRawIntBits(vert.x);
                data[4 * i + 1] = Float.floatToRawIntBits(vert.y);
                data[4 * i + 2] = Float.floatToRawIntBits(vert.z);
                data[4 * i + 3] = component.getShape().shade() && !face.texture.isBloom() ? getFaceShadeColor(side) : -1;
                data[4 * i + 4] = Float.floatToRawIntBits(sprite.getInterpolatedU(uvs[i].x));
                data[4 * i + 5] = Float.floatToRawIntBits(sprite.getInterpolatedV(uvs[i].y));
                data[4 * i + 6] = normal;
            }

            collection.add(new Quad(
                    new BakedQuad(data,
                            face.texture.tintIndex(),
                            FaceBakery.getFacingFromVertexData(data),
                            sprite, model.ambientOcclusion() && !face.texture.isBloom(), format),
                    face.cullFace,
                    face.texture.isBloom()));
        }
    }

    private static int getFaceShadeColor(EnumFacing facing) {
        int brightness = MathHelper.clamp((int) (getFaceBrightness(facing) * 255.0F), 0, 255);
        return 0xff000000 | brightness << 16 | brightness << 8 | brightness;
    }

    private static float getFaceBrightness(EnumFacing facing) {
        return switch (facing) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    public void addQuads(@Nonnull Collection<BakedQuad> collection,
                         @Nonnull IntCollection partIDs,
                         @Nullable EnumFacing facing,
                         boolean includeBloomLayer,
                         boolean includeNonBloomLayer) {
        if (!includeBloomLayer && !includeNonBloomLayer) return;

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
            for (Quad quad : this.quads[i]) {
                if (facing == quad.cullFace && (quad.bloom ? includeBloomLayer : includeNonBloomLayer)) {
                    Collections.addAll(collection, quad.quad);
                }
            }
        }
    }

    private static final class Quad {

        private final BakedQuad quad;
        private final EnumFacing cullFace;
        private final boolean bloom;

        private Quad(BakedQuad quad, EnumFacing cullFace, boolean bloom) {
            this.quad = quad;
            this.cullFace = cullFace;
            this.bloom = bloom;
        }
    }
}
