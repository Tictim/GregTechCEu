package gregtech.client.model.special.part;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MissingTextureException extends RuntimeException {

    private final List<Set<String>> requiredTextureSet = new ArrayList<>();

    public MissingTextureException(Set<String> requiredTextures) {
        this.requiredTextureSet.add(requiredTextures);
    }

    public void addRequiredTextures(Set<String> requiredTextures) {
        this.requiredTextureSet.add(requiredTextures);
    }

    @Override
    public String getMessage() {
        return "Missing textures, require these textures to be defined in model:\n  " +
                this.requiredTextureSet.stream()
                        .map(requiredTextures -> String.join("\n  ", requiredTextures))
                        .collect(Collectors.joining("\nor:\n  "));
    }
}
