package uk.gov.moj.cpp.sjp.query.view.converter;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.D45;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.DPR;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.D45;
import static uk.gov.moj.cpp.sjp.query.view.util.results.ResultsMatchers.DPR;

import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressRestrictionResultTest {

    @Mock
    private CachedReferenceData referenceData;

    @Before
    public void setUp() {
        when(referenceData.getResultId(D45.name())).thenReturn(D45.getResultDefinitionId());
        when(referenceData.getResultId(DPR.name())).thenReturn(DPR.getResultDefinitionId());
    }

    @Test
    public void shouldParsePressRestrictionApplied() {
        final JsonObject pressRestriction = createObjectBuilder()
                .add("name", "A Name")
                .add("requested", true)
                .build();

        final JsonObject actual = new PressRestrictionResult(pressRestriction, referenceData, null).toJsonObjectBuilder().build();

        assertThat(actual.toString(), D45("A Name"));
    }

    @Test
    public void shouldParsePressRestrictionRevoked() {
        final JsonObject pressRestriction = createObjectBuilder()
                .addNull("name")
                .add("requested", false)
                .build();
        final String resultedOnDate = "2020-06-22";

        final JsonObject actual = new PressRestrictionResult(pressRestriction, referenceData, resultedOnDate).toJsonObjectBuilder().build();

        assertThat(actual.toString(), DPR(resultedOnDate));
    }
}
