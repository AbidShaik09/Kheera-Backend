package com.knightdevelopers.kheerabackend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.knightdevelopers.kheerabackend.web.SpaceApiException;
import java.util.Set;
import java.util.UUID;

final class BoardRequestFields {
    private BoardRequestFields() { }

    static void validateObject(JsonNode json, Set<String> allowed) {
        if (json == null || !json.isObject()) throw SpaceApiException.invalid("request", "A JSON object is required.");
        json.fieldNames().forEachRemaining(key -> {
            if (!allowed.contains(key)) throw SpaceApiException.invalid(key, "Unknown field.");
            if (json.get(key).isNull()) throw SpaceApiException.invalid(key, "Null is not allowed; omit unchanged fields.");
        });
    }

    static String string(JsonNode json, String key) {
        if (!json.has(key)) return null;
        if (!json.get(key).isTextual()) throw SpaceApiException.invalid(key, "Must be a string.");
        return json.get(key).textValue();
    }

    static Integer integer(JsonNode json, String key) {
        if (!json.has(key)) return null;
        if (!json.get(key).isIntegralNumber() || !json.get(key).canConvertToInt())
            throw SpaceApiException.invalid(key, "Must be a 32-bit integer.");
        return json.get(key).intValue();
    }

    static Boolean bool(JsonNode json, String key) {
        if (!json.has(key)) return null;
        if (!json.get(key).isBoolean()) throw SpaceApiException.invalid(key, "Must be a boolean.");
        return json.get(key).booleanValue();
    }

    static UUID uuid(JsonNode json, String key) {
        String value = string(json, key);
        if (value == null) return null;
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
            return id;
        } catch (IllegalArgumentException ex) { throw SpaceApiException.invalid(key, "Must be a UUID."); }
    }
}
