package gregtech.client.model.pipe;

import gregtech.client.model.component.ComponentModel;
import gregtech.client.model.component.ComponentTexture;
import gregtech.client.model.component.IComponentLogic;
import gregtech.client.model.component.ModelTextureMapping;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CableModelLogicProvider extends PipeModelLogicProvider {

    protected static final String TEXTURE_INSULATION_ATLAS = "#insulation_atlas";
    protected static final String TEXTURE_INSULATION_ATLAS_JOINTED = "#insulation_atlas_jointed";
    protected static final String TEXTURE_OPEN_OVERLAY = "#open_overlay";
    protected static final String TEXTURE_INSULATION_EXTRUSION = "#insulation_extrusion";
    protected static final String TEXTURE_INSULATION_SIDE = "#insulation_side";

    protected static final ComponentTexture[] INSULATION_ATLAS_TEXTURES = generateAtlasTextures(TEXTURE_INSULATION_ATLAS, TINT_OVERLAY);
    protected static final ComponentTexture[] JOINTED_INSULATION_ATLAS_TEXTURES = generateAtlasTextures(TEXTURE_INSULATION_ATLAS_JOINTED, TINT_OVERLAY);

    protected static final ComponentTexture INSULATION_SIDE_TEXTURE = new ComponentTexture(TEXTURE_INSULATION_SIDE, TINT_OVERLAY);
    protected static final ComponentTexture INSULATION_EXTRUSION_TEXTURE = new ComponentTexture(TEXTURE_INSULATION_EXTRUSION, TINT_OVERLAY);

    protected static final ComponentTexture INSULATION_OPEN_TEXTURE = new ComponentTexture(OPEN_TEXTURE, TEXTURE_OPEN_OVERLAY, TINT_OVERLAY);

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
    public IComponentLogic buildLogic(ComponentModel.Register componentRegister, ModelTextureMapping textureMapping) {
        return new CableModelLogic(
                registerBaseModels(componentRegister, textureMapping),
                componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, false)),
                componentRegister.addForEachFacing((f, b) -> registerEndModels(f, b, textureMapping, true)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, false)),
                componentRegister.addForEachFacing((f, b) -> registerExtrusionModels(f, b, textureMapping, true)));
    }

    @Override
    protected ComponentTexture sideTexture() {
        return this.insulated ? INSULATION_SIDE_TEXTURE : super.sideTexture();
    }

    @Override
    protected ComponentTexture openEndTexture() {
        return this.insulated ? INSULATION_OPEN_TEXTURE : super.openEndTexture();
    }

    @Override
    protected ComponentTexture extrusionTexture() {
        return this.insulated ? INSULATION_EXTRUSION_TEXTURE : super.extrusionTexture();
    }

    @Override
    protected ComponentTexture[] sideAtlasTextures(boolean jointed) {
        return this.insulated ?
                jointed ? JOINTED_INSULATION_ATLAS_TEXTURES : INSULATION_ATLAS_TEXTURES :
                super.sideAtlasTextures(jointed);
    }
}
