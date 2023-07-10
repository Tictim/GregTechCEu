package gregtech.client.model.pipe;

import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.component.ComponentTexture;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelTextureMapping;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ItemPipeModelLogicProvider extends PipeModelLogicProvider {

    protected static final String TEXTURE_RESTRICTED_OVERLAY = "#restricted_overlay";

    protected static final ComponentTexture[] RESTRICTED_ATLAS_TEXTURES = new ComponentTexture[16];
    protected static final ComponentTexture[] JOINTED_RESTRICTED_ATLAS_TEXTURES = new ComponentTexture[16];

    protected static final ComponentTexture RESTRICTED_SIDE_TEXTURE = new ComponentTexture(SIDE_TEXTURE, TEXTURE_SIDE, TINT_PIPE);
    protected static final ComponentTexture RESTRICTED_EXTRUSION_TEXTURE = new ComponentTexture(EXTRUSION_TEXTURE, TEXTURE_OPEN, TINT_PIPE);

    static {
        for (int i = 0; i < 16; i++) {
            RESTRICTED_ATLAS_TEXTURES[i] = new ComponentTexture(ATLAS_TEXTURES[i], TEXTURE_RESTRICTED_OVERLAY, TINT_OVERLAY);
            JOINTED_RESTRICTED_ATLAS_TEXTURES[i] = new ComponentTexture(JOINTED_ATLAS_TEXTURES[i], TEXTURE_RESTRICTED_OVERLAY, TINT_OVERLAY);
        }
    }

    private final boolean restrictive;

    public ItemPipeModelLogicProvider(float thickness, boolean restrictive) {
        super(thickness);
        this.restrictive = restrictive;
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping) {
        return new ItemPipeModelLogic(
                registerBaseModels(componentRegister, textureMapping),
                componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, false)),
                componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, true)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, false)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, true)));
    }

    @Override
    protected ComponentTexture sideTexture() {
        return this.restrictive ? RESTRICTED_SIDE_TEXTURE : super.sideTexture();
    }

    @Override
    protected ComponentTexture extrusionTexture() {
        return this.restrictive ? RESTRICTED_EXTRUSION_TEXTURE : super.extrusionTexture();
    }

    @Override
    protected ComponentTexture[] sideAtlasTextures(boolean jointed) {
        return this.restrictive ?
                jointed ? JOINTED_RESTRICTED_ATLAS_TEXTURES : RESTRICTED_ATLAS_TEXTURES :
                super.sideAtlasTextures(jointed);
    }
}
