package gregtech.client.model.pipe;

import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.component.ComponentTexture;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelTextureMapping;

import javax.annotation.Nonnull;

public class LaserPipeModelLogicProvider extends PipeModelLogicProvider {

    public static final String TEXTURE_OVERLAY_ATLAS = "#overlay_atlas";
    public static final String TEXTURE_EMISSIVE_ATLAS = "#emissive_atlas";

    public static final String TEXTURE_OVERLAY_ATLAS_JOINTED = "#overlay_atlas_jointed";
    public static final String TEXTURE_EMISSIVE_ATLAS_JOINTED = "#emissive_atlas_jointed";

    public static final String TEXTURE_SIDE_OVERLAY = "#side_overlay";
    public static final String TEXTURE_SIDE_EMISSIVE = "#side_emissive";

    public static final String TEXTURE_EXTRUSION_OVERLAY = "#extrusion_overlay";
    public static final String TEXTURE_EXTRUSION_EMISSIVE = "#extrusion_emissive";

    public static final PipeModelTexture TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(
                    new PipeSideAtlasTexture(
                            DEFAULT_TEXTURES.atlas,
                            TEXTURE_OVERLAY_ATLAS,
                            TINT_OVERLAY),
                    TEXTURE_EMISSIVE_ATLAS,
                    TINT_OVERLAY
            ),
            new PipeSideAtlasTexture(
                    new PipeSideAtlasTexture(
                            DEFAULT_TEXTURES.jointedAtlas,
                            TEXTURE_OVERLAY_ATLAS_JOINTED,
                            TINT_OVERLAY),
                    TEXTURE_EMISSIVE_ATLAS_JOINTED,
                    TINT_OVERLAY
            ),
            new ComponentTexture(
                    new ComponentTexture(
                            DEFAULT_TEXTURES.side,
                            TEXTURE_SIDE_OVERLAY,
                            TINT_OVERLAY),
                    TEXTURE_SIDE_EMISSIVE,
                    TINT_OVERLAY
            ).setBloom(true),
            DEFAULT_TEXTURES.in,
            new ComponentTexture(
                    new ComponentTexture(
                            DEFAULT_TEXTURES.side,
                            TEXTURE_EXTRUSION_OVERLAY,
                            TINT_OVERLAY),
                    TEXTURE_EXTRUSION_EMISSIVE,
                    TINT_OVERLAY
            ).setBloom(true)
    );

    public LaserPipeModelLogicProvider(float thickness) {
        super(thickness);
    }

    @Nonnull
    @Override
    public ModelTextureMapping getDefaultTextureMappings() {
        return ModelTextureMapping.builder(super.getDefaultTextureMappings())
                .add(TEXTURE_OVERLAY_ATLAS_JOINTED, TEXTURE_OVERLAY_ATLAS)
                .add(TEXTURE_EMISSIVE_ATLAS_JOINTED, TEXTURE_EMISSIVE_ATLAS)
                .add(TEXTURE_EXTRUSION_OVERLAY, TEXTURE_SIDE_OVERLAY)
                .add(TEXTURE_EXTRUSION_EMISSIVE, TEXTURE_SIDE_EMISSIVE)
                .build();
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister, @Nonnull ModelTextureMapping textureMapping) {
        return new LaserPipeModelLogic(
                defaultBaseModels(componentRegister, textureMapping),
                defaultEndModels(componentRegister, textureMapping, true),
                defaultEndModels(componentRegister, textureMapping, false),
                defaultExtrusionModels(componentRegister, textureMapping, true),
                defaultExtrusionModels(componentRegister, textureMapping, false));
    }

    @Override
    protected PipeModelTexture getModelTextures() {
        return TEXTURES;
    }
}
