package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.repository.AssignmentRepository;

import java.util.Arrays;
import java.util.Collections;
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

    @Mock
    private AssignmentRepository assignmentRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private UUID assigneeId;
    private Set<String> excludedProsecutingAuthorities;
    private int limit;

    @Before
    public void init() {
        assigneeId = UUID.randomUUID();
        excludedProsecutingAuthorities = Collections.emptySet();
        limit = 10;
    }

    @Test
    public void shouldGetAssignmentCandidatesForMagistrateSession() {
        final SessionType sessionType = SessionType.MAGISTRATE;

        final List<AssignmentCandidate> expectedAssignmentCandidates = Arrays.asList(new AssignmentCandidate(randomUUID(), 1));

        when(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities, limit)).thenReturn(expectedAssignmentCandidates);

        final List<AssignmentCandidate> actualAssignmentCandidates = assignmentService.getAssignmentCandidates(assigneeId, sessionType, excludedProsecutingAuthorities, limit);
        assertThat(actualAssignmentCandidates, contains(expectedAssignmentCandidates.toArray()));
    }

    @Test
    public void shouldGetAssignmentCandidatesForDelegatedPowersSession() {
        final SessionType sessionType = SessionType.DELEGATED_POWERS;

        final List<AssignmentCandidate> expectedAssignmentCandidates = Arrays.asList(new AssignmentCandidate(randomUUID(), 1));

        when(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities, limit)).thenReturn(expectedAssignmentCandidates);

        final List<AssignmentCandidate> actualAssignmentCandidates = assignmentService.getAssignmentCandidates(assigneeId, sessionType, excludedProsecutingAuthorities, limit);
        assertThat(actualAssignmentCandidates, contains(expectedAssignmentCandidates.toArray()));
    }
}
