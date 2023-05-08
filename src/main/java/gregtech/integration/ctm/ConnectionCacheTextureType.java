package gregtech.integration.ctm;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.util.TextureInfo;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Extremely hacky solution to provide connection data to cached CTM models.
 */
public final class ConnectionCacheTextureType implements ITextureType,
        ICTMTexture<ConnectionCacheTextureType>,
        ITextureContext {

    private final int index;
    private final long data;

    public ConnectionCacheTextureType(int index, long data) {
        this.index = index;
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ICTMTexture<? extends ConnectionCacheTextureType> makeTexture(TextureInfo info) {
        return this;
    }

    @Override
    public ITextureContext getBlockRenderContext(IBlockState state, IBlockAccess world, BlockPos pos, ICTMTexture<?> tex) {
        return this;
    }

    @Override
    public ITextureContext getContextFromData(long data) {
        return this;
    }

    @Override
    public List<BakedQuad> transformQuad(BakedQuad quad, @Nullable ITextureContext context, int quadGoal) {
        return Collections.singletonList(quad);
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return Collections.emptySet();
    }

    @Override
    public ConnectionCacheTextureType getType() {
        return this;
    }

    @Nullable
    @Override
    public TextureAtlasSprite getParticle() {
        return null;
    }

    @Nullable
    @Override
    public BlockRenderLayer getLayer() {
        return null;
    }

    @Override
    public long getCompressedData() {
        return this.data;
    }

    @Override
    public int hashCode() {
        return this.index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnectionCacheTextureType)) return false;
        return index == ((ConnectionCacheTextureType) o).index;
    }

    @Override
    public String toString() {
        return "ConnectionCacheTextureType{" +
                "index=" + index +
                ", data=" + data +
                '}';
    }
}
