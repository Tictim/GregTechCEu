package gregtech.client.model.pipe;

import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.component.ComponentTexture;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelTextureMapping;

import javax.annotation.Nonnull;

public class ItemPipeModelLogicProvider extends PipeModelLogicProvider {

    public static final String TEXTURE_RESTRICTED_OVERLAY = "#restricted_overlay";

    public static final PipeModelTexture RESTRICTED_TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(TEXTURE_RESTRICTED_OVERLAY,
                    i -> new ComponentTexture(DEFAULT_TEXTURES.expectAtlas().get(i), TEXTURE_RESTRICTED_OVERLAY, TINT_OVERLAY)),
            new PipeSideAtlasTexture(TEXTURE_RESTRICTED_OVERLAY,
                    i -> new ComponentTexture(DEFAULT_TEXTURES.expectJointedAtlas().get(i), TEXTURE_RESTRICTED_OVERLAY, TINT_OVERLAY)),
            new ComponentTexture(DEFAULT_TEXTURES.side, TEXTURE_RESTRICTED_OVERLAY, TINT_OVERLAY),
            DEFAULT_TEXTURES.in,
            new ComponentTexture(DEFAULT_TEXTURES.extrusion, TEXTURE_RESTRICTED_OVERLAY, TINT_OVERLAY)
    );

    private final boolean restrictive;

    public ItemPipeModelLogicProvider(float thickness, boolean restrictive) {
        super(thickness);
        this.restrictive = restrictive;
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister, @Nonnull ModelTextureMapping textureMapping) {
        return new ItemPipeModelLogic(
                defaultBaseModels(componentRegister, textureMapping),
                defaultEndModels(componentRegister, textureMapping, true),
                defaultEndModels(componentRegister, textureMapping, false),
                defaultExtrusionModels(componentRegister, textureMapping, true),
                defaultExtrusionModels(componentRegister, textureMapping, false));
    }

    @Override
    protected PipeModelTexture getModelTextures() {
        return this.restrictive ? RESTRICTED_TEXTURES : DEFAULT_TEXTURES;
    }
}
