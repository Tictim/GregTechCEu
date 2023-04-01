package gregtech.integration.ctm;

import gregtech.api.util.GTLog;
import gregtech.common.blocks.BlockFrame;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap.BasicEntry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import team.chisel.ctm.Configurations;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;
import team.chisel.ctm.api.util.TextureInfo;
import team.chisel.ctm.client.texture.render.AbstractTexture;
import team.chisel.ctm.client.util.BakedQuadRetextured;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.Quad;
import team.chisel.ctm.client.util.Quad.UVs;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@TextureType("frame_gt")
public class FrameTextureType implements ITextureType {

    private static final CTMLogic.StateComparisonCallback STATE_COMPARATOR = (instance, from, to, dir) -> to.getBlock() instanceof BlockFrame;
    private static final DecimalFormat FORMAT = new DecimalFormat("#.####");

    @SuppressWarnings("unchecked")
    @Override
    public ICTMTexture<? extends FrameTextureType> makeTexture(TextureInfo info) {
        return new Texture<>(this, info);
    }

    @Override
    public ITextureContext getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
        return new TextureContext(world, pos);
    }

    @Override
    public int getQuadsPerSide() {
        return Configurations.disableCTM ? 1 : 4;
    }

    @Override
    public int requiredTextures() {
        return 2;
    }

    @Override
    public ITextureContext getContextFromData(long data) {
        throw new UnsupportedOperationException();
    }

    private static final class Texture<T extends FrameTextureType> extends AbstractTexture<T> {

        Texture(T type, TextureInfo info) {
            super(type, info);
        }

        @Override
        public List<BakedQuad> transformQuad(BakedQuad bq, @Nullable ITextureContext context, int quadGoal) {
            if (context == null || Configurations.disableCTM) {
                return Collections.singletonList(new BakedQuadRetextured(bq, sprites[0]));
            }

            Quad quad = makeQuad(bq, context).derotate();
            Quad[] quads = quad.subdivide(4);
            // for (int i = 0; i < quads.length; i++) {
            //     Quad q = quads[i];
            //     if (q == null || q == quad) continue;
            //     quads[i] = q.derotate();
            // }

            adjustUV(quad, quads);

            if (isFucked(quad, quads)) {
                GTLog.logger.warn("Your shit is fucked up");
                GTLog.logger.warn("Original Quad: {}", format(quad));
                for (int i = 0; i < quads.length; i++) {
                    GTLog.logger.warn("Quad Quad #{}: {}", i + 1, format(quads[i]));
                }
            }

            int[] ctm = ((TextureContext) context)
                    .getCTM(bq.getTintIndex() == 2 ? bq.getFace().getOpposite() : bq.getFace())
                    .getSubmapIndices();

            for (int i = 0; i < quads.length; i++) {
                Quad q = quads[i];
                if (q != null) {
                    int quadrant = q.getUvs().normalize().getQuadrant();
                    quads[i] = q.grow().transformUVs(sprites[ctm[quadrant] > 15 ? 0 : 1], CTMLogic.uvs[ctm[quadrant]].normalize());
                }
            }
            return Arrays.stream(quads)
                    .filter(Objects::nonNull)
                    .map(Quad::rebake)
                    .collect(Collectors.toList());
        }

        private static final Comparator<Vector3f> POS_SORTER = (o1, o2) -> {
            int i = Float.compare(o1.x, o2.x);
            if (i != 0) return i;
            i = Float.compare(o1.y, o2.y);
            return i != 0 ? i : Float.compare(o1.z, o2.z);
        };

        private static final Comparator<Vector2f> UV_SORTER = (o1, o2) -> {
            int i = Float.compare(o1.x, o2.x);
            return i != 0 ? i : Float.compare(o1.y, o2.y);
        };

        private static final Comparator<Map.Entry<Vector3f, Integer>> POS_ENTRY_SORTER = Map.Entry.comparingByKey(POS_SORTER);
        private static final Comparator<Map.Entry<Vector2f, Integer>> UV_ENTRY_SORTER = Map.Entry.comparingByKey(UV_SORTER);

        @SuppressWarnings("unchecked")
        private static void adjustUV(Quad originalQuad, Quad[] subdividedQuads) {
            Vector2f[] uv1 = originalQuad.getUvs().vectorize();
            if (uv1.length != 4) return;
            Object2IntMap.Entry<Vector3f>[] pos1ToIndex = null;

            for (Quad q2 : subdividedQuads) {
                if (q2 == null || q2 == originalQuad) continue;

                Vector2f[] uv2 = q2.getUvs().vectorize();
                if (uv2.length != 4) continue;

                if (pos1ToIndex == null) {
                    Object2IntMap.Entry<Vector2f>[] uv1ToIndex = new Object2IntMap.Entry[4];
                    for (int i = 0; i < 4; i++) {
                        uv1ToIndex[i] = new BasicEntry<>(uv1[i], i);
                    }
                    Arrays.sort(uv1ToIndex, UV_ENTRY_SORTER);

                    pos1ToIndex = new Object2IntMap.Entry[]{
                            new BasicEntry<>(originalQuad.getVert(0), uv1ToIndex[0].getIntValue()),
                            new BasicEntry<>(originalQuad.getVert(1), uv1ToIndex[1].getIntValue()),
                            new BasicEntry<>(originalQuad.getVert(2), uv1ToIndex[2].getIntValue()),
                            new BasicEntry<>(originalQuad.getVert(3), uv1ToIndex[3].getIntValue())
                    };
                    Arrays.sort(pos1ToIndex, POS_ENTRY_SORTER);
                }

                Object2IntMap.Entry<Vector2f>[] uv2ToIndex = new Object2IntMap.Entry[4];
                for (int i = 0; i < 4; i++) {
                    uv2ToIndex[i] = new BasicEntry<>(uv2[i], i);
                }
                Arrays.sort(uv2ToIndex, UV_ENTRY_SORTER);

                Object2IntMap.Entry<Vector3f>[] pos2ToIndex = new Object2IntMap.Entry[]{
                        new BasicEntry<>(q2.getVert(0), uv2ToIndex[0].getIntValue()),
                        new BasicEntry<>(q2.getVert(1), uv2ToIndex[1].getIntValue()),
                        new BasicEntry<>(q2.getVert(2), uv2ToIndex[2].getIntValue()),
                        new BasicEntry<>(q2.getVert(3), uv2ToIndex[3].getIntValue())
                };
                Arrays.sort(pos2ToIndex, POS_ENTRY_SORTER);

                Vector2f[] uv2Copy = uv2.clone();
                for (int i = 0; i < pos2ToIndex.length; i++) {
                    uv2[pos1ToIndex[i].getIntValue()] = uv2Copy[pos2ToIndex[i].getIntValue()];
                }
            }
        }

        private static boolean isFucked(Quad quad, Quad[] subdividedQuads) {
            int uvOrder1 = getUVOrder(quad);
            for (Quad subdividedQuad : subdividedQuads) {
                if (subdividedQuad != null && getUVOrder(subdividedQuad) != uvOrder1) {
                    return true;
                }
            }
            return false;
        }

        private static int getUVOrder(Quad quad) {
            int o = 0;
            Vector2f[] uvs = quad.getUvs().vectorize();
            for (int i = 0; i < uvs.length; i++) {
                int prev = (i + uvs.length - 1) % uvs.length;
                if (uvs[i].x > uvs[prev].x) o |= 1 << i * 2;
                if (uvs[i].y > uvs[prev].y) o |= 1 << i * 2 + 1;
            }
            return o;
        }

        private static String format(@Nullable Quad quad) {
            if (quad == null) return null;
            StringBuilder stb = new StringBuilder();
            UVs uvs = quad.getUvs().normalize();
            stb.append("{minMax: [")
                    .append(FORMAT.format(uvs.getMinU())).append(", ").append(FORMAT.format(uvs.getMinV()))
                    .append("] ~ [")
                    .append(FORMAT.format(uvs.getMaxU())).append(", ").append(FORMAT.format(uvs.getMaxV()))
                    .append("], data: [");

            Vector2f[] v = uvs.vectorize();
            for (int i = 0; i < v.length; i++) {
                if (i != 0) stb.append(", ");
                stb.append('[').append(FORMAT.format(v[i].x)).append(", ").append(FORMAT.format(v[i].y)).append(']');
            }

            return stb.append("], uvOrder: ").append(getUVOrder(quad)).append("}").toString();
        }
    }

    private static final class TextureContext implements ITextureContext {

        private final EnumMap<EnumFacing, CTMLogic> ctmData = new EnumMap<>(EnumFacing.class);
        private final long data;

        TextureContext(IBlockAccess world, BlockPos pos) {
            long data = 0;

            for (EnumFacing face : EnumFacing.VALUES) {
                CTMLogic ctm = new CTMLogic().stateComparator(STATE_COMPARATOR);
                ctm.createSubmapIndices(world, pos, face);
                ctmData.put(face, ctm);
                data |= ctm.serialized() << (face.ordinal() * 10);
            }
            this.data = data;
        }

        public CTMLogic getCTM(EnumFacing face) {
            return ctmData.get(face);
        }

        @Override
        public long getCompressedData() {
            return data;
        }
    }
}
