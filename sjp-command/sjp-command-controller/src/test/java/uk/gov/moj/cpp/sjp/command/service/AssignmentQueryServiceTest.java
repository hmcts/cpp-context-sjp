package uk.gov.moj.cpp.sjp.command.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import javax.json.Json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentQueryServiceTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String STRUCTURE_COMMAND_REQUEST_WITHDRAWAL_ALL_OFFENCES = "sjp.command.request-withdrawal-all-offences";
    @InjectMocks
    private AssignmentQueryService assignmentQueryService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;

    @Test
    public void shouldFindAssignmentDetails() throws Exception {
        //Given
        final JsonEnvelope command = createEnvelope(STRUCTURE_COMMAND_REQUEST_WITHDRAWAL_ALL_OFFENCES,
                Json.createObjectBuilder().add("caseId", CASE_ID.toString())
                        .build());
        when(requester.request(captor.capture())).thenReturn(null);

        assignmentQueryService.findAssignmentDetails(command);

        final JsonEnvelope jsonEnvelope = captor.getValue();

        assertThat(jsonEnvelope, jsonEnvelope(
                metadata().withName("assignment.query.assignments"),
                payloadIsJson(allOf(withJsonPath("$.domainObjectId",
                        equalTo(CASE_ID.toString()))
                ))
        ));
    }
}