package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssignmentServiceTest {

    private static final String SESSION_STARTED_EVENT = "sjp.events.session-started";
    private static final String ASSIGNMENT_CANDIDATES_QUERY = "sjp.query.assignment-candidates";

    @Mock
    private CaseAssignmentConfiguration caseAssignmentConfiguration;

    @Mock
    private ProsecutingAuthoritiesAssignmentsRules prosecutingAuthoritiesAssignmentsRules;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    public void shouldReturnLimitedListOfAssignmentCandidates() {
        final UUID legalAdviserId = UUID.randomUUID();
        final String courtCode = "2222";
        final int assignmentCandidatesLimit = 10;
        final Set<String> excludedProsecutingAuthorities = Stream.of("TFL", "TVL").collect(toSet());

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 4);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 5);

        final JsonEnvelope sourceCommand = envelopeFrom(metadataWithRandomUUID(SESSION_STARTED_EVENT), createObjectBuilder().build());

        final JsonEnvelope assignmentCandidatesResponse = envelopeFrom(metadataWithRandomUUID(ASSIGNMENT_CANDIDATES_QUERY), createObjectBuilder()
                .add("assignmentCandidates", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("caseId", assignmentCandidate1.getCaseId().toString())
                                .add("caseStreamVersion", assignmentCandidate1.getCaseStreamVersion())
                        )
                        .add(createObjectBuilder()
                                .add("caseId", assignmentCandidate2.getCaseId().toString())
                                .add("caseStreamVersion", assignmentCandidate2.getCaseStreamVersion())
                        )
                ).build());

        when(caseAssignmentConfiguration.getAssignmentCandidatesLimit()).thenReturn(assignmentCandidatesLimit);
        when(caseAssignmentConfiguration.getProsecutingAuthoritiesAssignmentRules()).thenReturn(prosecutingAuthoritiesAssignmentsRules);
        when(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities(courtCode)).thenReturn(excludedProsecutingAuthorities);

        when(requester.request(argThat(jsonEnvelope()
                .withMetadataOf(withMetadataEnvelopedFrom(sourceCommand))
                .withPayloadOf(payloadIsJson(allOf(
                        withJsonPath("sessionType", equalTo(MAGISTRATE.toString())),
                        withJsonPath("assigneeId", equalTo(legalAdviserId.toString())),
                        withJsonPath("excludedProsecutingAuthorities", equalTo("TFL,TVL")),
                        withJsonPath("limit", equalTo(assignmentCandidatesLimit))
                ))))))
                .thenReturn(assignmentCandidatesResponse);

        final List<AssignmentCandidate> assignmentCandidates = assignmentService.getAssignmentCandidates(sourceCommand, legalAdviserId, courtCode, MAGISTRATE);

        assertThat(assignmentCandidates, contains(assignmentCandidate1, assignmentCandidate2));
    }

    @Test
    public void shouldReturnEmptyListOfAssignmentCandidates() {
        final UUID legalAdviserId = UUID.randomUUID();
        final String courtCode = "2222";
        final int assignmentCandidatesLimit = 10;
        final Set<String> excludedProsecutingAuthorities = Collections.emptySet();

        final JsonEnvelope sourceCommand = envelopeFrom(metadataWithRandomUUID(SESSION_STARTED_EVENT), createObjectBuilder().build());

        final JsonEnvelope assignmentCandidatesResponse = envelopeFrom(metadataWithRandomUUID(ASSIGNMENT_CANDIDATES_QUERY), createObjectBuilder()
                .add("assignmentCandidates", Json.createArrayBuilder())
                .build());

        when(caseAssignmentConfiguration.getAssignmentCandidatesLimit()).thenReturn(assignmentCandidatesLimit);
        when(caseAssignmentConfiguration.getProsecutingAuthoritiesAssignmentRules()).thenReturn(prosecutingAuthoritiesAssignmentsRules);
        when(prosecutingAuthoritiesAssignmentsRules.getCourtExcludedProsecutingAuthorities(courtCode)).thenReturn(excludedProsecutingAuthorities);

        when(requester.request(argThat(jsonEnvelope()
                .withMetadataOf(withMetadataEnvelopedFrom(sourceCommand))
                .withPayloadOf(payloadIsJson(allOf(
                        withJsonPath("sessionType", equalTo(DELEGATED_POWERS.toString())),
                        withJsonPath("assigneeId", equalTo(legalAdviserId.toString())),
                        withJsonPath("excludedProsecutingAuthorities", isEmptyString()),
                        withJsonPath("limit", equalTo(assignmentCandidatesLimit))
                ))))))
                .thenReturn(assignmentCandidatesResponse);

        final List<AssignmentCandidate> assignmentCandidates = assignmentService.getAssignmentCandidates(sourceCommand, legalAdviserId, courtCode, DELEGATED_POWERS);

        assertThat(assignmentCandidates, hasSize(0));
    }
}
