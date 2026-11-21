package server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.components.errors.InvalidInputException;

import java.util.Map;

final class JsonRequestBody {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private JsonRequestBody() {}

    static Map<String, Object> readMap(String json) {
        try {
            Map<String, Object> body = OBJECT_MAPPER.readValue(json, MAP_TYPE);
            if (body == null) {
                throw new InvalidInputException("INVALID_JSON", "Request body must be a JSON object.");
            }
            return body;
        } catch (JsonProcessingException exception) {
            throw new InvalidInputException("INVALID_JSON", "Request body must be valid JSON.");
        }
    }
}
