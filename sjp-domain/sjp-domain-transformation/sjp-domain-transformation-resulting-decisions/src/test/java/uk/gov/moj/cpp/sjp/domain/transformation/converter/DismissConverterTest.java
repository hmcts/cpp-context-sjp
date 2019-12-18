package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import org.junit.Test;

import javax.json.JsonObject;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.DismissConverter.INSTANCE;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TestUtils.readJson;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;

public class DismissConverterTest {

    @Test
    public void convertDecisionWhichIsResultedWithCodeD() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "dismissConverter/d-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = INSTANCE.convert(offenceDecisionJsonObject,"GUILTY", null);

        assertThat(transformedObject,
                is(readJson(
                        "dismissConverter/d-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }

}