package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.query.view.service.AssignmentService;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FindAssignmentCandidatesTest {

    private static final String QUERY_NAME = "sjp.query.assignment-candidates";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private AssignmentQueryView assignmentQueryView;

    private UUID assigneeId;
    private int limit;

    @Before
    public void init() {
        assigneeId = randomUUID();
        limit = 10;
    }

    @Test
    public void shouldFindAssignmentCandidatesForMagistrateSession() {
        final SessionType sessionType = MAGISTRATE;

        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME))
                .withPayloadOf(assigneeId, "assigneeId")
                .withPayloadOf(sessionType.name(), "sessionType")
                .withPayloadOf(10, "limit")
                .withPayloadOf("TFL,DVLA", "excludedProsecutingAuthorities")
                .build();

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 1);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 2);

        when(assignmentService.getAssignmentCandidates(eq(assigneeId), eq(sessionType), (Set) argThat(containsInAnyOrder("TFL", "DVLA")), eq(limit)))
                .thenReturn(asList(assignmentCandidate1, assignmentCandidate2));

        final JsonEnvelope assignmentCandidates = assignmentQueryView.findAssignmentCandidates(query);

        assertThat(assignmentCandidates, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate1.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate1.getCaseStreamVersion())),
                withJsonPath("$.assignmentCandidates[1].caseId", equalTo(assignmentCandidate2.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[1].caseStreamVersion", equalTo(assignmentCandidate2.getCaseStreamVersion()))
                )))
//                        .thatMatchesSchema() "Issue with remote refs, reported to Techpod: https://github.com/CJSCommonPlatform/microservice_framework/issues/648"

        );
    }

    @Test
    public void shouldFindAssignmentCandidatesForDelegatedPowersSession() {
        final SessionType sessionType = DELEGATED_POWERS;

        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME))
                .withPayloadOf(assigneeId, "assigneeId")
                .withPayloadOf(sessionType.name(), "sessionType")
                .withPayloadOf(10, "limit")
                .withPayloadOf("TFL,DVLA", "excludedProsecutingAuthorities")
                .build();

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 1);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 2);

        when(assignmentService.getAssignmentCandidates(eq(assigneeId), eq(sessionType), (Set) argThat(containsInAnyOrder("TFL", "DVLA")), eq(limit)))
                .thenReturn(asList(assignmentCandidate1, assignmentCandidate2));

        final JsonEnvelope assignmentCandidates = assignmentQueryView.findAssignmentCandidates(query);

        assertThat(assignmentCandidates, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate1.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate1.getCaseStreamVersion())),
                withJsonPath("$.assignmentCandidates[1].caseId", equalTo(assignmentCandidate2.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[1].caseStreamVersion", equalTo(assignmentCandidate2.getCaseStreamVersion()))
        )))
//                        .thatMatchesSchema() "Issue with remote refs, reported to Techpod: https://github.com/CJSCommonPlatform/microservice_framework/issues/648"
        );
    }

    @Test
    public void shouldReturnEmptyListOfAssignmentCandidates() {
        final SessionType sessionType = MAGISTRATE;

        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME))
                .withPayloadOf(assigneeId, "assigneeId")
                .withPayloadOf(sessionType.name(), "sessionType")
                .withPayloadOf(10, "limit")
                .withPayloadOf("TFL,DVLA", "excludedProsecutingAuthorities")
                .build();

        when(assignmentService.getAssignmentCandidates(eq(assigneeId), eq(sessionType), (Set) argThat(containsInAnyOrder("TFL", "DVLA")), eq(limit)))
                .thenReturn(Collections.emptyList());

        final JsonEnvelope assignmentCandidates = assignmentQueryView.findAssignmentCandidates(query);

        assertThat(assignmentCandidates, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(
                withJsonPath("$.assignmentCandidates.*", hasSize(0))
        ))
//                        .thatMatchesSchema() "Issue with remote refs, reported to Techpod: https://github.com/CJSCommonPlatform/microservice_framework/issues/648"
        );
    }

    @Test
    public void shouldUseEmptyExcludedProsecutingAuthoritiesSetIfParameterNotProvided() {
        final SessionType sessionType = MAGISTRATE;

        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME))
                .withPayloadOf(assigneeId, "assigneeId")
                .withPayloadOf(sessionType.name(), "sessionType")
                .withPayloadOf(10, "limit")
                .build();

        final AssignmentCandidate assignmentCandidate = new AssignmentCandidate(randomUUID(), 1);

        when(assignmentService.getAssignmentCandidates(eq(assigneeId), eq(sessionType), (Set) argThat(empty()), eq(limit)))
                .thenReturn(asList(assignmentCandidate));

        final JsonEnvelope assignmentCandidates = assignmentQueryView.findAssignmentCandidates(query);

        assertThat(assignmentCandidates, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate.getCaseStreamVersion()))
        )))
//                        .thatMatchesSchema() "Issue with remote refs, reported to Techpod: https://github.com/CJSCommonPlatform/microservice_framework/issues/648"
        );
    }
}
