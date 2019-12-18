package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import javax.json.JsonObject;

public interface Converter {
    JsonObject convert(final JsonObject jsonObject, final String pleaType, final String verdict);
}
