package uk.gov.moj.cpp.sjp.command.api.service;


import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private CaseService caseService;

    @Test
    public void shouldFindAndDecorateCase() {
        final UUID caseId = UUID.randomUUID();
        final JsonObject originalCaseDetails = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();
        final JsonEnvelope originalCaseResponse = envelopeFrom(metadataWithRandomUUIDAndName(), originalCaseDetails);
        when(requester.request(any())).thenReturn(originalCaseResponse);

        final JsonObject result = caseService.getCaseDetails(originalCaseResponse);
        MatcherAssert.assertThat(result.getString("caseId"), is(caseId.toString()));
    }
}
