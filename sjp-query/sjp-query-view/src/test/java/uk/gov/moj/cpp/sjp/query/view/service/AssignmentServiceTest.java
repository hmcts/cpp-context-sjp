package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.ALLOW;

import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.AssignmentRuleType;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.repository.AssignmentRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class AssignmentServiceTest {

    private final int QUERY_LIMIT = 10;
    private final int CASE_STREAM_VERSION = 1;

    @Mock
    private AssignmentRepository assignmentRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private UUID assigneeId;
    private List<AssignmentCandidate> expectedAssignmentCandidates;
    private Set<String> prosecutingAuthorities = emptySet();
    private AssignmentRuleType assignmentRule = ALLOW;

    @Before
    public void init() {
        assigneeId = UUID.randomUUID();
        expectedAssignmentCandidates = singletonList(new AssignmentCandidate(randomUUID(), CASE_STREAM_VERSION));
    }

    @Test
    public void shouldGetAssignmentCandidatesForMagistrateSession() {
        final SessionType sessionType = SessionType.MAGISTRATE;

        when(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities, assignmentRule, QUERY_LIMIT)).thenReturn(expectedAssignmentCandidates);

        final List<AssignmentCandidate> actualAssignmentCandidates = assignmentService.getAssignmentCandidates(assigneeId, sessionType, prosecutingAuthorities, ALLOW, QUERY_LIMIT);

        assertThat(actualAssignmentCandidates, contains(expectedAssignmentCandidates.toArray()));
    }

    @Test
    public void shouldGetAssignmentCandidatesForDelegatedPowersSession() {
        final SessionType sessionType = SessionType.DELEGATED_POWERS;
        when(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities, assignmentRule, QUERY_LIMIT)).thenReturn(expectedAssignmentCandidates);

        final List<AssignmentCandidate> actualAssignmentCandidates = assignmentService.getAssignmentCandidates(assigneeId, sessionType, prosecutingAuthorities, ALLOW, QUERY_LIMIT);

        assertThat(actualAssignmentCandidates, contains(expectedAssignmentCandidates.toArray()));
    }
}
