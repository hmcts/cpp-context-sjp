package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import org.junit.Test;

import javax.json.JsonObject;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.ReferredToOpenCourtConverter.INSTANCE;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TestUtils.readJson;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;

public class ReferredToOpenCourtConverterTest {

    @Test
    public void convertDecisionWhichIsResultedWithCodeSUMRTO() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "referredForOpenCourtConverter/sumrto-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "referredForOpenCourtConverter/sumrto-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }

}