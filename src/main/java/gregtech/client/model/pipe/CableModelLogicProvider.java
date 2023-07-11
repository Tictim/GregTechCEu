package gregtech.client.model.pipe;

import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.component.ComponentTexture;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelTextureMapping;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;

public class CableModelLogicProvider extends PipeModelLogicProvider {

    public static final String TEXTURE_INSULATION_ATLAS = "#insulation_atlas";
    public static final String TEXTURE_INSULATION_ATLAS_JOINTED = "#insulation_atlas_jointed";
    public static final String TEXTURE_INSULATION_SIDE = "#insulation_side";
    public static final String TEXTURE_OPEN_OVERLAY = "#open_overlay";
    public static final String TEXTURE_INSULATION_EXTRUSION = "#insulation_extrusion";

    public static final PipeModelTexture INSULATED_TEXTURES = new PipeModelTexture(
            new PipeSideAtlasTexture(DEFAULT_TEXTURES.atlas, TEXTURE_INSULATION_ATLAS, TINT_OVERLAY),
            new PipeSideAtlasTexture(DEFAULT_TEXTURES.atlas, TEXTURE_INSULATION_ATLAS_JOINTED, TINT_OVERLAY),
            new ComponentTexture(TEXTURE_INSULATION_SIDE, TINT_OVERLAY),
            new ComponentTexture(DEFAULT_TEXTURES.open, TEXTURE_OPEN_OVERLAY, TINT_OVERLAY),
            new ComponentTexture(TEXTURE_INSULATION_EXTRUSION, TINT_OVERLAY)
    );

    private final boolean insulated;

    public CableModelLogicProvider(float thickness, boolean insulated) {
        super(thickness);
        this.insulated = insulated;
    }

    @Nonnull
    @Override
    public ModelTextureMapping getDefaultTextureMappings() {
        Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<>();
        map.put(TEXTURE_ATLAS_JOINTED, TEXTURE_ATLAS);
        map.put(TEXTURE_EXTRUSION, TEXTURE_SIDE);
        map.put(TEXTURE_INSULATION_ATLAS_JOINTED, TEXTURE_INSULATION_ATLAS);
        map.put(TEXTURE_INSULATION_EXTRUSION, TEXTURE_INSULATION_SIDE);
        return new ModelTextureMapping(map);
    }

    @Nonnull
    @Override
    public IComponentLogic buildLogic(@Nonnull ComponentModel.Register componentRegister,
                                      @Nonnull ModelTextureMapping textureMapping) {
        return new CableModelLogic(
                defaultBaseModels(componentRegister, textureMapping),
                defaultEndModels(componentRegister, textureMapping, false),
                defaultEndModels(componentRegister, textureMapping, true),
                defaultExtrusionModels(componentRegister, textureMapping, false),
                defaultExtrusionModels(componentRegister, textureMapping, true));
    }

    @Override
    protected PipeModelTexture getModelTextures() {
        return this.insulated ? INSULATED_TEXTURES : DEFAULT_TEXTURES;
    }
}
