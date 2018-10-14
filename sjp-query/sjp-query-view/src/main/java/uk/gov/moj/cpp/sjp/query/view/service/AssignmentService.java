package uk.gov.moj.cpp.sjp.query.view.service;

import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.AssignmentRuleType;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.repository.AssignmentRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

public class AssignmentService {

    @Inject
    private AssignmentRepository assignmentRepository;

    public List<AssignmentCandidate> getAssignmentCandidates(final UUID assigneeId, final SessionType sessionType, final Set<String> prosecutingAuthorities, final AssignmentRuleType assignmentRule, int limit) {
        switch (sessionType) {
            case MAGISTRATE:
                return assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities, assignmentRule, limit);
            case DELEGATED_POWERS:
                return assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities, assignmentRule, limit);
            default:
                throw new UnsupportedOperationException(String.format("Session type %s is not supported", sessionType));
        }
    }
}
