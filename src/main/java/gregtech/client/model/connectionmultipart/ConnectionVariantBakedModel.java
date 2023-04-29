package gregtech.client.model.connectionmultipart;

import gregtech.client.model.connectionmultipart.condition.ConnectionVariantCondition;
import gregtech.common.blocks.extendedstate.ConnectionState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConnectionVariantBakedModel implements IBakedModel {

    private final IBakedModel baseModel;
    private final List<Pair<ConnectionVariantCondition, IBakedModel>> variants;

    public ConnectionVariantBakedModel(IBakedModel baseModel, List<Pair<ConnectionVariantCondition, IBakedModel>> variants) {
        this.baseModel = baseModel;
        this.variants = variants;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        List<BakedQuad> quads = this.baseModel.getQuads(state, side, rand);
        if (state instanceof ConnectionState) {
            ConnectionState connectionState = (ConnectionState) state;
            boolean firstMatch = true;
            for (Pair<ConnectionVariantCondition, IBakedModel> pair : this.variants) {
                if (!pair.getLeft().matches(connectionState)) {
                    continue;
                }
                if (firstMatch) {
                    firstMatch = false;
                    quads = new ArrayList<>(quads);
                }
                quads.addAll(pair.getRight().getQuads(state, side, rand));
            }
        }
        return quads;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.baseModel.isAmbientOcclusion();
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return this.baseModel.isAmbientOcclusion(state);
    }

    @Override
    public boolean isGui3d() {
        return this.baseModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.baseModel.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.baseModel.getItemCameraTransforms();
    }
}
