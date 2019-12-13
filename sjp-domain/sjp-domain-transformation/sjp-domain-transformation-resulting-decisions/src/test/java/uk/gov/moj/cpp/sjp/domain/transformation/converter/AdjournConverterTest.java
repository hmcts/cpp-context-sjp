package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TestUtils.readJson;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;

import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import java.util.Optional;

import javax.json.JsonObject;
import javax.swing.text.html.Option;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AdjournConverterTest {

    @Mock
    private SjpViewStoreService sjpViewStoreService;

    private AdjournConverter adjournConverter;

    @Before
    public void setUp() {
        this.adjournConverter = new AdjournConverter(sjpViewStoreService);
    }

    @Test
    public void convertDecisionWhichIsResultedWithCodeSUMRCC() {
        // given the offence decision
        when(sjpViewStoreService.getOffenceId("05b215d8-1432-4ea5-b9e9-11cb6c9ae8e7"))
                .thenReturn(Optional.of("702ff8df-8035-41f0-8398-492380adcbbf"));

        final JsonObject offenceDecisionJsonObject = readJson(
                "adjournConverter/adjournsjp-resultcode-input.json", JsonObject.class);

        final JsonObject transformedObject = adjournConverter.convert(offenceDecisionJsonObject, null, "NO_VERDICT");

        assertThat(transformedObject,
                is(readJson(
                        "adjournConverter/adjournsjp-resultcode-output.json",
                        JsonObject.class,
                        transformedObject.getString(ID))));
    }

}