package server.controller;

import org.junit.jupiter.api.Test;
import server.components.errors.InvalidInputException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonRequestBodyTest {
    @Test
    void readsJsonObjectsWithTypedMapValues() {
        Map<String, Object> body = JsonRequestBody.readMap("{\"name\":\"team\",\"count\":2,\"tags\":[\"a\"]}");

        assertEquals("team", body.get("name"));
        assertEquals(2, body.get("count"));
        assertEquals(List.of("a"), body.get("tags"));
    }

    @Test
    void rejectsInvalidJsonAndNonObjectBodiesAsClientInput() {
        assertThrows(InvalidInputException.class, () -> JsonRequestBody.readMap("{"));
        assertThrows(InvalidInputException.class, () -> JsonRequestBody.readMap("null"));
        assertThrows(InvalidInputException.class, () -> JsonRequestBody.readMap("[]"));
    }
}
