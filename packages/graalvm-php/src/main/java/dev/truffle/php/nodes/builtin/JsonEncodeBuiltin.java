package dev.truffle.php.nodes.builtin;

import dev.truffle.php.PhpLanguage;
import dev.truffle.php.nodes.PhpBuiltinRootNode;
import dev.truffle.php.runtime.PhpArray;
import dev.truffle.php.runtime.PhpClass;
import dev.truffle.php.runtime.PhpObject;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Built-in function: json_encode
 * Returns the JSON representation of a value.
 *
 * Usage: $json = json_encode($data);
 */
public final class JsonEncodeBuiltin extends PhpBuiltinRootNode {

    public JsonEncodeBuiltin(PhpLanguage language) {
        super(language, "json_encode");
    }

    @Override
    protected Object executeBuiltin(Object[] args) {
        if (args.length == 0) {
            throw new RuntimeException("json_encode() expects at least 1 parameter, 0 given");
        }

        Object value = args[0];

        try {
            Object jsonValue = convertToJson(value);
            if (jsonValue instanceof JSONObject) {
                return ((JSONObject) jsonValue).toString();
            } else if (jsonValue instanceof JSONArray) {
                return ((JSONArray) jsonValue).toString();
            } else {
                // For primitives, use JSONObject's valueToString
                return JSONObject.valueToString(jsonValue);
            }
        } catch (Exception e) {
            // PHP returns false on encoding error
            return false;
        }
    }

    private Object convertToJson(Object value) {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value instanceof String || value instanceof Long ||
            value instanceof Double || value instanceof Boolean) {
            return value;
        }
        if (value instanceof PhpArray) {
            PhpArray arr = (PhpArray) value;
            List<Object> keys = arr.keys();

            // Check if it's a sequential array (all keys are long and sequential starting from 0)
            boolean isSequential = true;
            for (int i = 0; i < keys.size(); i++) {
                Object key = keys.get(i);
                if (!(key instanceof Long) || ((Long) key) != i) {
                    isSequential = false;
                    break;
                }
            }

            if (isSequential && !keys.isEmpty()) {
                // Sequential array -> JSON array
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < arr.size(); i++) {
                    jsonArray.put(convertToJson(arr.get((long) i)));
                }
                return jsonArray;
            } else {
                // Associative array -> JSON object
                JSONObject jsonObject = new JSONObject();
                for (Object key : keys) {
                    String jsonKey = key.toString();
                    jsonObject.put(jsonKey, convertToJson(arr.get(key)));
                }
                return jsonObject;
            }
        }
        if (value instanceof PhpObject) {
            PhpObject obj = (PhpObject) value;
            JSONObject jsonObject = new JSONObject();
            // Convert object properties to JSON (use internal read to avoid visibility checks)
            PhpClass phpClass = obj.getPhpClass();
            for (String propName : phpClass.getProperties().keySet()) {
                Object propValue = obj.readPropertyInternal(propName);
                jsonObject.put(propName, convertToJson(propValue));
            }
            return jsonObject;
        }
        // Unknown type - convert to string
        return value.toString();
    }
}
