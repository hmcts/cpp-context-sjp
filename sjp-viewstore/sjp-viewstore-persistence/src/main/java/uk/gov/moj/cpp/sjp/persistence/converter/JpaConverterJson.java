package uk.gov.moj.cpp.sjp.persistence.converter;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JpaConverterJson implements AttributeConverter<JsonObject, String> {

    @Override
    public String convertToDatabaseColumn(JsonObject jsonObject) {
        return jsonObject.toString();
    }

    @Override
    public JsonObject convertToEntityAttribute(String dbData) {
        final JsonReader jsonReader = Json.createReader(new StringReader(dbData));
        JsonObject jsonObject;
        try {
            jsonObject = jsonReader.readObject();
        } finally {
            jsonReader.close();
        }
        return jsonObject;
    }

}