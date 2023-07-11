package gregtech.client.model.pipe;

import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.component.ComponentTexture;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelTextureMapping;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;

public class OpticalPipeModelLogicProvider extends PipeModelLogicProvider {

    protected static final String TEXTURE_OVERLAY_ATLAS = "#overlay_atlas";
    protected static final String TEXTURE_OVERLAY_ACTIVE_ATLAS = "#overlay_active_atlas";
    protected static final String TEXTURE_OVERLAY_ATLAS_JOINTED = "#overlay_atlas_jointed";
    protected static final String TEXTURE_OVERLAY_ACTIVE_ATLAS_JOINTED = "#overlay_active_atlas_jointed";

    protected static final String TEXTURE_SIDE_OVERLAY = "#side_overlay";
    protected static final String TEXTURE_SIDE_OVERLAY_ACTIVE = "#side_overlay";

    protected static final String TEXTURE_EXTRUSION_OVERLAY = "#extrusion_overlay";
    protected static final String TEXTURE_EXTRUSION_OVERLAY_ACTIVE = "#extrusion_overlay_active";

    public static final PipeModelTexture OVERLAY_TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(TEXTURE_OVERLAY_ATLAS, -1),
            new PipeSideAtlasTexture(TEXTURE_OVERLAY_ATLAS_JOINTED, -1),
            new ComponentTexture(TEXTURE_SIDE_OVERLAY, -1),
            null,
            new ComponentTexture(TEXTURE_EXTRUSION_OVERLAY, -1)
    );
    public static final PipeModelTexture ACTIVE_OVERLAY_TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(TEXTURE_OVERLAY_ACTIVE_ATLAS, -1),
            new PipeSideAtlasTexture(TEXTURE_OVERLAY_ACTIVE_ATLAS_JOINTED, -1),
            new ComponentTexture(TEXTURE_SIDE_OVERLAY_ACTIVE, -1),
            null,
            new ComponentTexture(TEXTURE_EXTRUSION_OVERLAY_ACTIVE, -1)
    );

    public OpticalPipeModelLogicProvider(float thickness) {
        super(thickness);
    }

    @Nonnull
    @Override
    public ModelTextureMapping getDefaultTextureMappings() {
        Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<>();
        map.put(TEXTURE_ATLAS_JOINTED, TEXTURE_ATLAS);
        map.put(TEXTURE_EXTRUSION, TEXTURE_SIDE);
        map.put(TEXTURE_EXTRUSION_OVERLAY, TEXTURE_SIDE_OVERLAY);
        map.put(TEXTURE_EXTRUSION_OVERLAY_ACTIVE, TEXTURE_SIDE_OVERLAY_ACTIVE);
        return new ModelTextureMapping(map);
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister,
                                      @Nonnull ModelTextureMapping textureMapping) {
        return new OpticalPipeModelLogic(
                defaultBaseModels(componentRegister, textureMapping),
                defaultEndModels(componentRegister, textureMapping, false),
                defaultEndModels(componentRegister, textureMapping, true),
                defaultExtrusionModels(componentRegister, textureMapping, false),
                defaultExtrusionModels(componentRegister, textureMapping, true),
                registerBaseModels(componentRegister, textureMapping, OVERLAY_TEXTURES),
                registerBaseModels(componentRegister, textureMapping, ACTIVE_OVERLAY_TEXTURES),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, OVERLAY_TEXTURES, false)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, ACTIVE_OVERLAY_TEXTURES, false)));
    }
}
