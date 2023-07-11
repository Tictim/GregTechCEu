package gregtech.client.model.pipe;

import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.component.ComponentTexture;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelTextureMapping;

import javax.annotation.Nonnull;

public class OpticalPipeModelLogicProvider extends PipeModelLogicProvider {

    public static final String TEXTURE_OVERLAY_ATLAS = "#overlay_atlas";
    public static final String TEXTURE_OVERLAY_ACTIVE_ATLAS = "#overlay_active_atlas";
    public static final String TEXTURE_OVERLAY_ATLAS_JOINTED = "#overlay_atlas_jointed";
    public static final String TEXTURE_OVERLAY_ACTIVE_ATLAS_JOINTED = "#overlay_active_atlas_jointed";

    public static final String TEXTURE_SIDE_OVERLAY = "#side_overlay";
    public static final String TEXTURE_SIDE_OVERLAY_ACTIVE = "#side_overlay_active";

    public static final String TEXTURE_EXTRUSION_OVERLAY = "#extrusion_overlay";
    public static final String TEXTURE_EXTRUSION_OVERLAY_ACTIVE = "#extrusion_overlay_active";

    public static final PipeModelTexture OVERLAY_TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(TEXTURE_OVERLAY_ATLAS, TINT_OVERLAY),
            new PipeSideAtlasTexture(TEXTURE_OVERLAY_ATLAS_JOINTED, TINT_OVERLAY),
            new ComponentTexture(TEXTURE_SIDE_OVERLAY, TINT_OVERLAY),
            null,
            new ComponentTexture(TEXTURE_EXTRUSION_OVERLAY, TINT_OVERLAY)
    );
    public static final PipeModelTexture ACTIVE_OVERLAY_TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(TEXTURE_OVERLAY_ACTIVE_ATLAS, TINT_OVERLAY),
            new PipeSideAtlasTexture(TEXTURE_OVERLAY_ACTIVE_ATLAS_JOINTED, TINT_OVERLAY),
            new ComponentTexture(TEXTURE_SIDE_OVERLAY_ACTIVE, TINT_OVERLAY),
            null,
            new ComponentTexture(TEXTURE_EXTRUSION_OVERLAY_ACTIVE, TINT_OVERLAY)
    );

    public OpticalPipeModelLogicProvider(float thickness) {
        super(thickness);
    }

    @Nonnull
    @Override
    public ModelTextureMapping getDefaultTextureMappings() {
        return ModelTextureMapping.builder(super.getDefaultTextureMappings())
                .add(TEXTURE_OVERLAY_ATLAS_JOINTED, TEXTURE_OVERLAY_ATLAS)
                .add(TEXTURE_OVERLAY_ACTIVE_ATLAS_JOINTED, TEXTURE_OVERLAY_ACTIVE_ATLAS)
                .add(TEXTURE_EXTRUSION_OVERLAY, TEXTURE_SIDE_OVERLAY)
                .add(TEXTURE_EXTRUSION_OVERLAY_ACTIVE, TEXTURE_SIDE_OVERLAY_ACTIVE)
                .build();
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister,
                                      @Nonnull ModelTextureMapping textureMapping) {
        return new OpticalPipeModelLogic(
                defaultBaseModels(componentRegister, textureMapping),
                defaultEndModels(componentRegister, textureMapping, true),
                defaultEndModels(componentRegister, textureMapping, false),
                defaultExtrusionModels(componentRegister, textureMapping, true),
                defaultExtrusionModels(componentRegister, textureMapping, false),
                registerBaseModels(componentRegister, textureMapping, OVERLAY_TEXTURES),
                registerBaseModels(componentRegister, textureMapping, ACTIVE_OVERLAY_TEXTURES),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, OVERLAY_TEXTURES, false)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, ACTIVE_OVERLAY_TEXTURES, false)));
    }
}
