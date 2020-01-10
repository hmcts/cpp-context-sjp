package uk.gov.moj.cpp.indexer.jolt.helper;

import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.json.jolt.JoltTransformer;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

public class JoltInstanceHelper {

    public static void initializeJolt(final JoltTransformer joltTransformer) {
        final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
        setField(joltTransformer, "stringToJsonObjectConverter", stringToJsonObjectConverter);
    }
}
