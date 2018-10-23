package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.ALLOW;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TVL;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.AssignmentRuleType;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.query.view.service.AssignmentService;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

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

    private final AssignmentRuleType assignmentRule = ALLOW;
    private final UUID assigneeId = randomUUID();
    private final int limit = 10;

    @Test
    public void shouldFindAssignmentCandidatesForMagistrateSession() {
        final SessionType sessionType = MAGISTRATE;

        final JsonEnvelope assignmentQuery = buildAssignmentQuery(sessionType, TFL, TVL);

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 1);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 2);

        when(assignmentService.getAssignmentCandidates(eq(assigneeId), eq(sessionType), (Set) argThat(containsInAnyOrder(TFL.name(), TVL.name())), eq(assignmentRule), eq(limit)))
                .thenReturn(asList(assignmentCandidate1, assignmentCandidate2));

        final JsonEnvelope assignmentCandidates = assignmentQueryView.findAssignmentCandidates(assignmentQuery);

        assertThat(assignmentCandidates, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate1.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate1.getCaseStreamVersion())),
                withJsonPath("$.assignmentCandidates[1].caseId", equalTo(assignmentCandidate2.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[1].caseStreamVersion", equalTo(assignmentCandidate2.getCaseStreamVersion()))
                )))
        );
    }

    @Test
    public void shouldFindAssignmentCandidatesForDelegatedPowersSession() {
        final SessionType sessionType = DELEGATED_POWERS;

        final JsonEnvelope assignmentQuery = buildAssignmentQuery(sessionType, TFL, TVL);

        final AssignmentCandidate assignmentCandidate1 = new AssignmentCandidate(randomUUID(), 1);
        final AssignmentCandidate assignmentCandidate2 = new AssignmentCandidate(randomUUID(), 2);

        when(assignmentService.getAssignmentCandidates(eq(assigneeId), eq(sessionType), (Set) argThat(containsInAnyOrder(TFL.name(), TVL.name())), eq(assignmentRule), eq(limit)))
                .thenReturn(asList(assignmentCandidate1, assignmentCandidate2));

        final JsonEnvelope assignmentCandidates = assignmentQueryView.findAssignmentCandidates(assignmentQuery);

        assertThat(assignmentCandidates, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate1.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate1.getCaseStreamVersion())),
                withJsonPath("$.assignmentCandidates[1].caseId", equalTo(assignmentCandidate2.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[1].caseStreamVersion", equalTo(assignmentCandidate2.getCaseStreamVersion()))
                )))
        );
    }

    @Test
    public void shouldReturnEmptyListOfAssignmentCandidates() {
        final SessionType sessionType = MAGISTRATE;

        final JsonEnvelope assignmentQuery = buildAssignmentQuery(sessionType, TFL, TVL);

        when(assignmentService.getAssignmentCandidates(eq(assigneeId), eq(sessionType), (Set) argThat(containsInAnyOrder(TFL.name(), TVL.name())), eq(assignmentRule), eq(limit)))
                .thenReturn(Collections.emptyList());

        final JsonEnvelope assignmentCandidates = assignmentQueryView.findAssignmentCandidates(assignmentQuery);

        assertThat(assignmentCandidates, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(
                withJsonPath("$.assignmentCandidates.*", hasSize(0))
                ))
        );
    }

    @Test
    public void shouldUseEmptyExcludedProsecutingAuthoritiesSetIfParameterNotProvided() {
        final SessionType sessionType = MAGISTRATE;

        final JsonEnvelope assignmentQuery = buildAssignmentQuery(sessionType);

        final AssignmentCandidate assignmentCandidate = new AssignmentCandidate(randomUUID(), 1);

        when(assignmentService.getAssignmentCandidates(eq(assigneeId), eq(sessionType), (Set) argThat(empty()), eq(assignmentRule), eq(limit)))
                .thenReturn(asList(assignmentCandidate));

        final JsonEnvelope assignmentCandidates = assignmentQueryView.findAssignmentCandidates(assignmentQuery);

        assertThat(assignmentCandidates, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.assignmentCandidates[0].caseId", equalTo(assignmentCandidate.getCaseId().toString())),
                withJsonPath("$.assignmentCandidates[0].caseStreamVersion", equalTo(assignmentCandidate.getCaseStreamVersion()))
                )))
        );
    }

    private JsonEnvelope buildAssignmentQuery(final SessionType sessionType, final ProsecutingAuthority... prosecutingAuthorities) {
        final JsonObjectBuilder assignmentQueryPayloadBuilder = Json.createObjectBuilder()
                .add("assigneeId", assigneeId.toString())
                .add("sessionType", sessionType.name())
                .add("limit", 10)
                .add("assignmentRule", assignmentRule.name());

        final String prosecutorsAsQueryParam = asList(prosecutingAuthorities).stream().map(ProsecutingAuthority::name).collect(joining(","));

        if (!prosecutorsAsQueryParam.isEmpty()) {
            assignmentQueryPayloadBuilder.add("prosecutingAuthorities", prosecutorsAsQueryParam);
        }

        return envelopeFrom(metadataWithRandomUUID(QUERY_NAME), assignmentQueryPayloadBuilder.build());
    }
}
