package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.RANDOM_TEXT;

import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpToHearingConverterTest {

    @InjectMocks
    SjpToHearingConverter sjpToHearingConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private SjpCaseDecisionToHearingResultConverter sjpCaseDecisionToHearingResultConverter;

    @Mock
    JsonEnvelope decisionSavedEventEnvelope;
    @Mock
    PublicHearingResulted result;

    @Test
    public void shouldConvertCaseDecision() {

        Metadata metadata = metadataWithRandomUUID(RANDOM_TEXT).build();
        when(decisionSavedEventEnvelope.metadata()).thenReturn(metadata);
        when(sjpCaseDecisionToHearingResultConverter.convertCaseDecision(anyObject())).thenReturn(result);
        final PublicHearingResulted publicHearingResulted = sjpToHearingConverter.convertCaseDecision(decisionSavedEventEnvelope);

        assertThat(publicHearingResulted, is(notNullValue()));

    }


}
