package gregtech.client.model.connectionmultipart.condition;

import com.google.gson.*;
import gregtech.common.blocks.extendedstate.ConnectionState.CubeEdge;
import gregtech.common.blocks.extendedstate.ConnectionState.CubeVertex;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConnectionVariantConditionDeserializer implements JsonDeserializer<ConnectionVariantCondition> {

    private static final String KEY_OR = "or";
    private static final String KEY_AND = "and";
    private static final String KEY_CONNECTION = "connection";
    private static final String KEY_OCCLUSION = "occlusion";

    private static final Object2IntMap<String> CONNECTION_FLAGS = new Object2IntOpenHashMap<>();
    private static final Object2ObjectMap<String, Vec3i> OFFSETS = new Object2ObjectOpenHashMap<>();

    static {
        OFFSETS.put(null, Vec3i.NULL_VECTOR);
        for (EnumFacing f : EnumFacing.values()) {
            CONNECTION_FLAGS.put(f.getName(), 1 << f.getIndex());
            OFFSETS.put(f.getName(), f.getDirectionVec());
        }
        for (CubeEdge e : CubeEdge.values()) {
            CONNECTION_FLAGS.put(e.id, 1 << (6 + e.ordinal()));
            OFFSETS.put(e.id, e.getDirection());
        }
        for (CubeVertex v : CubeVertex.values()) {
            CONNECTION_FLAGS.put(v.id, 1 << (18 + v.ordinal()));
            OFFSETS.put(v.id, v.getDirection());
        }
    }

    @Override
    public ConnectionVariantCondition deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return parse(json);
    }

    @Nonnull
    public static ConnectionVariantCondition parse(@Nonnull JsonElement element) {
        ConnectionVariantCondition[] c = parseInternal(element);
        switch (c.length) {
            case 0: return ConnectionVariantCondition.never();
            case 1: return c[0];
            default: return new MultiCondition(false, c);
        }
    }

    @Nonnull
    static ConnectionVariantCondition[] parseInternal(@Nonnull JsonElement element) {
        JsonObject o = element.getAsJsonObject();

        String parsed = null;
        ConnectionVariantCondition[] result = null;

        if (o.has(KEY_OR)) {
            parsed = KEY_OR;
            result = new ConnectionVariantCondition[]{
                    parseMultiple(true, o.get(KEY_OR))
            };
        }
        if (o.has(KEY_AND)) {
            if (parsed != null) {
                ambiguousCondition(parsed, KEY_AND);
            }
            parsed = KEY_AND;
            result = new ConnectionVariantCondition[]{
                    parseMultiple(false, o.get(KEY_AND))
            };
        }
        boolean connection = o.has(KEY_CONNECTION);
        boolean occlusion = o.has(KEY_OCCLUSION);
        if (connection || occlusion) {
            if (parsed != null) {
                ambiguousCondition(parsed, connection ? KEY_CONNECTION : KEY_OCCLUSION);
            }
            if (connection) {
                parsed = KEY_CONNECTION;
                if (occlusion) {
                    result = new ConnectionVariantCondition[]{
                            parseConnection(o.get(KEY_CONNECTION)),
                            parseOcclusion(o.get(KEY_OCCLUSION))
                    };
                } else {
                    result = new ConnectionVariantCondition[]{
                            parseConnection(o.get(KEY_CONNECTION))
                    };
                }
            } else {
                parsed = KEY_OCCLUSION;
                result = new ConnectionVariantCondition[]{
                        parseOcclusion(o.get(KEY_OCCLUSION))
                };
            }
        }
        if (parsed == null) {
            throw new JsonParseException("No matching conditions; '" +
                    KEY_OR + "', '" +
                    KEY_AND + "', '" +
                    KEY_CONNECTION + "' or '" +
                    KEY_OCCLUSION + "' expected");
        }
        return result;
    }

    private static void ambiguousCondition(String key1, String key2) {
        throw new JsonParseException("Ambiguous condition between '" + key1 + "' and '" + key2 + '\'');
    }

    @Nonnull
    private static ConnectionVariantCondition parseMultiple(boolean or, @Nonnull JsonElement element) {
        if (element.isJsonObject()) {
            return parse(element);
        } else if (element.isJsonArray()) {
            List<ConnectionVariantCondition> list = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                ConnectionVariantCondition[] conditions = parseInternal(e);
                if (or) {
                    list.add(new MultiCondition(false, conditions));
                } else {
                    Collections.addAll(list, conditions);
                }
            }
            return new MultiCondition(or, list.toArray(new ConnectionVariantCondition[0]));
        } else {
            throw new JsonParseException("Expected array for '" + (or ? KEY_OR : KEY_AND) + '\'');
        }
    }

    @Nonnull
    private static ConnectionVariantCondition parseConnection(@Nonnull JsonElement element) {
        if (element.isJsonObject()) {
            int flagMask = 0, flagValue = 0;
            for (Map.Entry<String, JsonElement> e : element.getAsJsonObject().entrySet()) {
                String key = e.getKey();
                int flag = CONNECTION_FLAGS.getInt(key);
                if (flag == 0) {
                    throw new JsonParseException("Unknown connection factor '" + key + '\'');
                }
                if ((flagMask & flag) != 0) {
                    throw new JsonParseException("Connection factor '" + key + "' defined twice... somehow");
                }

                flagMask |= flag;
                boolean expectedValue = e.getValue().getAsBoolean();
                if (expectedValue) {
                    flagValue |= flag;
                }
            }

            return new ConnectionCondition(flagMask, flagValue);
        } else {
            throw new JsonParseException("Expected object for '" + KEY_CONNECTION + '\'');
        }
    }

    @Nonnull
    static ConnectionVariantCondition parseOcclusion(@Nonnull JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject o = element.getAsJsonObject();
            EnumFacing side = EnumFacing.byName(o.get("side").getAsString());
            if (side == null) {
                throw new JsonParseException("Unknown side value '" + o.get("side").getAsString() + '\'');
            }
            boolean shown = o.get("shown").getAsBoolean();
            Vec3i offset = Vec3i.NULL_VECTOR;
            if (o.has("offset")) {
                offset = OFFSETS.get(o.get("offset").getAsString());
                if (offset == null) {
                    throw new JsonParseException("Unknown offset value '" + o.get("offset").getAsString() + '\'');
                }
            }

            return new OcclusionCondition(side, shown, offset);
        } else {
            throw new JsonParseException("Expected object for '" + KEY_OCCLUSION + '\'');
        }
    }
}
