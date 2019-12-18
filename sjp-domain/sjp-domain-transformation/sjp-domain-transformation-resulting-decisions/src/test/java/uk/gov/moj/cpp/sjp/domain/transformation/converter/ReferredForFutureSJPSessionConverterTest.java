package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import org.junit.Test;

import javax.json.JsonObject;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.ReferredForFutureSJPSessionConverter.INSTANCE;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TestUtils.readJson;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;

public class ReferredForFutureSJPSessionConverterTest {

    @Test
    public void convertDecisionWhichIsResultedWithCodeRSJP() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "referredForFutureSjpHearingConverter/rsjp-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject, null, null);

        assertThat(transformedObject,
                is(readJson(
                        "referredForFutureSjpHearingConverter/rsjp-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }
}