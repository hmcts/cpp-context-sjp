package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import javax.json.JsonObject;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TestUtils.readJson;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;

@RunWith(MockitoJUnitRunner.class)
public class ReferForCourtHearingConverterTest {

    @Mock
    private SjpViewStoreService sjpViewStoreService;

    private ReferForCourtHearingConverter referForCourtHearingConverter;

    @Before
    public void setUp() {
        this.referForCourtHearingConverter = new ReferForCourtHearingConverter(sjpViewStoreService);
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeSUMRCC() {
        // given the offence decision
        JsonObject offenceDecisionJsonObject = readJson(
                "referForCourtHearingConverter/sumrcc-resultcode-input.json", JsonObject.class);

        JsonObject transformedObject = referForCourtHearingConverter.convert(offenceDecisionJsonObject, "GUILTY", null);

        assertThat(transformedObject,
                is(readJson(
                        "referForCourtHearingConverter/sumrcc-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }
}