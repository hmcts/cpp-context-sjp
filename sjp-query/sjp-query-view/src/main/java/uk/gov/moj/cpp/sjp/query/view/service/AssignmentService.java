package uk.gov.moj.cpp.sjp.query.view.service;

import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.repository.AssignmentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseAssignmentRestrictionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

public class AssignmentService {

    @Inject
    private AssignmentRepository assignmentRepository;

    @Inject
    private CaseAssignmentRestrictionRepository caseAssignmentRestrictionRepository;

    public List<AssignmentCandidate> getAssignmentCandidates(final UUID assigneeId, final SessionType sessionType, final Set<String> prosecutingAuthorities, int limit) {
        switch (sessionType) {
            case MAGISTRATE:
                return assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities, limit);
            case DELEGATED_POWERS:
                return assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities, limit);
            default:
                throw new UnsupportedOperationException(String.format("Session type %s is not supported", sessionType));
        }
    }

    public List<String> getProsecutingAuthorityByLja(String lja) {
        return caseAssignmentRestrictionRepository.findProsecutingAuthoritiesByLja(lja, LocalDate.now());
    }
}
