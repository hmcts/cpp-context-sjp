package uk.gov.moj.cpp.sjp.query.view.service;

import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.repository.AssignmentRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

public class AssignmentService {

    @Inject
    private AssignmentRepository assignmentRepository;

    public List<AssignmentCandidate> getAssignmentCandidates(final UUID assigneeId, final SessionType sessionType, final Set<String> excludedProsecutingAuthorities, int limit) {
        switch (sessionType) {
            case MAGISTRATE:
                return assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities, limit);
            case DELEGATED_POWERS:
                return assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities, limit);
            default:
                throw new UnsupportedOperationException(String.format("Session type % is not supported", sessionType));
        }
    }
}
