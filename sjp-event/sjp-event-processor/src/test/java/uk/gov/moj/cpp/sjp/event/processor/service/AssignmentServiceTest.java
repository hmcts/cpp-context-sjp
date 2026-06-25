package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentConfiguration;
import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AssignmentServiceTest {

    private static final String SESSION_STARTED_EVENT = "sjp.events.session-started";
    private static final String ASSIGNMENT_CANDIDATES_QUERY = "sjp.query.assignment-candidates";
    private final JsonArray prosecutors = createArrayBuilder().add("TFL").add("DVL").build();

    @Mock
    private AssignmentConfiguration assignmentConfiguration;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    public void shouldReturnLimitedListOfAssignmentCandidates() {
        final UUID legalAdviserId = UUID.randomUUID();
        final int assignmentCandidatesLimit = 10;
        final String localJusticeAreaNationalCourtCode= "1800";

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 4);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 5);

        final JsonEnvelope sourceCommand = envelopeFrom(metadataWithRandomUUID(SESSION_STARTED_EVENT), createObjectBuilder().build());

        final JsonEnvelope assignmentCandidatesResponse = envelopeFrom(metadataWithRandomUUID(ASSIGNMENT_CANDIDATES_QUERY), createObjectBuilder()
                .add("assignmentCandidates", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("caseId", assignmentCandidate1.getCaseId().toString())
                                .add("caseStreamVersion", assignmentCandidate1.getCaseStreamVersion())
                        )
                        .add(createObjectBuilder()
                                .add("caseId", assignmentCandidate2.getCaseId().toString())
                                .add("caseStreamVersion", assignmentCandidate2.getCaseStreamVersion())
                        )
                ).build());

        when(assignmentConfiguration.getAssignmentCandidatesLimit()).thenReturn(assignmentCandidatesLimit);

        when(requester.request(argThat(jsonEnvelope()
                .withMetadataOf(withMetadataEnvelopedFrom(sourceCommand))
                .withPayloadOf(payloadIsJson(allOf(
                        withJsonPath("sessionType", equalTo(MAGISTRATE.toString())),
                        withJsonPath("assigneeId", equalTo(legalAdviserId.toString())),
                        withJsonPath("localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("limit", equalTo(assignmentCandidatesLimit)),
                        withJsonPath("prosecutors", equalTo("TFL,DVL"))
                ))))))
                .thenReturn(assignmentCandidatesResponse);

        final List<AssignmentCandidate> assignmentCandidates = assignmentService.getAssignmentCandidates(sourceCommand, legalAdviserId, MAGISTRATE, localJusticeAreaNationalCourtCode, prosecutors);

        assertThat(assignmentCandidates, contains(assignmentCandidate1, assignmentCandidate2));
    }

    @Test
    public void shouldReturnLimitedListOfAssignmentCandidatesWithoutProsecutors() {
        final UUID legalAdviserId = UUID.randomUUID();
        final int assignmentCandidatesLimit = 10;
        final String localJusticeAreaNationalCourtCode= "1800";

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 4);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 5);

        final JsonEnvelope sourceCommand = envelopeFrom(metadataWithRandomUUID(SESSION_STARTED_EVENT), createObjectBuilder().build());

        final JsonEnvelope assignmentCandidatesResponse = envelopeFrom(metadataWithRandomUUID(ASSIGNMENT_CANDIDATES_QUERY), createObjectBuilder()
                .add("assignmentCandidates", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("caseId", assignmentCandidate1.getCaseId().toString())
                                .add("caseStreamVersion", assignmentCandidate1.getCaseStreamVersion())
                        )
                        .add(createObjectBuilder()
                                .add("caseId", assignmentCandidate2.getCaseId().toString())
                                .add("caseStreamVersion", assignmentCandidate2.getCaseStreamVersion())
                        )
                ).build());

        when(assignmentConfiguration.getAssignmentCandidatesLimit()).thenReturn(assignmentCandidatesLimit);

        when(requester.request(argThat(jsonEnvelope()
                .withMetadataOf(withMetadataEnvelopedFrom(sourceCommand))
                .withPayloadOf(payloadIsJson(allOf(
                        withJsonPath("sessionType", equalTo(MAGISTRATE.toString())),
                        withJsonPath("assigneeId", equalTo(legalAdviserId.toString())),
                        withJsonPath("localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("limit", equalTo(assignmentCandidatesLimit)),
                        withoutJsonPath("prosecutors")
                ))))))
                .thenReturn(assignmentCandidatesResponse);

        final List<AssignmentCandidate> assignmentCandidates = assignmentService.getAssignmentCandidates(sourceCommand, legalAdviserId, MAGISTRATE, localJusticeAreaNationalCourtCode, null);

        assertThat(assignmentCandidates, contains(assignmentCandidate1, assignmentCandidate2));
    }

    @Test
    public void shouldReturnEmptyListOfAssignmentCandidates() {
        final UUID legalAdviserId = UUID.randomUUID();
        final int assignmentCandidatesLimit = 10;
        final String localJusticeAreaNationalCourtCode= "1800";

        final JsonEnvelope sourceCommand = envelopeFrom(metadataWithRandomUUID(SESSION_STARTED_EVENT), createObjectBuilder().build());

        final JsonEnvelope assignmentCandidatesResponse = envelopeFrom(metadataWithRandomUUID(ASSIGNMENT_CANDIDATES_QUERY), createObjectBuilder()
                .add("assignmentCandidates", createArrayBuilder())
                .build());

        when(assignmentConfiguration.getAssignmentCandidatesLimit()).thenReturn(assignmentCandidatesLimit);

        when(requester.request(argThat(jsonEnvelope()
                .withMetadataOf(withMetadataEnvelopedFrom(sourceCommand))
                .withPayloadOf(payloadIsJson(allOf(
                        withJsonPath("sessionType", equalTo(DELEGATED_POWERS.toString())),
                        withJsonPath("assigneeId", equalTo(legalAdviserId.toString())),
                        withJsonPath("localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("limit", equalTo(assignmentCandidatesLimit)),
                        withJsonPath("prosecutors", equalTo("TFL,DVL"))
                ))))))
                .thenReturn(assignmentCandidatesResponse);

        final List<AssignmentCandidate> assignmentCandidates = assignmentService.getAssignmentCandidates(sourceCommand, legalAdviserId, DELEGATED_POWERS, localJusticeAreaNationalCourtCode, prosecutors);

        assertThat(assignmentCandidates, hasSize(0));
    }

    @Test
    public void shouldSendUnassignCaseCommand() {

        final UUID caseId = UUID.randomUUID();

        assignmentService.unassignCase(caseId);

        verify(sender).sendAsAdmin(argThat(commandContainsAll(ImmutableMap.of("name", "sjp.command.unassign-case"), ImmutableMap.of(CASE_ID, caseId.toString()))));
    }

    private Matcher<JsonEnvelope> commandContainsAll(Map<String, String> headers, Map<String, String> payload) {
        return new CustomTypeSafeMatcher<JsonEnvelope>("did not match all attributes") {
            @Override
            protected boolean matchesSafely(JsonEnvelope actualAttributes) {
                return headers.entrySet().stream().allMatch(e -> e.getValue().equals(actualAttributes.metadata().asJsonObject().getString(e.getKey())))
                        &&
                        payload.entrySet().stream().allMatch(e -> e.getValue().equals(actualAttributes.payloadAsJsonObject().getString(e.getKey())));
            }
        };
    }
}
