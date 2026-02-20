package uk.gov.moj.cpp.sjp.query.view.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.JsonObject;
import javax.json.JsonValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class JsonUtilityTest {

    private static final String EMPTY = "";
    private static final String FROM_DATE = "2017-07-15";
    private static final String TO_DATE = "2017-08-15";

    private static JsonObject createPayload(final String key, final String value) {
        return Json.createObjectBuilder()
                .add(key, value)
                .build();
    }

    private static JsonObject createEmptyPayload() {
        return Json.createObjectBuilder()
                .build();
    }

    @Test
    public void shouldGetString() throws Exception {
        final String id = "7e2f843e-d639-40b3-8611-8015f3a18958";
        final JsonObject jsonObject = createPayload("id", id);

        final String result = JsonUtility.getString(jsonObject, "id");

        assertEquals(id, result);
    }

    @Test
    public void shouldReturnEmptyStringIfNotFound() throws Exception {
        final JsonObject jsonObject = createEmptyPayload();

        final String result = JsonUtility.getString(jsonObject, "id");

        assertEquals(EMPTY, result);
    }

    @Test
    public void shouldReturnEmptyStringForJsonNull() throws Exception {
        final JsonObject jsonNull = Json.createObjectBuilder().add("id", JsonValue.NULL).build();

        final String result = JsonUtility.getString(jsonNull, "id");

        assertEquals(EMPTY, result);
    }
}
