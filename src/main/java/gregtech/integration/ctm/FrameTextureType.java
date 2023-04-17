package gregtech.integration.ctm;

import gregtech.common.blocks.BlockFrame;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
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

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@TextureType("frame_gt")
public class FrameTextureType implements ITextureType {

    private static final CTMLogic.StateComparisonCallback STATE_COMPARATOR = (instance, from, to, dir) -> to.getBlock() instanceof BlockFrame;

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

            Quad[] quads = makeQuad(bq, context).derotate().subdivide(4);

            int[] ctm = ((TextureContext) context)
                    .getCTM(bq.getFace(), bq.getTintIndex() == 2)
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
    }

    private static final class TextureContext implements ITextureContext {

        private final EnumMap<EnumFacing, CTMLogic> ctmData = new EnumMap<>(EnumFacing.class);
        private final EnumMap<EnumFacing, CTMLogic> innerCtmData = new EnumMap<>(EnumFacing.class);
        private final long data;

        TextureContext(IBlockAccess world, BlockPos pos) {
            long data = 0;

            for (EnumFacing face : EnumFacing.VALUES) {
                CTMLogic ctm = new CTMLogic().stateComparator(STATE_COMPARATOR);
                ctm.disableObscuredFaceCheck = Optional.of(false);
                ctm.createSubmapIndices(world, pos, face);
                ctmData.put(face, ctm);
                data |= ctm.serialized() << (face.ordinal() * 10);

                ctm = new CTMLogic().stateComparator(STATE_COMPARATOR);
                ctm.disableObscuredFaceCheck = Optional.of(true);
                ctm.createSubmapIndices(world, pos, face);
                innerCtmData.put(face, ctm);
            }
            this.data = data;
        }

        public CTMLogic getCTM(EnumFacing face, boolean inner) {
            if (inner) return innerCtmData.get(face);
            return ctmData.get(face);
        }

        @Override
        public long getCompressedData() {
            return data;
        }
    }
}
