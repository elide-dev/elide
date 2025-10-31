package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Built-in function: json_decode
 * Decodes a JSON string.
 *
 * Usage: $data = json_decode($json);
 */
public final class JsonDecodeBuiltin extends PhpBuiltinRootNode {

    public JsonDecodeBuiltin(PhpLanguage language) {
        super(language, "json_decode");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            throw new RuntimeException("json_decode() expects at least 1 parameter, 0 given");
        }

        Object jsonArg = args[0];
        if (!(jsonArg instanceof String)) {
            return null;
        }

        String json = (String) jsonArg;

        // Optional second parameter: associative (default false in PHP)
        // If true, objects are converted to associative arrays
        boolean associative = false;
        if (args.length >= 2 && args[1] instanceof Boolean) {
            associative = (Boolean) args[1];
        }

        try {
            // Try parsing as object first
            if (json.trim().startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                return convertFromJson(jsonObject, associative);
            } else if (json.trim().startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                return convertFromJson(jsonArray);
            } else {
                // Primitive value
                return json;
            }
        } catch (Exception e) {
            // PHP returns null on decoding error
            return null;
        }
    }

    private Object convertFromJson(JSONObject jsonObject, boolean associative) {
        // Always convert to associative array (PHP objects are complex)
        PhpArray result = new PhpArray();
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            result.put(key, convertValue(value));
        }
        return result;
    }

    private Object convertFromJson(JSONArray jsonArray) {
        PhpArray result = new PhpArray();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            result.put((long) i, convertValue(value));
        }
        return result;
    }

    private Object convertValue(Object value) {
        if (value == JSONObject.NULL || value == null) {
            return null;
        }
        if (value instanceof JSONObject) {
            return convertFromJson((JSONObject) value, true);
        }
        if (value instanceof JSONArray) {
            return convertFromJson((JSONArray) value);
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long || value instanceof Double ||
            value instanceof String || value instanceof Boolean) {
            return value;
        }
        return value.toString();
    }
}
