package gregtech.client.model.special;

import javax.annotation.Nonnull;

public final class EnumIndexedPart<E extends Enum<E>> {

    private final int startIndex;

    public EnumIndexedPart(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getPart(@Nonnull E e) {
        return startIndex + e.ordinal();
    }
}
